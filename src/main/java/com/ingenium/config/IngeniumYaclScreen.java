package com.ingenium.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.LongFieldControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

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
                .title(Text.literal("Ingenium Optimization"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("General"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Enabled"))
                                .description(OptionDescription.of(Text.literal("Master toggle for all Ingenium features.")))
                                .binding(initial.enabled(), () -> builder.enabled(), (v) -> builder.enabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Ticking"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Timing Wheel Scheduler"))
                                .description(OptionDescription.of(Text.literal("Replace scheduled tick scheduler with timing wheel.")))
                                .binding(initial.timingWheelEnabled(), () -> builder.timingWheelEnabled(), (v) -> builder.timingWheelEnabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Block Entities"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Throttle Block Entity Ticks"))
                                .description(OptionDescription.of(Text.literal("Skip non-critical block entity ticks under load.")))
                                .binding(initial.throttleBlockEntitiesEnabled(), () -> builder.throttleBlockEntities(), (v) -> builder.throttleBlockEntities(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Off-Heap BE Metadata"))
                                .description(OptionDescription.of(Text.literal("Store BE throttling metadata off-heap to reduce GC pressure.")))
                                .binding(initial.offHeapBeMetadataEnabled(), () -> builder.offHeapBeMetadataEnabled(), (v) -> builder.offHeapBeMetadataEnabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Critical Radius (blocks)"))
                                .description(OptionDescription.of(Text.literal("Block entities within this radius of any player always tick.")))
                                .binding(initial.beCriticalRadiusBlocks(), () -> builder.beCriticalRadiusBlocks(), (v) -> builder.beCriticalRadiusBlocks(v))
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 128).step(1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Max Consecutive Skips"))
                                .description(OptionDescription.of(Text.literal("Safety cap: after this many skips, force a tick.")))
                                .binding(initial.beMaxConsecutiveSkips(), () -> builder.beMaxConsecutiveSkips(), (v) -> builder.beMaxConsecutiveSkips(v))
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 200).step(1))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Diagnostics"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Enable Diagnostics"))
                                .description(OptionDescription.of(Text.literal("Collect subsystem timings and feed data to the Governor.")))
                                .binding(initial.diagnosticsEnabled(), () -> builder.diagnosticsEnabled(), (v) -> builder.diagnosticsEnabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Long>createBuilder()
                                .name(Text.literal("Snapshot Interval (ticks)"))
                                .description(OptionDescription.of(Text.literal("How often to snapshot rolling averages into the Governor.")))
                                .binding(initial.diagnosticsSnapshotIntervalTicks(), () -> builder.diagnosticsSnapshotIntervalTicks(), (v) -> builder.diagnosticsSnapshotIntervalTicks(v))
                                .controller(LongFieldControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Enable Phase B Bench"))
                                .description(OptionDescription.of(Text.literal("Allow bounded synthetic probes for stress testing (off by default).")))
                                .binding(initial.benchPhaseBEnabled(), () -> builder.benchPhaseBEnabled(), (v) -> builder.benchPhaseBEnabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Spark Integration"))
                                .description(OptionDescription.of(Text.literal("Soft-enable Spark bridge hooks if Spark is installed.")))
                                .binding(initial.sparkIntegrationEnabled(), () -> builder.sparkIntegrationEnabled(), (v) -> builder.sparkIntegrationEnabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Benchmark"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Enable Benchmark"))
                                .description(OptionDescription.of(Text.literal("Allow running Phase A/B benchmarks via /ingenium bench.")))
                                .binding(initial.benchmarkEnabled, () -> builder.benchmarkEnabled(), (v) -> builder.benchmarkEnabled(v))
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Phase Duration (seconds)"))
                                .binding(initial.benchmarkPhaseDurationSeconds, () -> builder.benchmarkPhaseDurationSeconds(), (v) -> builder.benchmarkPhaseDurationSeconds(v))
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(5, 300).step(5))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Warmup Ticks"))
                                .binding(initial.benchmarkWarmupTicks, () -> builder.benchmarkWarmupTicks(), (v) -> builder.benchmarkWarmupTicks(v))
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(20, 1200).step(20))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Stressor Count"))
                                .binding(initial.benchmarkStressorCount, () -> builder.benchmarkStressorCount(), (v) -> builder.benchmarkStressorCount(v))
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 2000).step(50))
                                .build())
                        .build())
                .save(() -> {
                    // Apply snapshot atomically.
                    IngeniumConfig newConfig = builder.build();
                    IngeniumConfig.set(newConfig);
                    newConfig.save();
                })
                .build()
                .generateScreen(parent);
    }
}
