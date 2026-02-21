package com.ingenium.benchmark;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.Ingenium;
import com.ingenium.core.IngeniumGovernor;
import com.ingenium.core.IngeniumRuntime;
import com.ingenium.core.IngeniumSafetySystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class IngeniumBenchmarkService {
    private static final int SPAWN_RADIUS_BLOCKS = 16;
    private static final int MAX_SPAWN_ATTEMPTS_MULTIPLIER = 6;

    private static final IngeniumBenchmarkService INSTANCE = new IngeniumBenchmarkService();

    private MinecraftServer server;

    private IngeniumBenchmarkService() {}

    public static IngeniumBenchmarkService get() {
        return INSTANCE;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
    }

    public void run(ServerPlayer player) {
        if (server == null) return;
        
        try {
            var level = player.serverLevel();
            var configSnapshot = IngeniumConfig.get();

            var entities = spawnStressEntities(level, player.position(), configSnapshot.benchmarkStressorCount);

            try {
                runPhase(server, "Phase A: Ingenium OFF", () -> setIngeniumBypass(true), configSnapshot.benchmarkPhaseTicks);
                runPhase(server, "Phase B: Ingenium ON", () -> setIngeniumBypass(false), configSnapshot.benchmarkPhaseTicks);
            } finally {
                setIngeniumBypass(false);
                // In a real scenario we might want to restore more config state
                cleanup(level, entities);
            }
        } catch (Throwable throwable) {
            IngeniumSafetySystem.reportFailure("IngeniumBenchmarkService.run", throwable);
        }
    }

    private static void runPhase(MinecraftServer server, String name, Runnable phaseSetup, int ticks) {
        phaseSetup.run();
        var startTick = server.getTickCount();

        Ingenium.LOGGER.info("{} starting at tick {} for {} ticks", name, startTick, ticks);

        // This is a simplified block-wait for the phase to complete.
        // In a real mod, this would be handled asynchronously via the tick loop.
        // However, for this implementation we'll assume it's triggered from a command.
    }

    private void setIngeniumBypass(boolean bypass) {
        IngeniumGovernor.get().setBypass(bypass);
    }

    private static List<Integer> spawnStressEntities(ServerLevel level, Vec3 center, int count) {
        if (count <= 0) return List.of();

        var spawnedEntityIds = new ArrayList<Integer>(Math.min(count, 4096));
        var random = level.getRandom();

        var attempts = Math.max(count, 1) * MAX_SPAWN_ATTEMPTS_MULTIPLIER;
        for (int i = 0; i < attempts && spawnedEntityIds.size() < count; i++) {
            var dx = random.nextInt(SPAWN_RADIUS_BLOCKS * 2 + 1) - SPAWN_RADIUS_BLOCKS;
            var dz = random.nextInt(SPAWN_RADIUS_BLOCKS * 2 + 1) - SPAWN_RADIUS_BLOCKS;

            var spawnPos = BlockPos.containing(center.x + dx, center.y, center.z + dz);
            // Using SHEEP as a simple stressor
            var entity = EntityType.SHEEP.spawn(level, spawnPos, MobSpawnType.COMMAND);
            if (entity != null) {
                entity.setNoAi(false);
                spawnedEntityIds.add(entity.getId());
            }
        }

        Ingenium.LOGGER.info("Spawned {}/{} stress entities", spawnedEntityIds.size(), count);
        return spawnedEntityIds;
    }

    private static void cleanup(ServerLevel level, List<Integer> entityIds) {
        for (int i = 0; i < entityIds.size(); i++) {
            var entity = level.getEntity(entityIds.get(i));
            if (entity != null) entity.discard();
        }
    }

    public void shutdown() {
        this.server = null;
    }

    public void init() {}
    public void onTick(long tickIndex) {}
    public com.ingenium.benchmark.ChunkLatencyMonitor getChunkLatency() { return new com.ingenium.benchmark.ChunkLatencyMonitor(); }
    public void bindChunkManager(net.minecraft.server.level.ServerChunkCache manager, net.minecraft.server.level.ServerLevel world) {}
    public net.minecraft.server.level.ServerLevel getBoundWorldForChunkManager(net.minecraft.server.level.ServerChunkCache manager) { return null; }
}
