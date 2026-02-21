package com.ingenium.mixin;

import com.ingenium.util.ChunkStampRegistry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class ChunkStampMixin {

    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void ingenium$invalidateOnBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        long packed = ChunkPos.asLong(pos);
        ChunkStampRegistry.invalidate(packed);
    }

    @Inject(method = "addAndRegisterBlockEntity", at = @At("TAIL"))
    private void ingenium$invalidateOnAddBE(BlockEntity be, CallbackInfo ci) {
        long packed = ChunkPos.asLong(be.getBlockPos());
        ChunkStampRegistry.invalidate(packed);
    }

    @Inject(method = "removeBlockEntity", at = @At("TAIL"))
    private void ingenium$invalidateOnRemoveBE(BlockPos pos, CallbackInfo ci) {
        long packed = ChunkPos.asLong(pos);
        ChunkStampRegistry.invalidate(packed);
    }

    @Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
    private void ingenium$invalidateOnUnload(CallbackInfo ci) {
        LevelChunk self = (LevelChunk) (Object) this;
        ChunkStampRegistry.remove(self.getPos().toLong());
    }
}
