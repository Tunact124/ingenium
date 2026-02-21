package com.ingenium.config;

import com.ingenium.config.IngeniumConfig.ImpactLevel;
import dev.isxander.yacl3.api.Binding;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.LongFieldControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * YACL v3 screen builder for Ingenium.
 *
 * <p>Architect: wire persistence to disk by hooking the save callback to your chosen serializer.
 */
public final class IngeniumYaclScreen {

    private IngeniumYaclScreen() {}

    /**
     * Build the config screen.
     *
     * @param parent parent screen
     * @return YACL screen
     */
    public static Screen create(Screen parent) {
        final IngeniumConfig initial = IngeniumConfig.get();
        final IngeniumConfig.Builder builder = initial.toBuilder();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Ingenium Optimization"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("General"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Enabled"))
                                .description(OptionDescription.of(
                                        Component.literal("Master toggle for all Ingenium features."),
                                        ImpactLevel.HIGH.asText()
                                ))
                                .binding(initial.enabled(), () -> builder.enabled(), (v) -> builder.enabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Ticking"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Timing Wheel Scheduler"))
                                .description(OptionDescription.of(
                                        Component.literal("Replace scheduled tick scheduler with timing wheel."),
                                        ImpactLevel.HIGH.asText()
                                ))
                                .binding(initial.timingWheelEnabled(), () -> builder.timingWheelEnabled(), (v) -> builder.timingWheelEnabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Specialist Gaps"))
                        .option(toggle("Hopper throttling", () -> builder.hopperOptimizationEnabled(), value -> builder.hopperOptimizationEnabled(value),
                                "Reduces hopper CPU when pushing into full inventories."))
                        .option(toggle("Redstone property caching", () -> builder.redstoneOptimizationEnabled(), value -> builder.redstoneOptimizationEnabled(value),
                                "Caches selected property access paths to reduce redstone update overhead."))
                        .option(toggle("XP orb coalescing", () -> builder.xpOrbCoalescingEnabled(), value -> builder.xpOrbCoalescingEnabled(value),
                                "Reduces XP orb merge scans and prioritizes count-aware merges."))
                        .option(toggle("POI spatial hashing", () -> builder.poiSpatialHashingEnabled(), value -> builder.poiSpatialHashingEnabled(value),
                                "Adds a per-tick spatial index for repeated villager POI queries."))
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Block Entities"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Throttle Block Entity Ticks"))
                                .description(OptionDescription.of(
                                        Component.literal("Skip non-critical block entity ticks under load."),
                                        ImpactLevel.HIGH.asText()
                                ))
                                .binding(initial.throttleBlockEntitiesEnabled(), () -> builder.throttleBlockEntities(), (v) -> builder.throttleBlockEntities(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Off-Heap BE Metadata"))
                                .description(OptionDescription.of(
                                        Component.literal("Store BE throttling metadata off-heap to reduce GC pressure."),
                                        ImpactLevel.HIGH.asText()
                                ))
                                .binding(initial.offHeapBeMetadataEnabled(), () -> builder.offHeapBeMetadataEnabled(), (v) -> builder.offHeapBeMetadataEnabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Critical Radius (blocks)"))
                                .description(OptionDescription.of(
                                        Component.literal("Block entities within this radius of any player always tick."),
                                        ImpactLevel.MEDIUM.asText()
                                ))
                                .binding(initial.beCriticalRadiusBlocks(), () -> builder.beCriticalRadiusBlocks(), (v) -> builder.beCriticalRadiusBlocks(v))
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 128).step(1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Max Consecutive Skips"))
                                .description(OptionDescription.of(
                                        Component.literal("Safety cap: after this many skips, force a tick."),
                                        ImpactLevel.MEDIUM.asText()
                                ))
                                .binding(initial.beMaxConsecutiveSkips(), () -> builder.beMaxConsecutiveSkips(), (v) -> builder.beMaxConsecutiveSkips(v))
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 200).step(1))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Diagnostics"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Enable Diagnostics"))
                                .description(OptionDescription.of(
                                        Component.literal("Collect subsystem timings and feed data to the Governor."),
                                        ImpactLevel.LOW.asText()
                                ))
                                .binding(initial.diagnosticsEnabled(), () -> builder.diagnosticsEnabled(), (v) -> builder.diagnosticsEnabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Long>createBuilder()
                                .name(Component.literal("Snapshot Interval (ticks)"))
                                .description(OptionDescription.of(
                                        Component.literal("How often to snapshot rolling averages into the Governor."),
                                        ImpactLevel.LOW.asText()
                                ))
                                .binding(initial.diagnosticsSnapshotIntervalTicks(), () -> builder.diagnosticsSnapshotIntervalTicks(), (v) -> builder.diagnosticsSnapshotIntervalTicks(v))
                                .controller(LongFieldControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Enable Phase B Bench"))
                                .description(OptionDescription.of(
                                        Component.literal("Allow bounded synthetic probes for stress testing (off by default)."),
                                        ImpactLevel.LOW.asText()
                                ))
                                .binding(initial.benchPhaseBEnabled(), () -> builder.benchPhaseBEnabled(), (v) -> builder.benchPhaseBEnabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Spark Integration"))
                                .description(OptionDescription.of(
                                        Component.literal("Soft-enable Spark bridge hooks if Spark is installed."),
                                        ImpactLevel.LOW.asText()
                                ))
                                .binding(initial.sparkIntegrationEnabled(), () -> builder.sparkIntegrationEnabled(), (v) -> builder.sparkIntegrationEnabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Integration & Benchmark"))
                        .option(toggle("Enable Benchmark", () -> builder.benchmarkEnabled(), value -> builder.benchmarkEnabled(value),
                                "Allow running Phase A/B benchmarks via /ingenium bench."))
                        .option(toggle("Sodium menu integration", () -> builder.sodiumMenuIntegrationEnabled(), value -> builder.sodiumMenuIntegrationEnabled(value),
                                "Adds an Ingenium button inside Sodium’s options UI (if available)."))
                        .option(intField("Benchmark stressor count", 0, 20_000,
                                () -> builder.benchmarkStressorCount(), value -> builder.benchmarkStressorCount(value),
                                "Entity count used by /ingenium benchmark."))
                        .option(intField("Benchmark phase ticks", 20, 10_000,
                                () -> builder.benchmarkPhaseTicks(), value -> builder.benchmarkPhaseTicks(value),
                                "Duration per phase (OFF then ON)."))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Phase Duration (seconds)"))
                                .binding(initial.benchmarkPhaseDurationSeconds, () -> builder.benchmarkPhaseDurationSeconds(), (v) -> builder.benchmarkPhaseDurationSeconds(v))
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(5, 300).step(5))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Warmup Ticks"))
                                .binding(initial.benchmarkWarmupTicks, () -> builder.benchmarkWarmupTicks(), (v) -> builder.benchmarkWarmupTicks(v))
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(20, 1200).step(20))
                                .build())
                        .build())
                .save(() -> {
                    // Apply snapshot atomically.
                    IngeniumConfig newConfig = builder.build();
                    IngeniumConfig.set(newConfig);
                    ConfigIO.save();
                })
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> toggle(
            String name,
            java.util.function.BooleanSupplier getter,
            java.util.function.Consumer<Boolean> setter,
            String description
    ) {
        return Option.<Boolean>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(true, () -> getter.getAsBoolean(), setter)
                .controller(BooleanControllerBuilder::create)
                .build();
    }

    private static Option<Integer> intField(
            String name,
            int min,
            int max,
            java.util.function.IntSupplier getter,
            java.util.function.IntConsumer setter,
            String description
    ) {
        return Option.<Integer>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(min, () -> getter.getAsInt(), setter::accept)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(min).max(max))
                .build();
    }
}
