package com.ingenium.core;

import com.ingenium.config.IngeniumConfig;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IngeniumGovernor: the "adaptive intelligence" controller.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Track tick MSPT (server thread).</li>
 *   <li>Maintain an OptimizationProfile (auto or manual).</li>
 *   <li>Provide per-subsystem nanosecond budgets and time-share counters.</li>
 *   <li>Provide a benchmark bypass mode (Phase A baseline).</li>
 * </ul>
 *
 * <p>Threading:
 * <ul>
 *   <li>onTickStart/onTickEnd must be called from the server thread.</li>
 *   <li>Getters are safe from any thread (volatile/atomics).</li>
 * </ul>
 */
public final class IngeniumGovernor {
    private static final Logger LOGGER = LogManager.getLogger("Ingenium/Governor");
    private static final IngeniumGovernor INSTANCE = new IngeniumGovernor();

    public static final double ENTER_EMERGENCY_MSPT = 52.0;
    public static final double EXIT_EMERGENCY_MSPT = 45.0;
    public static final int ENTER_EMERGENCY_TICKS = 100;
    public static final int EXIT_EMERGENCY_TICKS = 200;
    private static final long MODE_CHANGE_COOLDOWN_NANOS = 5_000_000_000L;

    public void restoreFromSnapshot(OptimizationProfile profile, double msptAvg, long stabilityTicks) {
        this.profile = profile;
        this.currentMspt.set((long) msptAvg);
        this.stabilityCounter = (int) stabilityTicks;
        this.lastModeChangeNanos = System.nanoTime();
        LOGGER.info("Governor restored from snapshot: profile={}, msptAvg={}", profile, msptAvg);
    }

    public double getRecentMsptAverage() {
        return currentMspt.get();
    }

    public long getProfileStabilityTicks() {
        return stabilityCounter;
    }

    public static IngeniumGovernor get() {
        return INSTANCE;
    }

    /** Adaptive profile states (mandated by research). */
    public enum OptimizationProfile {
        AGGRESSIVE(1, 0.95),
        BALANCED(2, 0.75),
        REACTIVE(4, 0.60),
        EMERGENCY(8, 0.40);

        /** Suggested divisor for BE ticking (policy uses this as a base). */
        public final int beDivisor;
        /** Multiplier applied to configured base budgets. */
        public final double budgetMultiplier;

        OptimizationProfile(int beDivisor, double budgetMultiplier) {
            this.beDivisor = beDivisor;
            this.budgetMultiplier = budgetMultiplier;
        }

        public OptimizationProfile oneStepTighter() {
            return switch (this) {
                case AGGRESSIVE -> BALANCED;
                case BALANCED -> REACTIVE;
                case REACTIVE, EMERGENCY -> EMERGENCY;
            };
        }

        public OptimizationProfile oneStepLooser() {
            return switch (this) {
                case EMERGENCY -> REACTIVE;
                case REACTIVE -> BALANCED;
                case BALANCED, AGGRESSIVE -> AGGRESSIVE;
            };
        }
    }

    /** Budget/time-share categories used across the mod. */
    public enum SubsystemType {
        CORE_GOVERNOR,
        CORE_COMMIT_QUEUE,

        TIMING_WHEEL_DRAIN,
        TIMING_WHEEL_OVERFLOW,

        BLOCK_ENTITY_TICKING,
        RANDOM_TICK_SCANNER_SIMD,
        CHUNK_SCHEDULING,
        RANDOM_TICK_SCANNER_SCALAR,

        CHUNK_IO,
        NETWORK,

        HOPPER,
        XP_ORBS,
        POI_QUERIES,

        // Visual Pipeline
        ENTITY_BACKFACE_CULLING,
        ITEM_FAST_PATH,
        FAST_FRUSTUM,
        CULL_PASS,

        // Legacy/Misc
        SCHEDULED_TICKS, 
        BLOCK_ENTITIES,
        PATHFINDING, 
        CHUNK_TICKS, 
        ASYNC_SAVE
    }

    /**
     * Small RAII timer that accumulates into subsystem time-share with minimal overhead.
     * Use in hot paths as: {@code try (var t = governor.time(SubsystemType.X)) { ... }}
     */
    public final class SubsystemTimer implements AutoCloseable {
        private final SubsystemType subsystem;
        private final long startNs;

        private SubsystemTimer(SubsystemType subsystem) {
            this.subsystem = subsystem;
            this.startNs = System.nanoTime();
        }

        @Override
        public void close() {
            final long dt = System.nanoTime() - startNs;
            subsystemNs.get(subsystem).addAndGet(dt);
        }
    }

    private volatile MinecraftServer server;
    private volatile IngeniumConfig config;

    private final AtomicLong tickStartNs = new AtomicLong();
    private final AtomicLong lastTickNs = new AtomicLong();
    private final AtomicLong currentMspt = new AtomicLong();

    private volatile OptimizationProfile profile = OptimizationProfile.BALANCED;
    private volatile boolean manualProfile;
    private volatile boolean bypass; // Phase A baseline (disable adaptive effects)

    private volatile int stabilityCounter;

    private int overBudgetTicks;
    private int underBudgetTicks;
    private long lastModeChangeNanos;
    private final GovernorBudgetProfile budgetProfile = GovernorBudgetProfile.defaults();

    private int remainingEntityCull;
    private int remainingItemFastPath;

    private final EnumMap<SubsystemType, AtomicLong> subsystemNs = new EnumMap<>(SubsystemType.class);

    private final MsptTrendAnalyzer trendAnalyzer = new MsptTrendAnalyzer();
    private int preemptiveTightenCooldown = 0;

    private IngeniumGovernor() {
        for (SubsystemType s : SubsystemType.values()) {
            subsystemNs.put(s, new AtomicLong());
        }
        this.lastModeChangeNanos = System.nanoTime();
        resetFrameBudgets();
    }

    /** Attach to a running server instance. */
    public void attach(MinecraftServer server, IngeniumConfig config) {
        this.server = Objects.requireNonNull(server, "server");
        this.config = config; // allow null -> safe defaults
        this.profile = OptimizationProfile.BALANCED;
        this.manualProfile = false;
        this.bypass = false;
        this.stabilityCounter = 0;
        resetSubsystemTimeShare();
        boolean auto = true;
        if (config != null && config.core() != null) {
            auto = config.core().governorAutoProfile();
        }
        LOGGER.info("Attached. AutoProfile={}", auto);
    }

    /** Detach on shutdown to release references. */
    public void detach() {
        this.server = null;
        this.config = null;
    }

    public OptimizationProfile profile() {
        return profile;
    }

    public OptimizationProfile getCurrentProfile() {
        return profile;
    }

    public long getCurrentMspt() {
        return currentMspt.get();
    }

    public boolean isBypassed() {
        return bypass;
    }

    public long getRemainingBudgetNs(SubsystemType subsystem) {
        final IngeniumConfig cfg = this.config;
        if (cfg == null) return 0L;
        return effectiveBudgetNs(cfg, subsystem);
    }

    public long getSubsystemTimeNs(SubsystemType subsystem) {
        return subsystemTimeNs(subsystem);
    }

    public void resetSubsystemTimes() {
        resetSubsystemTimeShare();
    }

    public long currentMspt() {
        return getCurrentMspt();
    }

    public boolean isBypass() {
        return isBypassed();
    }

    /**
     * Benchmark Phase A support: when bypass=true, other systems should behave like "vanilla".
     * This does not forcibly unapply mixins; it is a behavioral contract.
     */
    public void setBypass(boolean bypass) {
        this.bypass = bypass;
    }

    /** Manual override for benchmarking, debugging, and stress tests. */
    public void setManualProfile(OptimizationProfile p) {
        this.manualProfile = true;
        transitionTo(p);
    }

    /** Return to automatic profile transitions (if enabled in config). */
    public void clearManualProfile() {
        this.manualProfile = false;
    }

    public void onTickStart() {
        tickStartNs.set(System.nanoTime());
        // We do not reset subsystemNs every tick; benchmark/diagnostics can snapshot periodically.
    }

    public void onTickEnd() {
        final long endNs = System.nanoTime();
        final long dtNs = endNs - tickStartNs.get();
        final long mspt = dtNs / 1_000_000L;
        lastTickNs.set(dtNs);
        currentMspt.set(mspt);

        // Record to global metrics
        var rt = Ingenium.runtime();
        if (rt != null) {
            rt.metrics().mspt().recordTickNs(dtNs);
            // Update mode from MSPT evidence (last 100 ticks)
            updateModeFromMspt(rt.metrics().mspt().avgMspt100());
        }

        // Auto-transition if enabled and not in manual mode.
        final IngeniumConfig cfg = this.config;
        if (!manualProfile && cfg != null && cfg.core().governorAutoProfile()) {
            try (SubsystemTimer ignored = time(SubsystemType.CORE_GOVERNOR)) {
                evaluateTrend(mspt);
                maybeTransitionProfile(cfg, mspt);
            }
        }

        // Optional: periodic decay/reset of subsystem time-share to avoid unbounded growth.
        if (server != null) {
            final int period = (cfg != null) ? Math.max(20, cfg.core().timeShareResetPeriodTicks()) : 200;
            if ((server.getTickCount() % period) == 0) {
                resetSubsystemTimeShare();
            }
        }
    }

    private void evaluateTrend(long currentMspt) {
        trendAnalyzer.recordSample(currentMspt);
        
        if (preemptiveTightenCooldown > 0) {
            preemptiveTightenCooldown--;
            return;
        }
        
        MsptTrendAnalyzer.TrendAction action = trendAnalyzer.getRecommendedAction();
        
        switch (action) {
            case PREEMPTIVE_TIGHTEN -> {
                if (profile != OptimizationProfile.EMERGENCY) {
                    transitionTo(profile.oneStepTighter());
                    preemptiveTightenCooldown = 60; // 3 second cooldown
                }
            }
            default -> {}
        }
    }

    /** Create a timer for subsystem time-share accounting. */
    public SubsystemTimer time(SubsystemType subsystem) {
        return new SubsystemTimer(subsystem);
    }

    /** Returns accumulated nanoseconds spent in subsystem since last reset. */
    public long subsystemTimeNs(SubsystemType subsystem) {
        return subsystemNs.get(subsystem).get();
    }

    public void resetSubsystemTimeShare() {
        for (AtomicLong v : subsystemNs.values()) v.set(0L);
    }

    /**
     * Budget check (cheap): if bypass is enabled, always allow (baseline mode should not self-throttle).
     */
    public boolean hasBudget(SubsystemType subsystem, long estimatedCostNs) {
        if (bypass) return true;

        final IngeniumConfig cfg = this.config;
        if (cfg == null) return true;

        if (!cfg.budgets().isSubsystemEnabled(subsystem)) return false;

        final long budget = effectiveBudgetNs(cfg, subsystem);
        return estimatedCostNs <= budget;
    }

    /**
     * Consume budget (slightly heavier): subtract from a per-tick budget pool.
     *
     * <p>This implementation is deliberately simple and allocation-free:
     * budgets are computed from config/profile; per-tick enforcement is done by callers
     * using {@link #hasBudget(SubsystemType, long)} and/or sampling time-share.
     *
     * <p>Section 5.1 (Benchmark/Diagnostics) will formalize "System Time Share" reporting.
     */
    public boolean consumeBudget(SubsystemType subsystem, long estimatedCostNs) {
        return hasBudget(subsystem, estimatedCostNs);
    }

    /**
     * Called once per rendered frame (client-only) to reset token budgets.
     */
    public void beginClientFrame() {
        resetFrameBudgets();
    }

    /**
     * Check if a feature is allowed by the current budget.
     */
    public boolean allow(SubsystemType feature) {
        if (bypass) return true;
        return switch (feature) {
            case ENTITY_BACKFACE_CULLING -> consumeEntityCull();
            case ITEM_FAST_PATH -> consumeItemFastPath();
            default -> true;
        };
    }

    private boolean consumeEntityCull() {
        if (remainingEntityCull <= 0) return false;
        remainingEntityCull--;
        return true;
    }

    private boolean consumeItemFastPath() {
        if (remainingItemFastPath <= 0) return false;
        remainingItemFastPath--;
        return true;
    }

    private void resetFrameBudgets() {
        if (profile == OptimizationProfile.EMERGENCY) {
            remainingEntityCull = budgetProfile.entityBackfaceCullPerFrameEmergency();
            remainingItemFastPath = budgetProfile.itemFastPathPerFrameEmergency();
        } else {
            remainingEntityCull = budgetProfile.entityBackfaceCullPerFrame();
            remainingItemFastPath = budgetProfile.itemFastPathPerFrame();
        }
    }

    private void updateModeFromMspt(double mspt) {
        if (Double.isNaN(mspt) || mspt <= 0.0) return;

        if (profile != OptimizationProfile.EMERGENCY) {
            if (mspt > ENTER_EMERGENCY_MSPT) {
                overBudgetTicks++;
                if (overBudgetTicks >= ENTER_EMERGENCY_TICKS && canChangeMode()) {
                    setMode(OptimizationProfile.EMERGENCY);
                }
            } else {
                overBudgetTicks = 0;
            }
        } else {
            if (mspt < EXIT_EMERGENCY_MSPT) {
                underBudgetTicks++;
                if (underBudgetTicks >= EXIT_EMERGENCY_TICKS && canChangeMode()) {
                    setMode(OptimizationProfile.BALANCED);
                }
            } else {
                underBudgetTicks = 0;
            }
        }
    }

    private boolean canChangeMode() {
        return (System.nanoTime() - lastModeChangeNanos) >= MODE_CHANGE_COOLDOWN_NANOS;
    }

    private void setMode(OptimizationProfile newMode) {
        if (newMode == profile) return;
        transitionTo(newMode);
        overBudgetTicks = 0;
        underBudgetTicks = 0;
        lastModeChangeNanos = System.nanoTime();
        resetFrameBudgets();
    }

    /**
     * Hint used by commit queue processing and wheel reinsertion caps.
     * Keeps behavior stable in EMERGENCY profile by reducing main-thread extra work.
     */
    public int reinsertionCapHint() {
        return switch (profile) {
            case AGGRESSIVE -> 1000;
            case BALANCED -> 750;
            case REACTIVE -> 500;
            case EMERGENCY -> 250;
        };
    }

    public long getBudgetForSubsystem(SubsystemType subsystem) {
        return effectiveBudgetNs(this.config, subsystem);
    }

    private long effectiveBudgetNs(IngeniumConfig cfg, SubsystemType subsystem) {
        final long base = cfg.budgets().baseBudgetNs(subsystem);
        final double mult = profile.budgetMultiplier;
        return (long) Math.max(0L, base * mult);
    }

    private void maybeTransitionProfile(IngeniumConfig cfg, long mspt) {
        // Config-driven thresholds (no hardcoded numbers).
        final int balanced = cfg.core().thresholdBalancedMspt();
        final int reactive = cfg.core().thresholdReactiveMspt();
        final int emergency = cfg.core().thresholdEmergencyMspt();
        final int stability = Math.max(1, cfg.core().profileStabilityTicks());

        final OptimizationProfile target =
                (mspt >= emergency) ? OptimizationProfile.EMERGENCY :
                (mspt >= reactive) ? OptimizationProfile.REACTIVE :
                (mspt >= balanced) ? OptimizationProfile.BALANCED :
                        OptimizationProfile.AGGRESSIVE;

        if (target != profile) {
            stabilityCounter++;
            if (stabilityCounter >= stability) {
                transitionTo(target);
                stabilityCounter = 0;
            }
        } else {
            stabilityCounter = 0;
        }
    }

    private void transitionTo(OptimizationProfile next) {
        if (next == profile) return;
        final OptimizationProfile prev = profile;
        profile = next;
        LOGGER.info("Profile transition {} -> {} (MSPT={}ms, bypass={}, manual={})",
                prev, next, currentMspt.get(), bypass, manualProfile);
    }

    /**
     * Feedback from Diagnostics: how much wall time the mod is consuming overall.
     */
    public void reportSystemSharePermille(long permille) {
        // Architect: use this to damp AGGRESSIVE transitions if mod overhead is high
    }

    /**
     * Feedback from Diagnostics: average cost of a subsystem.
     */
    public void reportSubsystemAvgMicros(SubsystemType subsystem, long avgUs) {
        // Architect: use this for dynamic budget adjustment
    }

    // Legacy/Compatibility methods
    public static IngeniumGovernor getInstance() { return get(); }
    public OptimizationProfile getProfile() { return profile; }
    public int blockEntityTickDivisor() { return profile.beDivisor; }
    public void recordSubsystemTime(SubsystemType subsystem, long ns) {
        subsystemNs.get(subsystem).addAndGet(ns);
    }
    public boolean allowBlockEntityTick(net.minecraft.world.level.block.entity.TickingBlockEntity invoker) { return true; }
}
