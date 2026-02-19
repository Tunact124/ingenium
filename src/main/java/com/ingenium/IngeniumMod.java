package com.ingenium;

import com.ingenium.benchmark.SafeMetricsCollector;
import com.ingenium.command.IngeniumCommand;
import com.ingenium.config.IngeniumConfig;
import com.ingenium.governor.IngeniumGovernor;
import com.ingenium.memory.GcHintScheduler;
import com.ingenium.threading.ChunkStampRegistry;
import com.ingenium.threading.IngeniumExecutors;
import com.ingenium.util.IngeniumLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

public class IngeniumMod implements ModInitializer {
 
    public static final IngeniumGovernor GOVERNOR = new IngeniumGovernor();
    public static volatile SafeMetricsCollector activeBenchmarkCollector = null;
 
    @Override
    public void onInitialize() {
        // 1. Load config
        IngeniumConfig.HANDLER.load();
        com.ingenium.compat.CompatibilityBridge.logSummary();
 
        // 2. Register chunk events for stamp invalidation
        ChunkStampRegistry.registerChunkEvents();
 
        // 3. Wire governor — START records tick start time (MSPT bug fix)
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (activeBenchmarkCollector != null) activeBenchmarkCollector.recordTick();
            GOVERNOR.onTickStart(server);
            IngeniumExecutors.drainCommitQueue(
                GOVERNOR.getAiBudgetMs(),
                IngeniumConfig.get().maxCommitsPerTick
            );
        });
        ServerTickEvents.END_SERVER_TICK.register(GOVERNOR::onTickEnd);
 
        // 4. Register Governor listeners
        GOVERNOR.register(new GcHintScheduler());
 
        // 5. Wire activity classifiers
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(
            GOVERNOR.getClassifier()::onCombat);
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            GOVERNOR.getClassifier().onBlockInteract(world, (PlayerEntity) player,
                hit.getBlockPos());
            return ActionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register(
            (w, p, pos, s, be) -> GOVERNOR.getClassifier().onBlockInteract(w, p, pos));
 
        // 6. Register /ingenium command
        CommandRegistrationCallback.EVENT.register(
            (d, r, e) -> IngeniumCommand.register(d));
 
        IngeniumLogger.info("Ingenium v2 initialized.");
    }
}
