package com.ingenium.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class IngeniumConfigScreen {
 
    public static Screen create(Screen parent) {
        IngeniumConfig cfg = IngeniumConfig.get();
 
        return YetAnotherConfigLib.createBuilder()
            .title(Text.of("Ingenium Settings"))
 
            // ── TAB 1: General ─────────────────────────────────────────────
            .category(ConfigCategory.createBuilder()
                .name(Text.of("General"))
                .tooltip(Text.of("Core governor and compatibility settings."))
 
                .group(OptionGroup.createBuilder()
                    .name(Text.of("Performance Governor"))
                    .collapsed(false)
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.of("Enable Governor"))
                        .description(OptionDescription.of(Text.of(
                            "The Governor profiles MSPT and player activity to\n" +
                            "dynamically adjust all optimization subsystems.")))
                        .binding(true,
                            () -> cfg.enableGovernor,
                            v -> cfg.enableGovernor = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.<Integer>createBuilder()
                        .name(Text.of("Emergency MSPT Threshold"))
                        .description(OptionDescription.of(Text.of(
                            "MSPT above this value triggers EMERGENCY profile.")))
                        .binding(45,
                            () -> cfg.msptEmergencyThreshold,
                            v -> cfg.msptEmergencyThreshold = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                            .range(20, 60).step(1))
                        .build())
                    .option(Option.<Integer>createBuilder()
                        .name(Text.of("Hysteresis Window (ticks)"))
                        .description(OptionDescription.of(Text.of(
                            "Ticks a new state must be stable before profile switch.\n" +
                            "Prevents thrashing between profiles.")))
                        .binding(60,
                            () -> cfg.hysteresisTickWindow,
                            v -> cfg.hysteresisTickWindow = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                            .range(0, 120).step(10))
                        .build())
                    .build())
 
                .group(OptionGroup.createBuilder()
                    .name(Text.of("Compatibility"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.of("Verbose Compat Logging"))
                        .binding(false,
                            () -> cfg.verboseCompatLogging,
                            v -> cfg.verboseCompatLogging = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .build())
                .build())
 
            // ── TAB 2: Rendering ───────────────────────────────────────────
            .category(ConfigCategory.createBuilder()
                .name(Text.of("Rendering"))
                .group(OptionGroup.createBuilder()
                    .name(Text.of("Instanced Block Entity Rendering"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.of("Enable Instanced Rendering"))
                        .binding(true,
                            () -> cfg.enableInstancedRendering,
                            v -> cfg.enableInstancedRendering = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.of("Frustum Culling"))
                        .binding(true,
                            () -> cfg.enableFrustumCulling,
                            v -> cfg.enableFrustumCulling = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.of("Dirty Region Tracking"))
                        .description(OptionDescription.of(Text.of(
                            "Only re-uploads instance data for regions that changed.\n" +
                            "Reduces GPU bandwidth on static builds.")))
                        .binding(true,
                            () -> cfg.enableDirtyTracking,
                            v -> cfg.enableDirtyTracking = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .build())
                .build())
 
            // ── TAB 3: Threading ───────────────────────────────────────────
            .category(ConfigCategory.createBuilder()
                .name(Text.of("Threading"))
                .group(OptionGroup.createBuilder()
                    .name(Text.of("Async Workers"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.of("Async Pathfinding"))
                        .binding(true,
                            () -> cfg.enableAsyncPathfinding,
                            v -> cfg.enableAsyncPathfinding = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.of("Async World Save"))
                        .binding(true,
                            () -> cfg.enableAsyncWorldSave,
                            v -> cfg.enableAsyncWorldSave = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.<Integer>createBuilder()
                        .name(Text.of("Max Commits Per Tick"))
                        .binding(20,
                            () -> cfg.maxCommitsPerTick,
                            v -> cfg.maxCommitsPerTick = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                            .range(5, 50).step(5))
                        .build())
                    .build())
                .build())
 
            // ── TAB 4: Memory ──────────────────────────────────────────────
            .category(ConfigCategory.createBuilder()
                .name(Text.of("Memory"))
                .group(OptionGroup.createBuilder()
                    .name(Text.of("Object Pooling"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.of("Enable Object Pooling"))
                        .binding(true,
                            () -> cfg.enableObjectPooling,
                            v -> cfg.enableObjectPooling = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Text.of("Proactive GC Hints"))
                        .binding(true,
                            () -> cfg.enableGcHints,
                            v -> cfg.enableGcHints = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.<Integer>createBuilder()
                        .name(Text.of("BlockPos Pool Capacity (per thread)"))
                        .binding(256,
                            () -> cfg.blockPosPoolCapacity,
                            v -> cfg.blockPosPoolCapacity = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                            .range(64, 1024).step(64))
                        .build())
                    .build())
                .build())
 
            .save(() -> IngeniumConfig.HANDLER.save())
            .build()
            .generateScreen(parent);
    }
}
