package com.ingenium;

import com.ingenium.benchmark.IngeniumBenchmarkService;
import com.ingenium.benchmark.IngeniumDiagnostics;
import com.ingenium.command.IngeniumCommands;
import com.ingenium.compat.CompatibilityRegistry;
import com.ingenium.compat.IrisCompatibilityLayer;
import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.IngeniumExecutors;
import com.ingenium.core.IngeniumGovernor;
import com.ingenium.core.IngeniumGovernor.Subsystem;
import com.ingenium.core.IngeniumGovernor.SubsystemTimer;
import com.ingenium.core.spark.SparkIntegration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mod initializer and lifecycle wiring for Ingenium.
 *
 * <p>Design goals:
 * <ul>
 *   <li>Keep Minecraft/Fabric hooks thin.</li>
 *   <li>Push all heavy logic into core systems (Governor/Executors/etc.).</li>
 *   <li>Ensure safe fallbacks: if config disables a system, nothing breaks.</li>
 * </ul>
 */
public final class IngeniumMod implements ModInitializer {
    public static final String MODID = "ingenium";
    public static final String MOD_ID = MODID;
    private static final Logger LOGGER = LogManager.getLogger("Ingenium");
    public static final Logger LOG = LOGGER;

    @Override
    public void onInitialize() {
        LOGGER.info("Ingenium initializing (MC 1.20.1, Java {}).", Runtime.version());

        IrisCompatibilityLayer.detectEarly();
        IngeniumConfig.init();
        CompatibilityRegistry.init();
        SparkIntegration.initSoft();
        IngeniumBenchmarkService.get().init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            IngeniumCommands.register(dispatcher);
            LOGGER.info("Commands registered (env={})", environment);
        });

        // Server lifecycle: create/close singleton subsystems on server boundaries.
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

        // Tick wiring: measure MSPT, rotate budgets, process commit queue.
        ServerTickEvents.START_SERVER_TICK.register(this::onTickStart);
        ServerTickEvents.END_SERVER_TICK.register(this::onTickEnd);

        LOGGER.info("Ingenium Ready. Compat: {}", IrisCompatibilityLayer.summary());
    }

    private void onServerStarting(MinecraftServer server) {
        final var config = IngeniumConfig.get();

        IngeniumGovernor.get().attach(server, config);

        // Executors are safe to init lazily, but we prime them here to surface misconfig early.
        if (config != null && config.core().enableExecutors()) {
            IngeniumExecutors.ensureStarted();
        }

        IngeniumDiagnostics.get().onServerStartThreadCaptured();

        LOGGER.info("Ingenium started. Governor profile={} (auto={})",
                IngeniumGovernor.get().profile(),
                config == null || config.core().governorAutoProfile());
    }

    private void onServerStopping(MinecraftServer server) {
        try {
            IngeniumExecutors.shutdown();
        } catch (Throwable t) {
            LOGGER.warn("Executors shutdown encountered an error (ignored).", t);
        }

        try {
            IngeniumGovernor.get().detach();
        } catch (Throwable t) {
            LOGGER.warn("Governor detach encountered an error (ignored).", t);
        }
    }

    private void onTickStart(MinecraftServer server) {
        final var gov = IngeniumGovernor.get();
        gov.onTickStart();

        // If other subsystems need main-thread commits, process a bounded number per tick.
        // This is intentionally *before* tick-end MSPT sampling so it counts into MSPT.
        if (IngeniumConfig.get().core().enableExecutors()) {
            try (SubsystemTimer ignored = gov.time(Subsystem.CORE_COMMIT_QUEUE)) {
                IngeniumExecutors.processCommitQueue(gov.reinsertionCapHint());
            }
        }
    }

    private void onTickEnd(MinecraftServer server) {
        IngeniumGovernor.get().onTickEnd();
        SparkIntegration.reportTickSoft();
        
        // Backward compatibility for diagnostics if it still expects raw tick time
        // Note: Governor already calculated MSPT.
    }
}
