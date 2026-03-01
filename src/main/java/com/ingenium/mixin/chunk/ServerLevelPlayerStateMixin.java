package com.ingenium.mixin.chunk;

import com.ingenium.chunk.ChunkPriorityEngine;
import com.ingenium.core.IngeniumSafetySystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelPlayerStateMixin {

    /**
     * Update player state in ChunkPriorityEngine once per tick.
     * We use the first player in the level for scoring.
     * In a multi-player environment, a more sophisticated approach
     * (like per-player engine or combined scoring) might be better.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void ingenium_updatePlayerStateForPriority(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        IngeniumSafetySystem.guard("chunk_priority_update_player", () -> {
            ServerLevel self = (ServerLevel) (Object) this;
            ChunkPriorityEngine engine = ChunkPriorityAccess.getEngine(self);
            if (engine == null)
                return;

            // Avoid allocating a full List<ServerPlayer> every single tick
            // as self.players() often copies or instantiates collection wrappers.
            // Just grab the first available player via the raw players collection.
            for (ServerPlayer player : self.players()) {
                if (player != null) {
                    engine.updatePlayerState(
                            player.position(),
                            player.getLookAngle(),
                            player.getDeltaMovement(),
                            self.getGameTime());
                    break; // Only need one player for global anchor
                }
            }
        });
    }
}
