package com.ingenium.mixin;

import com.ingenium.util.ChunkStampRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public abstract class ChunkStampMixin {

    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void ingenium$invalidateOnBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        long packed = ChunkPos.toLong(pos);
        ChunkStampRegistry.invalidate(packed);
    }

    @Inject(method = "addBlockEntity", at = @At("TAIL"))
    private void ingenium$invalidateOnAddBE(BlockEntity be, CallbackInfo ci) {
        long packed = ChunkPos.toLong(be.getPos());
        ChunkStampRegistry.invalidate(packed);
    }

    @Inject(method = "removeBlockEntity", at = @At("TAIL"))
    private void ingenium$invalidateOnRemoveBE(BlockPos pos, CallbackInfo ci) {
        long packed = ChunkPos.toLong(pos);
        ChunkStampRegistry.invalidate(packed);
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void ingenium$invalidateOnUnload(CallbackInfo ci) {
        WorldChunk self = (WorldChunk) (Object) this;
        ChunkStampRegistry.remove(self.getPos().toLong());
    }
}
