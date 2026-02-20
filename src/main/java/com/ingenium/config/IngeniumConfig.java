package com.ingenium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ingenium.core.IngeniumGovernor;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class IngeniumConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "ingenium.json";

    public static final String IMPACT_HIGH = "HIGH";
    public static final String IMPACT_MED = "MED";
    public static final String IMPACT_LOW = "LOW";

    private static volatile IngeniumConfig INSTANCE;

    public static void init() {
        INSTANCE = load();
    }

    public static IngeniumConfig get() {
        if (INSTANCE == null) {
            init();
        }
        return INSTANCE;
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    private final Core core = new Core();
    private final Budgets budgets = new Budgets();

    public Core core() { return core; }
    public Budgets budgets() { return budgets; }

    public static class Core {
        private boolean enableExecutors = true;
        private boolean governorAutoProfile = true;
        private int timeShareResetPeriodTicks = 200;
        private int thresholdBalancedMspt = 25;
        private int thresholdReactiveMspt = 40;
        private int thresholdEmergencyMspt = 55;
        private int profileStabilityTicks = 20;
        private int commitQueueCapacity = 2048;
        private double computePoolMultiplier = 1.0;
        private boolean useVirtualThreadsForIO = true;
        private int ioCoreThreads = 4;

        public boolean enableExecutors() { return enableExecutors; }
        public boolean governorAutoProfile() { return governorAutoProfile; }
        public int timeShareResetPeriodTicks() { return timeShareResetPeriodTicks; }
        public int thresholdBalancedMspt() { return thresholdBalancedMspt; }
        public int thresholdReactiveMspt() { return thresholdReactiveMspt; }
        public int thresholdEmergencyMspt() { return thresholdEmergencyMspt; }
        public int profileStabilityTicks() { return profileStabilityTicks; }
        public int commitQueueCapacity() { return commitQueueCapacity; }
        public double computePoolMultiplier() { return computePoolMultiplier; }
        public boolean useVirtualThreadsForIO() { return useVirtualThreadsForIO; }
        public int ioCoreThreads() { return ioCoreThreads; }
    }

    public static class Budgets {
        private final Map<IngeniumGovernor.Subsystem, Long> baseBudgets = new EnumMap<>(IngeniumGovernor.Subsystem.class);
        private final Map<IngeniumGovernor.Subsystem, Boolean> enabledSubsystems = new EnumMap<>(IngeniumGovernor.Subsystem.class);

        public Budgets() {
            // Default budgets in nanoseconds
            for (IngeniumGovernor.Subsystem s : IngeniumGovernor.Subsystem.values()) {
                baseBudgets.put(s, 1_000_000L); // 1ms default
                enabledSubsystems.put(s, true);
            }
        }

        public boolean isSubsystemEnabled(IngeniumGovernor.Subsystem subsystem) {
            return enabledSubsystems.getOrDefault(subsystem, true);
        }

        public long baseBudgetNs(IngeniumGovernor.Subsystem subsystem) {
            return baseBudgets.getOrDefault(subsystem, 1_000_000L);
        }
    }

    // --------------------------
    // Legacy fields for compatibility
    // --------------------------
    public volatile boolean masterEnabled = true;

    /** "Governor Emergency Threshold: MSPT value at which non-critical block entities begin skipping ticks. [Default: 45ms, Range: 20-100]" */
    public volatile float emergencyMsptThreshold = 45.0f;

    /** "GC Coordination: Monitors heap occupancy each tick... [Default: ON]" */
    public volatile boolean enableGcCoordination = true;

    // --------------------------
    // Scheduled ticks (Wheel replacement)
    // --------------------------
    /** "Scheduled Tick Bucketing: Replaces vanilla's O(log n) tree queue with O(1) hierarchical timing wheels... [Default: ON]" */
    public volatile boolean scheduledTickWheelEnabled = true;

    /** wheel slot count; power-of-two strongly recommended */
    public volatile int timingWheelBucketCount = 256;

    // --------------------------
    // Block entity throttling
    // --------------------------
    /** "Block Entity Throttling: Tiered ticking... [Default: ON]" */
    public volatile boolean blockEntityThrottlingEnabled = true;

    /** Radius within which BEs are CRITICAL */
    public volatile int blockEntityCriticalRadius = 32;

    /** Enables occlusion heuristic (experimental; default false) */
    public volatile boolean blockEntityOcclusionHeuristic = false;

    // --------------------------
    // Off-heap BE metadata
    // --------------------------
    /** "Off-Heap Block Entity Cache... [Default: OFF]" */
    public volatile boolean offHeapBlockEntityDataEnabled = false;

    // --------------------------
    // SIMD (experimental)
    // --------------------------
    public volatile boolean simdPaletteEnabled = false;

    // --------------------------
    // Debug / Offboarding
    // --------------------------
    /** If true, wheel will flush-to-vanilla on every save (recommended for safety) */
    public volatile boolean offboardOnSaveEnabled = true;

    @Override
    public String toString() {
        return "IngeniumConfig{" +
                "master=" + masterEnabled +
                ", emergencyMspt=" + emergencyMsptThreshold +
                ", wheel=" + scheduledTickWheelEnabled +
                ", wheelBuckets=" + timingWheelBucketCount +
                ", beThrottle=" + blockEntityThrottlingEnabled +
                ", offHeapBE=" + offHeapBlockEntityDataEnabled +
                ", gcCoord=" + enableGcCoordination +
                ", simd=" + simdPaletteEnabled +
                ", offboardOnSave=" + offboardOnSaveEnabled +
                '}';
    }

    public void save() {
        try {
            Files.writeString(path(), GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // avoid logger dependency here; upstream will report in practice
            e.printStackTrace();
        }
    }

    private static IngeniumConfig load() {
        Path p = path();
        try {
            if (!Files.exists(p)) {
                IngeniumConfig cfg = new IngeniumConfig();
                cfg.save();
                return cfg;
            }
            String json = Files.readString(p, StandardCharsets.UTF_8);
            IngeniumConfig cfg = GSON.fromJson(json, IngeniumConfig.class);
            return (cfg != null) ? cfg : new IngeniumConfig();
        } catch (Exception e) {
            e.printStackTrace();
            return new IngeniumConfig();
        }
    }

    // For compatibility with some snippets that use getInstance()
    public static IngeniumConfig getInstance() {
        return get();
    }

    // Adding some fields that appear later in the description
    public volatile boolean scheduledTickBucketingEnabled = true; // used in some snippets instead of scheduledTickWheelEnabled
    public volatile boolean offboardRequested = false;
}
