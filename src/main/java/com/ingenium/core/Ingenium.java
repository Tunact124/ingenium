package com.ingenium.core;

import com.ingenium.benchmark.IngeniumBenchmarkService;
import com.ingenium.benchmark.IngeniumDiagnostics;
import com.ingenium.command.IngeniumCommand;
import com.ingenium.compat.CompatibilityRegistry;
import com.ingenium.compat.IrisCompatibilityLayer;
import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.hw.HardwareProbe;
import com.ingenium.core.hw.HardwareProfile;
import com.ingenium.core.spark.SparkIntegration;
import com.ingenium.metrics.IngeniumMetrics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ingenium implements ModInitializer {
    public static final String MOD_ID = "ingenium";
    public static final String MODID = MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger("Ingenium");
    public static final Logger LOG = LOGGER;

    private static volatile IngeniumRuntime runtime;

    @Override
    public void onInitialize() {
        try {
            final EnvType env = FabricLoader.getInstance().getEnvironmentType();

            // Section 1.1 - SIMDCapability initializes itself on first access or class load
            com.ingenium.compat.BuddyLogic.fullInit();

            // Section 2 (hardware assessment)
            HardwareProfile hw = HardwareProbe.probe(LOGGER);

            // Section 1.2 metrics
            // Choose a power-of-two; 2048 samples is plenty and cheap.
            IngeniumMetrics metrics = new IngeniumMetrics(2048, env);

            IngeniumGovernor gov = IngeniumGovernor.get();
            runtime = new IngeniumRuntime(env, metrics, hw, gov);

            LOGGER.info("[Ingenium] Initialized. env={}, tier={}, score={}",
                    env, hw.tier(), hw.qualityScore());

            // Legacy Wiring
            IrisCompatibilityLayer.detectEarly();
            IngeniumConfig.init();
            CompatibilityRegistry.init();
            SparkIntegration.initSoft();
            IngeniumBenchmarkService.get().init();

            var jit = new com.ingenium.jit.JitEnvironmentDetector();
            jit.generateRecommendations().forEach(LOGGER::info);

            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, cmdEnv) -> {
                IngeniumCommand.register(dispatcher);
                LOGGER.info("Commands registered (env={})", cmdEnv);
            });

            ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
            ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
            ServerTickEvents.START_SERVER_TICK.register(this::onTickStart);
            ServerTickEvents.END_SERVER_TICK.register(this::onTickEnd);

            LOGGER.info("Ingenium Ready. Compat: {}", IrisCompatibilityLayer.summary());
        } catch (Throwable t) {
            IngeniumSafetySystem.reportFailure("Ingenium:onInitialize", t);
        }
    }

    public static IngeniumRuntime runtime() {
        var r = runtime;
        if (r == null) {
            throw new IllegalStateException("Ingenium runtime accessed before initialization");
        }
        return r;
    }

    private void onServerStarting(MinecraftServer server) {
        try {
            final var config = IngeniumConfig.get();
            IngeniumGovernor.get().attach(server, config);
            IngeniumBenchmarkService.get().initialize(server);

            if (config != null && config.core().enableExecutors()) {
                IngeniumExecutors.ensureStarted();
            }

            IngeniumDiagnostics.get().onServerStartThreadCaptured();

            LOGGER.info("Ingenium started. Governor profile={} (auto={})",
                    IngeniumGovernor.get().profile(),
                    config == null || config.core().governorAutoProfile());
        } catch (Throwable t) {
            IngeniumSafetySystem.reportFailure("Ingenium:onServerStarting", t);
        }
    }

    private void onServerStopping(MinecraftServer server) {
        try {
            IngeniumBenchmarkService.get().shutdown();
        } catch (Throwable t) {
            LOGGER.warn("Benchmark shutdown error", t);
        }

        try {
            IngeniumExecutors.shutdown();
        } catch (Throwable t) {
            LOGGER.warn("Executors shutdown error", t);
        }

        try {
            IngeniumGovernor.get().detach();
        } catch (Throwable t) {
            LOGGER.warn("Governor detach error", t);
        }
    }

    private void onTickStart(MinecraftServer server) {
        try {
            final long now = System.nanoTime();
            IngeniumDiagnostics.get().onTickStart(now);

            final var gov = IngeniumGovernor.get();
            gov.onTickStart();

            if (IngeniumConfig.get().core().enableExecutors()) {
                try (var ignored = gov.time(IngeniumGovernor.SubsystemType.CORE_COMMIT_QUEUE)) {
                    IngeniumExecutors.processCommitQueue(gov.reinsertionCapHint());
                }
            }
        } catch (Throwable t) {
            IngeniumSafetySystem.reportFailure("Ingenium:onTickStart", t);
        }
    }

    private void onTickEnd(MinecraftServer server) {
        try {
            final long now = System.nanoTime();
            IngeniumGovernor.get().onTickEnd();
            IngeniumDiagnostics.get().onTickEnd(now);
            IngeniumBenchmarkService.get().onTick(server.getTickCount());

            SparkIntegration.reportTickSoft();
        } catch (Throwable t) {
            IngeniumSafetySystem.reportFailure("Ingenium:onTickEnd", t);
        }
    }
}
