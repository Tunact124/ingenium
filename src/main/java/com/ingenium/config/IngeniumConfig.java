package com.ingenium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ingenium.core.IngeniumGovernor;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ingenium configuration snapshot.
 *
 * <p>Important: hot-path code should only read primitives from this snapshot and avoid allocations.
 * The UI can replace the snapshot atomically.
 */
public final class IngeniumConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "ingenium.json";
    private static final AtomicReference<IngeniumConfig> LIVE = new AtomicReference<>();

    public static void init() {
        if (LIVE.get() == null) {
            LIVE.set(load());
        }
    }

    public static IngeniumConfig get() {
        if (LIVE.get() == null) {
            init();
        }
        return LIVE.get();
    }

    public static void set(IngeniumConfig newConfig) {
        LIVE.set(newConfig == null ? defaults() : newConfig);
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    // ---- feature flags ----
    public final boolean enabled;
    public final boolean timingWheelEnabled;
    public final boolean offHeapBeMetadataEnabled;
    public final boolean throttleBlockEntities;
    public final boolean diagnosticsEnabled;
    public final boolean benchPhaseBEnabled;
    public final boolean sparkIntegrationEnabled;
    public final boolean enableGcCoordination;
    public final boolean benchmarkEnabled;
    public final boolean hopperOptimizationEnabled;
    public final boolean redstoneOptimizationEnabled;
    public final boolean xpOrbCoalescingEnabled;
    public final boolean poiSpatialHashingEnabled;
    public final boolean sodiumMenuIntegrationEnabled;

    // ---- tuning knobs ----
    public final int beCriticalRadiusBlocks;
    public final int beMaxConsecutiveSkips;
    public final long diagnosticsSnapshotIntervalTicks;
    public final int benchmarkPhaseDurationSeconds;
    public final int benchmarkWarmupTicks;
    public final int benchmarkCooldownTicks;
    public final int benchmarkStressorCount;
    public final int benchmarkPhaseTicks;

    // ---- compatibility fields ----
    public final boolean masterEnabled;
    public final boolean scheduledTickWheelEnabled;
    public final boolean offHeapBlockEntityDataEnabled;
    public final boolean blockEntityThrottlingEnabled;
    public final int blockEntityCriticalRadius;
    public final int offHeapBeMetadataCapacity;
    public final boolean simdPaletteEnabled;

    // ---- nested structures for compatibility ----
    public final Core core;
    public final Budgets budgets;

    private IngeniumConfig(
            boolean enabled,
            boolean timingWheelEnabled,
            boolean offHeapBeMetadataEnabled,
            boolean throttleBlockEntities,
            boolean diagnosticsEnabled,
            boolean benchPhaseBEnabled,
            boolean sparkIntegrationEnabled,
            boolean enableGcCoordination,
            boolean benchmarkEnabled,
            int beCriticalRadiusBlocks,
            int beMaxConsecutiveSkips,
            long diagnosticsSnapshotIntervalTicks,
            int benchmarkPhaseDurationSeconds,
            int benchmarkWarmupTicks,
            int benchmarkCooldownTicks,
            int benchmarkStressorCount,
            int benchmarkPhaseTicks,
            boolean hopperOptimizationEnabled,
            boolean redstoneOptimizationEnabled,
            boolean xpOrbCoalescingEnabled,
            boolean poiSpatialHashingEnabled,
            boolean sodiumMenuIntegrationEnabled,
            Core core,
            Budgets budgets
    ) {
        this.enabled = enabled;
        this.timingWheelEnabled = timingWheelEnabled;
        this.offHeapBeMetadataEnabled = offHeapBeMetadataEnabled;
        this.throttleBlockEntities = throttleBlockEntities;
        this.diagnosticsEnabled = diagnosticsEnabled;
        this.benchPhaseBEnabled = benchPhaseBEnabled;
        this.sparkIntegrationEnabled = sparkIntegrationEnabled;
        this.enableGcCoordination = enableGcCoordination;
        this.benchmarkEnabled = benchmarkEnabled;
        this.beCriticalRadiusBlocks = beCriticalRadiusBlocks;
        this.beMaxConsecutiveSkips = beMaxConsecutiveSkips;
        this.diagnosticsSnapshotIntervalTicks = diagnosticsSnapshotIntervalTicks;
        this.benchmarkPhaseDurationSeconds = benchmarkPhaseDurationSeconds;
        this.benchmarkWarmupTicks = benchmarkWarmupTicks;
        this.benchmarkCooldownTicks = benchmarkCooldownTicks;
        this.benchmarkStressorCount = benchmarkStressorCount;
        this.benchmarkPhaseTicks = benchmarkPhaseTicks;
        this.hopperOptimizationEnabled = hopperOptimizationEnabled;
        this.redstoneOptimizationEnabled = redstoneOptimizationEnabled;
        this.xpOrbCoalescingEnabled = xpOrbCoalescingEnabled;
        this.poiSpatialHashingEnabled = poiSpatialHashingEnabled;
        this.sodiumMenuIntegrationEnabled = sodiumMenuIntegrationEnabled;
        this.core = core;
        this.budgets = budgets;

        // Map to compatibility fields
        this.masterEnabled = enabled;
        this.scheduledTickWheelEnabled = timingWheelEnabled;
        this.offHeapBlockEntityDataEnabled = offHeapBeMetadataEnabled;
        this.blockEntityThrottlingEnabled = throttleBlockEntities;
        this.blockEntityCriticalRadius = beCriticalRadiusBlocks;
        this.offHeapBeMetadataCapacity = 131072;
        this.simdPaletteEnabled = true;
    }

    public static IngeniumConfig defaults() {
        return new IngeniumConfig(
                true, true, true, true, true, false, true, true, true,
                24, 40, 100L, 30, 100, 40, 500, 200,
                true, true, true, true, true,
                new Core(true, true, 200, 25, 40, 55, 20, 2048, 1.0, true, 4),
                new Budgets(Collections.emptyMap(), Collections.emptyMap())
        );
    }

    // ---- getters ----
    public boolean enabled() { return enabled; }
    public boolean timingWheelEnabled() { return enabled && timingWheelEnabled; }
    public boolean offHeapBeMetadataEnabled() { return enabled && offHeapBeMetadataEnabled; }
    public boolean throttleBlockEntitiesEnabled() { return enabled && throttleBlockEntities; }
    public boolean diagnosticsEnabled() { return enabled && diagnosticsEnabled; }
    public boolean benchPhaseBEnabled() { return enabled && benchPhaseBEnabled; }
    public boolean sparkIntegrationEnabled() { return enabled && sparkIntegrationEnabled; }
    public int beCriticalRadiusBlocks() { return beCriticalRadiusBlocks; }
    public int beMaxConsecutiveSkips() { return beMaxConsecutiveSkips; }
    public long diagnosticsSnapshotIntervalTicks() { return diagnosticsSnapshotIntervalTicks; }

    public Core core() {
        return core != null ? core : defaults().core;
    }

    public Budgets budgets() {
        return budgets != null ? budgets : defaults().budgets;
    }

    // Compatibility aliases
    public boolean throttleBlockEntities() { return throttleBlockEntitiesEnabled(); }
    public boolean governorEnabled() { return enabled(); }
    public int beMaxSkipCount() { return beMaxConsecutiveSkips(); }
    public boolean offHeapBlockEntityDataEnabled() { return offHeapBeMetadataEnabled(); }
    public int offHeapBeMetadataCapacity() { return 131072; } // Default
    public boolean simdEnabled() { return true; }

    public static class Core {
        public final boolean enableExecutors;
        public final boolean governorAutoProfile;
        public final int timeShareResetPeriodTicks;
        public final int thresholdBalancedMspt;
        public final int thresholdReactiveMspt;
        public final int thresholdEmergencyMspt;
        public final int profileStabilityTicks;
        public final int commitQueueCapacity;
        public final double computePoolMultiplier;
        public final boolean useVirtualThreadsForIO;
        public final int ioCoreThreads;

        public Core(boolean enableExecutors, boolean governorAutoProfile, int timeShareResetPeriodTicks,
                    int thresholdBalancedMspt, int thresholdReactiveMspt, int thresholdEmergencyMspt,
                    int profileStabilityTicks, int commitQueueCapacity, double computePoolMultiplier,
                    boolean useVirtualThreadsForIO, int ioCoreThreads) {
            this.enableExecutors = enableExecutors;
            this.governorAutoProfile = governorAutoProfile;
            this.timeShareResetPeriodTicks = timeShareResetPeriodTicks;
            this.thresholdBalancedMspt = thresholdBalancedMspt;
            this.thresholdReactiveMspt = thresholdReactiveMspt;
            this.thresholdEmergencyMspt = thresholdEmergencyMspt;
            this.profileStabilityTicks = profileStabilityTicks;
            this.commitQueueCapacity = commitQueueCapacity;
            this.computePoolMultiplier = computePoolMultiplier;
            this.useVirtualThreadsForIO = useVirtualThreadsForIO;
            this.ioCoreThreads = ioCoreThreads;
        }

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
        private final Map<IngeniumGovernor.SubsystemType, Long> baseBudgets;
        private final Map<IngeniumGovernor.SubsystemType, Boolean> enabledSubsystems;

        public Budgets(Map<IngeniumGovernor.SubsystemType, Long> baseBudgets, Map<IngeniumGovernor.SubsystemType, Boolean> enabledSubsystems) {
            this.baseBudgets = new EnumMap<>(IngeniumGovernor.SubsystemType.class);
            this.enabledSubsystems = new EnumMap<>(IngeniumGovernor.SubsystemType.class);
            
            // Defaults
            for (IngeniumGovernor.SubsystemType s : IngeniumGovernor.SubsystemType.values()) {
                this.baseBudgets.put(s, 1_000_000L);
                this.enabledSubsystems.put(s, true);
            }
            // Overrides
            this.baseBudgets.putAll(baseBudgets);
            this.enabledSubsystems.putAll(enabledSubsystems);
        }

        public boolean isSubsystemEnabled(IngeniumGovernor.SubsystemType subsystem) {
            return enabledSubsystems.getOrDefault(subsystem, true);
        }

        public long baseBudgetNs(IngeniumGovernor.SubsystemType subsystem) {
            return baseBudgets.getOrDefault(subsystem, 1_000_000L);
        }
    }

    public void save() {
        try {
            Files.writeString(path(), GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IngeniumConfig load() {
        Path p = path();
        try {
            if (!Files.exists(p)) {
                IngeniumConfig cfg = defaults();
                cfg.save();
                return cfg;
            }
            String json = Files.readString(p, StandardCharsets.UTF_8);
            IngeniumConfig cfg = GSON.fromJson(json, IngeniumConfig.class);
            if (cfg == null) return defaults();
            // Ensure core and budgets are not null due to partial JSON
            if (cfg.core == null || cfg.budgets == null) {
                cfg = cfg.toBuilder().build();
            }
            return cfg;
        } catch (Exception e) {
            e.printStackTrace();
            return defaults();
        }
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private boolean enabled;
        private boolean timingWheelEnabled;
        private boolean offHeapBeMetadataEnabled;
        private boolean throttleBlockEntities;
        private boolean diagnosticsEnabled;
        private boolean benchPhaseBEnabled;
        private boolean sparkIntegrationEnabled;
        private boolean enableGcCoordination;
        private boolean benchmarkEnabled;
        private int beCriticalRadiusBlocks;
        private int beMaxConsecutiveSkips;
        private long diagnosticsSnapshotIntervalTicks;
        private int benchmarkPhaseDurationSeconds;
        private int benchmarkWarmupTicks;
        private int benchmarkCooldownTicks;
        private int benchmarkStressorCount;
        private int benchmarkPhaseTicks;

        private boolean hopperOptimizationEnabled;
        private boolean redstoneOptimizationEnabled;
        private boolean xpOrbCoalescingEnabled;
        private boolean poiSpatialHashingEnabled;
        private boolean sodiumMenuIntegrationEnabled;
        
        // Core fields
        private boolean enableExecutors;
        private boolean governorAutoProfile;
        private int timeShareResetPeriodTicks;
        private int thresholdBalancedMspt;
        private int thresholdReactiveMspt;
        private int thresholdEmergencyMspt;
        private int profileStabilityTicks;
        private int commitQueueCapacity;
        private double computePoolMultiplier;
        private boolean useVirtualThreadsForIO;
        private int ioCoreThreads;

        // Budgets
        private Map<IngeniumGovernor.SubsystemType, Long> baseBudgets = new EnumMap<>(IngeniumGovernor.SubsystemType.class);
        private Map<IngeniumGovernor.SubsystemType, Boolean> enabledSubsystems = new EnumMap<>(IngeniumGovernor.SubsystemType.class);

        public Builder(IngeniumConfig base) {
            this.enabled = base.enabled;
            this.timingWheelEnabled = base.timingWheelEnabled;
            this.offHeapBeMetadataEnabled = base.offHeapBeMetadataEnabled;
            this.throttleBlockEntities = base.throttleBlockEntities;
            this.diagnosticsEnabled = base.diagnosticsEnabled;
            this.benchPhaseBEnabled = base.benchPhaseBEnabled;
            this.sparkIntegrationEnabled = base.sparkIntegrationEnabled;
            this.enableGcCoordination = base.enableGcCoordination;
            this.benchmarkEnabled = base.benchmarkEnabled;
            this.beCriticalRadiusBlocks = base.beCriticalRadiusBlocks;
            this.beMaxConsecutiveSkips = base.beMaxConsecutiveSkips;
            this.diagnosticsSnapshotIntervalTicks = base.diagnosticsSnapshotIntervalTicks;
            this.benchmarkPhaseDurationSeconds = base.benchmarkPhaseDurationSeconds;
            this.benchmarkWarmupTicks = base.benchmarkWarmupTicks;
            this.benchmarkCooldownTicks = base.benchmarkCooldownTicks;
            this.benchmarkStressorCount = base.benchmarkStressorCount;
            this.benchmarkPhaseTicks = base.benchmarkPhaseTicks;
            this.hopperOptimizationEnabled = base.hopperOptimizationEnabled;
            this.redstoneOptimizationEnabled = base.redstoneOptimizationEnabled;
            this.xpOrbCoalescingEnabled = base.xpOrbCoalescingEnabled;
            this.poiSpatialHashingEnabled = base.poiSpatialHashingEnabled;
            this.sodiumMenuIntegrationEnabled = base.sodiumMenuIntegrationEnabled;
            
            Core c = base.core != null ? base.core : defaults().core;
            this.enableExecutors = c.enableExecutors;
            this.governorAutoProfile = c.governorAutoProfile;
            this.timeShareResetPeriodTicks = c.timeShareResetPeriodTicks;
            this.thresholdBalancedMspt = c.thresholdBalancedMspt;
            this.thresholdReactiveMspt = c.thresholdReactiveMspt;
            this.thresholdEmergencyMspt = c.thresholdEmergencyMspt;
            this.profileStabilityTicks = c.profileStabilityTicks;
            this.commitQueueCapacity = c.commitQueueCapacity;
            this.computePoolMultiplier = c.computePoolMultiplier;
            this.useVirtualThreadsForIO = c.useVirtualThreadsForIO;
            this.ioCoreThreads = c.ioCoreThreads;
            
            Budgets b = base.budgets != null ? base.budgets : defaults().budgets;
            this.baseBudgets.putAll(b.baseBudgets);
            this.enabledSubsystems.putAll(b.enabledSubsystems);
        }

        public Builder enabled(boolean v) { this.enabled = v; return this; }
        public Builder timingWheelEnabled(boolean v) { this.timingWheelEnabled = v; return this; }
        public Builder offHeapBeMetadataEnabled(boolean v) { this.offHeapBeMetadataEnabled = v; return this; }
        public Builder throttleBlockEntities(boolean v) { this.throttleBlockEntities = v; return this; }
        public Builder diagnosticsEnabled(boolean v) { this.diagnosticsEnabled = v; return this; }
        public Builder benchPhaseBEnabled(boolean v) { this.benchPhaseBEnabled = v; return this; }
        public Builder sparkIntegrationEnabled(boolean v) { this.sparkIntegrationEnabled = v; return this; }
        public Builder enableGcCoordination(boolean v) { this.enableGcCoordination = v; return this; }
        public Builder benchmarkEnabled(boolean v) { this.benchmarkEnabled = v; return this; }
        public Builder beCriticalRadiusBlocks(int v) { this.beCriticalRadiusBlocks = v; return this; }
        public Builder beMaxConsecutiveSkips(int v) { this.beMaxConsecutiveSkips = v; return this; }
        public Builder diagnosticsSnapshotIntervalTicks(long v) { this.diagnosticsSnapshotIntervalTicks = v; return this; }
        public Builder benchmarkPhaseDurationSeconds(int v) { this.benchmarkPhaseDurationSeconds = v; return this; }
        public Builder benchmarkWarmupTicks(int v) { this.benchmarkWarmupTicks = v; return this; }
        public Builder benchmarkCooldownTicks(int v) { this.benchmarkCooldownTicks = v; return this; }
        public Builder benchmarkStressorCount(int v) { this.benchmarkStressorCount = v; return this; }
        public Builder benchmarkPhaseTicks(int v) { this.benchmarkPhaseTicks = v; return this; }
        public Builder hopperOptimizationEnabled(boolean v) { this.hopperOptimizationEnabled = v; return this; }
        public Builder redstoneOptimizationEnabled(boolean v) { this.redstoneOptimizationEnabled = v; return this; }
        public Builder xpOrbCoalescingEnabled(boolean v) { this.xpOrbCoalescingEnabled = v; return this; }
        public Builder poiSpatialHashingEnabled(boolean v) { this.poiSpatialHashingEnabled = v; return this; }
        public Builder sodiumMenuIntegrationEnabled(boolean v) { this.sodiumMenuIntegrationEnabled = v; return this; }

        public boolean enabled() { return enabled; }
        public boolean timingWheelEnabled() { return timingWheelEnabled; }
        public boolean offHeapBeMetadataEnabled() { return offHeapBeMetadataEnabled; }
        public boolean throttleBlockEntities() { return throttleBlockEntities; }
        public boolean diagnosticsEnabled() { return diagnosticsEnabled; }
        public boolean benchPhaseBEnabled() { return benchPhaseBEnabled; }
        public boolean sparkIntegrationEnabled() { return sparkIntegrationEnabled; }
        public boolean enableGcCoordination() { return enableGcCoordination; }
        public boolean benchmarkEnabled() { return benchmarkEnabled; }
        public int beCriticalRadiusBlocks() { return beCriticalRadiusBlocks; }
        public int beMaxConsecutiveSkips() { return beMaxConsecutiveSkips; }
        public long diagnosticsSnapshotIntervalTicks() { return diagnosticsSnapshotIntervalTicks; }
        public int benchmarkPhaseDurationSeconds() { return benchmarkPhaseDurationSeconds; }
        public int benchmarkWarmupTicks() { return benchmarkWarmupTicks; }
        public int benchmarkCooldownTicks() { return benchmarkCooldownTicks; }
        public int benchmarkStressorCount() { return benchmarkStressorCount; }
        public int benchmarkPhaseTicks() { return benchmarkPhaseTicks; }
        public boolean hopperOptimizationEnabled() { return hopperOptimizationEnabled; }
        public boolean redstoneOptimizationEnabled() { return redstoneOptimizationEnabled; }
        public boolean xpOrbCoalescingEnabled() { return xpOrbCoalescingEnabled; }
        public boolean poiSpatialHashingEnabled() { return poiSpatialHashingEnabled; }
        public boolean sodiumMenuIntegrationEnabled() { return sodiumMenuIntegrationEnabled; }

        public IngeniumConfig build() {
            return new IngeniumConfig(
                    enabled, timingWheelEnabled, offHeapBeMetadataEnabled, throttleBlockEntities,
                    diagnosticsEnabled, benchPhaseBEnabled, sparkIntegrationEnabled, enableGcCoordination,
                    benchmarkEnabled, beCriticalRadiusBlocks, beMaxConsecutiveSkips, diagnosticsSnapshotIntervalTicks,
                    benchmarkPhaseDurationSeconds, benchmarkWarmupTicks, benchmarkCooldownTicks, benchmarkStressorCount,
                    benchmarkPhaseTicks, hopperOptimizationEnabled, redstoneOptimizationEnabled, xpOrbCoalescingEnabled,
                    poiSpatialHashingEnabled, sodiumMenuIntegrationEnabled,
                    new Core(enableExecutors, governorAutoProfile, timeShareResetPeriodTicks,
                            thresholdBalancedMspt, thresholdReactiveMspt, thresholdEmergencyMspt,
                            profileStabilityTicks, commitQueueCapacity, computePoolMultiplier,
                            useVirtualThreadsForIO, ioCoreThreads),
                    new Budgets(baseBudgets, enabledSubsystems)
            );
        }
    }

    public static IngeniumConfig getInstance() { return get(); }

    public enum ImpactLevel {
        LOW(net.minecraft.ChatFormatting.GREEN, "Low impact"),
        MEDIUM(net.minecraft.ChatFormatting.YELLOW, "Medium impact"),
        HIGH(net.minecraft.ChatFormatting.RED, "High impact");

        public final net.minecraft.ChatFormatting color;
        public final String label;

        ImpactLevel(net.minecraft.ChatFormatting color, String label) {
            this.color = color;
            this.label = label;
        }

        public net.minecraft.network.chat.Component asText() {
            return net.minecraft.network.chat.Component.literal("Impact: " + label).withStyle(color);
        }
    }
}
