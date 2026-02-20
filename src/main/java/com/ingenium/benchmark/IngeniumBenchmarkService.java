package com.ingenium.benchmark;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.IngeniumGovernor;
import com.ingenium.core.IngeniumGovernor.SubsystemType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.UUID;

/**
 * Orchestrates a controlled two-phase benchmark:
 * <ul>
 *   <li>Phase A = baseline (Governor bypass enabled; optimizations should behave vanilla-like)</li>
 *   <li>Phase B = optimized (Governor bypass disabled; optimizations active)</li>
 * </ul>
 *
 * <p>Threading model: server thread only (invoked from END_SERVER_TICK or similar).
 *
 * <p>Design goals:
 * <ul>
 *   <li>Safe entity spawn caps</li>
 *   <li>Cleanup on stop/crash</li>
 *   <li>System time share = delta of per-subsystem ns counters from IngeniumGovernor</li>
 * </ul>
 */
public final class IngeniumBenchmarkService {
    private static final Logger LOGGER = LogManager.getLogger("Ingenium/Bench");
    private static final IngeniumBenchmarkService INSTANCE = new IngeniumBenchmarkService();

    public static IngeniumBenchmarkService getInstance() {
        return INSTANCE;
    }

    private enum State {
        IDLE,
        WARMUP_A,
        RUNNING_A,
        COOLDOWN,
        WARMUP_B,
        RUNNING_B
    }

    private State state = State.IDLE;

    private MinecraftServer server;

    /** We keep UUIDs (not strong refs) so we don’t keep entities alive accidentally. */
    private UUID[] spawned;
    private int spawnedCount;

    private int ticksRemaining;
    private int warmupTicks;

    private double sumMsptA;
    private int samplesA;

    private double sumMsptB;
    private int samplesB;

    private final long[] subsystemStart = new long[SubsystemType.values().length];
    private final long[] subsystemEnd = new long[SubsystemType.values().length];

    private IngeniumBenchmarkService() {
    }

    /**
     * Initializes this service. Safe to call multiple times.
     *
     * @param server active server
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("Benchmark service initialized.");
    }

    /**
     * Starts a Phase A/B benchmark if idle.
     * Sends feedback to server operators via broadcast chat (and logs).
     */
    public void startBenchmark(ServerPlayerEntity initiatorOrNull) {
        if (server == null) {
            return;
        }
        if (!IngeniumConfig.get().masterEnabled) {
            sendToInitiatorOrOps(initiatorOrNull,
                    Text.literal("[Ingenium] Master switch is disabled; benchmark cannot start.")
                            .formatted(Formatting.RED));
            return;
        }
        if (state != State.IDLE) {
            sendToInitiatorOrOps(initiatorOrNull,
                    Text.literal("[Ingenium] Benchmark already running.")
                            .formatted(Formatting.RED));
            return;
        }

        final int durationSeconds = Math.max(5, IngeniumConfig.get().benchmarkPhaseDurationSeconds);
        warmupTicks = Math.max(40, IngeniumConfig.get().benchmarkWarmupTicks); // default suggestion: 100
        ticksRemaining = warmupTicks;

        sumMsptA = 0.0;
        samplesA = 0;
        sumMsptB = 0.0;
        samplesB = 0;

        cleanupEntities(); // just in case
        spawnStressors(initiatorOrNull);

        // Phase A: baseline (bypass = true)
        IngeniumGovernor.getInstance().setBypass(true);
        state = State.WARMUP_A;
        ticksRemaining = warmupTicks;

        sendToInitiatorOrOps(initiatorOrNull,
                Text.literal("[Ingenium] Benchmark starting: Phase A (baseline) warmup...")
                        .formatted(Formatting.YELLOW));
        LOGGER.info("Benchmark start: Phase A warmup ({} ticks), duration={}s", warmupTicks, durationSeconds);
    }

    /**
     * Ticks the benchmark state machine. Call once per server tick on the server thread.
     */
    public void tick() {
        if (server == null) return;
        if (state == State.IDLE) return;

        // If config flips off mid-run, abort safely.
        if (!IngeniumConfig.get().masterEnabled || !IngeniumConfig.get().benchmarkEnabled) {
            abort("Disabled by config during benchmark.");
            return;
        }

        ticksRemaining--;

        switch (state) {
            case WARMUP_A -> {
                if (ticksRemaining <= 0) {
                    state = State.RUNNING_A;
                    ticksRemaining = 20 * Math.max(5, IngeniumConfig.get().benchmarkPhaseDurationSeconds);

                    sumMsptA = 0.0;
                    samplesA = 0;
                    snapshotSubsystemTimes(subsystemStart);

                    broadcast(Text.literal("[Ingenium] Phase A measuring...").formatted(Formatting.YELLOW));
                }
            }
            case RUNNING_A -> {
                recordMsptSampleA();
                if (ticksRemaining <= 0) {
                    snapshotSubsystemTimes(subsystemEnd);

                    // Cooldown then Phase B
                    state = State.COOLDOWN;
                    ticksRemaining = Math.max(20, IngeniumConfig.get().benchmarkCooldownTicks); // default suggestion: 40

                    IngeniumGovernor.getInstance().setBypass(false);

                    broadcast(Text.literal("[Ingenium] Phase A complete. Cooling down before Phase B...")
                            .formatted(Formatting.AQUA));
                }
            }
            case COOLDOWN -> {
                if (ticksRemaining <= 0) {
                    state = State.WARMUP_B;
                    ticksRemaining = warmupTicks;

                    broadcast(Text.literal("[Ingenium] Phase B (optimized) warmup...").formatted(Formatting.GREEN));
                }
            }
            case WARMUP_B -> {
                if (ticksRemaining <= 0) {
                    state = State.RUNNING_B;
                    ticksRemaining = 20 * Math.max(5, IngeniumConfig.get().benchmarkPhaseDurationSeconds);

                    sumMsptB = 0.0;
                    samplesB = 0;
                    snapshotSubsystemTimes(subsystemStart);

                    broadcast(Text.literal("[Ingenium] Phase B measuring...").formatted(Formatting.GREEN));
                }
            }
            case RUNNING_B -> {
                recordMsptSampleB();
                if (ticksRemaining <= 0) {
                    snapshotSubsystemTimes(subsystemEnd);
                    finish();
                }
            }
            default -> {
                // no-op
            }
        }
    }

    /**
     * Shuts down the service, ensuring stressors are removed.
     */
    public void shutdown() {
        abort("Server stopping.");
    }

    // -------------------
    // Internals
    // -------------------

    private void finish() {
        final double avgA = samplesA == 0 ? 0.0 : (sumMsptA / samplesA);
        final double avgB = samplesB == 0 ? 0.0 : (sumMsptB / samplesB);
        final double improvement = avgA > 0.0 ? ((avgA - avgB) / avgA) * 100.0 : 0.0;

        final long[] delta = new long[SubsystemType.values().length];
        long totalDelta = 0L;
        for (int i = 0; i < delta.length; i++) {
            delta[i] = subsystemEnd[i] - subsystemStart[i];
            totalDelta += Math.max(0L, delta[i]);
        }

        broadcast(Text.literal("=== Ingenium Benchmark Results ===").formatted(Formatting.GOLD));
        broadcast(Text.literal(String.format(Locale.ROOT, "Phase A (baseline): %.2f mspt", avgA)).formatted(Formatting.GRAY));
        broadcast(Text.literal(String.format(Locale.ROOT, "Phase B (optimized): %.2f mspt", avgB))
                .formatted(improvement >= 0 ? Formatting.GREEN : Formatting.RED));
        broadcast(Text.literal(String.format(Locale.ROOT, "Improvement: %.2f%%", improvement))
                .formatted(improvement >= 0 ? Formatting.GREEN : Formatting.RED));

        if (totalDelta > 0L) {
            broadcast(Text.literal("Subsystem time share (ns delta):").formatted(Formatting.AQUA));
            for (SubsystemType t : SubsystemType.values()) {
                final long d = Math.max(0L, delta[t.ordinal()]);
                final double pct = (d * 100.0) / totalDelta;
                broadcast(Text.literal(String.format(Locale.ROOT, "  %s: %.1f%%", t.name(), pct)).formatted(Formatting.GRAY));
            }
        }

        cleanupEntities();

        // Restore governor to normal operation.
        IngeniumGovernor.getInstance().setBypass(false);
        IngeniumGovernor.getInstance().resetSubsystemTimes();

        state = State.IDLE;
        LOGGER.info("Benchmark finished: avgA={}mspt avgB={}mspt improvement={}%", avgA, avgB, improvement);
    }

    private void abort(String reason) {
        if (state == State.IDLE) return;

        LOGGER.warn("Benchmark aborted: {}", reason);
        broadcast(Text.literal("[Ingenium] Benchmark aborted: " + reason).formatted(Formatting.RED));

        cleanupEntities();
        IngeniumGovernor.getInstance().setBypass(false);

        state = State.IDLE;
    }

    private void recordMsptSampleA() {
        final double mspt = getServerMsptSample();
        sumMsptA += mspt;
        samplesA++;
    }

    private void recordMsptSampleB() {
        final double mspt = getServerMsptSample();
        sumMsptB += mspt;
        samplesB++;
    }

    /**
     * Attempts to get a per-tick mspt sample from the server.
     *
     * <p>Architect: Verify mapping. Depending on Yarn/Fabric,
     * you may prefer:
     * <ul>
     *   <li>MinecraftServer#getAverageTickTime()</li>
     *   <li>or tickTimes array exposure</li>
     *   <li>or reuse IngeniumGovernor’s own currentMspt</li>
     * </ul>
     */
    private double getServerMsptSample() {
        // Preferred: use governor’s measured MSPT (stable across mappings).
        return (double) IngeniumGovernor.getInstance().getCurrentMspt();
    }

    private void snapshotSubsystemTimes(long[] target) {
        final IngeniumGovernor gov = IngeniumGovernor.getInstance();
        for (SubsystemType t : SubsystemType.values()) {
            target[t.ordinal()] = gov.getSubsystemTimeNs(t);
        }
    }

    private void spawnStressors(ServerPlayerEntity initiatorOrNull) {
        if (server == null) return;

        final ServerWorld world = server.getOverworld();
        if (world == null) return;

        // Choose a safe spawn location: near initiator if present, else at world spawn.
        final Vec3d base;
        if (initiatorOrNull != null) {
            base = initiatorOrNull.getPos();
        } else {
            BlockPos spawn = world.getSpawnPos();
            base = new Vec3d(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        }

        // Hard cap to avoid accidental server death spirals.
        final int requested = Math.max(0, IngeniumConfig.get().benchmarkStressorCount);
        final int cap = 2000;
        final int count = Math.min(requested, cap);

        spawned = new UUID[count];
        spawnedCount = 0;

        for (int i = 0; i < count; i++) {
            VillagerEntity villager = EntityType.VILLAGER.create(world);
            if (villager == null) continue;

            // Stack them with slight offsets to reduce collisions; keep within loaded area.
            double x = base.x + (i & 15) * 0.25;
            double z = base.z + ((i >> 4) & 15) * 0.25;
            double y = Math.max(base.y, world.getBottomY() + 5);

            villager.refreshPositionAndAngles(x, y, z, 0.0f, 0.0f);
            villager.setPersistent(); // discourage auto-despawn

            if (world.spawnEntity(villager)) {
                spawned[spawnedCount++] = villager.getUuid();
            }
        }

        broadcast(Text.literal("[Ingenium] Spawned " + spawnedCount + " stressor entities (requested " + requested + ").")
                .formatted(Formatting.GRAY));
        LOGGER.info("Spawned {} stressors (requested={}, cap={})", spawnedCount, requested, cap);
    }

    private void cleanupEntities() {
        if (server == null) return;
        if (spawned == null || spawnedCount == 0) return;

        for (int i = 0; i < spawnedCount; i++) {
            UUID id = spawned[i];
            if (id == null) continue;

            // Search worlds; stressors were spawned in overworld but be robust.
            for (ServerWorld w : server.getWorlds()) {
                Entity e = w.getEntity(id); // Architect: Verify mapping (ServerWorld#getEntity(UUID))
                if (e != null) {
                    e.remove(Entity.RemovalReason.DISCARDED);
                    break;
                }
            }
        }

        spawned = null;
        spawnedCount = 0;
    }

    private void broadcast(Text msg) {
        if (server == null) return;
        server.getPlayerManager().broadcast(msg, false);
        LOGGER.info(msg.getString());
    }

    private void sendToInitiatorOrOps(ServerPlayerEntity initiatorOrNull, Text msg) {
        if (initiatorOrNull != null) {
            initiatorOrNull.sendMessage(msg, false);
        } else {
            broadcast(msg);
        }
    }

    public static IngeniumBenchmarkService get() { return getInstance(); }
    public void init() {}
    public void onTick(long tickIndex) { tick(); }
    public void onTick() { tick(); }
    public com.ingenium.benchmark.ChunkLatencyMonitor getChunkLatency() { return new com.ingenium.benchmark.ChunkLatencyMonitor(); }
    public void bindChunkManager(net.minecraft.server.world.ServerChunkManager manager, net.minecraft.server.world.ServerWorld world) {}
    public net.minecraft.server.world.ServerWorld getBoundWorldForChunkManager(net.minecraft.server.world.ServerChunkManager manager) { return null; }
}
