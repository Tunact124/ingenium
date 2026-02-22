package com.ingenium.mixin.chunk;

import com.ingenium.chunk.ChunkPriorityEngine;
import com.ingenium.chunk.ChunkPriorityEngine.ChunkWorkType;
import com.ingenium.core.IngeniumSafetySystem;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelChunkPriorityMixin {

    /**
     * Hook into the block change notification path.
     * When a block changes, instead of immediately queuing a chunk rebuild,
     * submit it to the priority engine.
     */
    @Inject(
        method = "onBlockStateChange",
        at = @At("HEAD")
    )
    private void ingenium_prioritizeBlockChange(
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState oldState,
            net.minecraft.world.level.block.state.BlockState newState,
            CallbackInfo ci) {
        IngeniumSafetySystem.guard("chunk_priority_block_change", () -> {
            ChunkPriorityEngine engine = ChunkPriorityAccess.getEngine((ServerLevel)(Object)this);
            if (engine == null) return;

            SectionPos section = SectionPos.of(pos);
            long currentTick = ((ServerLevel)(Object)this).getGameTime();
            engine.submit(section, ChunkWorkType.BLOCK_CHANGE, currentTick);
        });
    }
}
