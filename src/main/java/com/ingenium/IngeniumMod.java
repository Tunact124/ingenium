package com.ingenium;

import com.ingenium.benchmark.IngeniumBenchmarkService;
import com.ingenium.benchmark.IngeniumDiagnostics;
import com.ingenium.command.IngeniumCommands;
import com.ingenium.compat.CompatibilityRegistry;
import com.ingenium.compat.IrisCompatibilityLayer;
import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.IngeniumExecutors;
import com.ingenium.core.IngeniumGovernor;
import com.ingenium.core.spark.SparkIntegration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IngeniumMod implements ModInitializer {
    public static final String MOD_ID = "ingenium";
    public static final Logger LOG = LoggerFactory.getLogger("Ingenium");

    private long tickStartNs;

    @Override
    public void onInitialize() {
        IrisCompatibilityLayer.detectEarly();
        IngeniumConfig.init();
        CompatibilityRegistry.init();

        LOG.info("[Ingenium] Initializing (MC=1.20.1 Yarn=1.20.1+build.10 Java={})", Runtime.version());
        LOG.info("[Ingenium] Config: {}", IngeniumConfig.get());

        SparkIntegration.initSoft();
        IngeniumCommands.register();
        IngeniumBenchmarkService.get().init();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            IngeniumExecutors.init();
            IngeniumGovernor.get().onServerStart(server);
            IngeniumDiagnostics.get().onServerStartThreadCaptured();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            IngeniumGovernor.get().onServerStopping();
            IngeniumExecutors.shutdown();
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (!IngeniumConfig.get().masterEnabled) return;
            tickStartNs = System.nanoTime();
            IngeniumGovernor.get().onTickStart(tickStartNs);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!IngeniumConfig.get().masterEnabled) return;
            long tickNs = System.nanoTime() - tickStartNs;
            IngeniumGovernor.get().onTickEnd(tickStartNs);
            IngeniumDiagnostics.get().onTickEnd(tickNs);
            IngeniumExecutors.drainCommitQueue(1_500_000L);
            SparkIntegration.reportTickSoft();
        });

        LOG.info("[Ingenium] Ready. Compat: {}", IrisCompatibilityLayer.summary());
    }
}
