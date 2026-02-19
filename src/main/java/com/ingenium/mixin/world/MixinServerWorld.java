package com.ingenium.mixin.world;

import com.ingenium.threading.ChunkStampRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class MixinServerWorld {
 
    // Intercept setBlockState at TAIL (after vanilla logic completes)
    // so the stamp is valid for any subsequent async tasks.
    @Inject(
        method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z",
        at = @At("TAIL")
    )
    private void onBlockChanged(BlockPos pos, BlockState state, int flags,
                                int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        ChunkStampRegistry.invalidate(new ChunkPos(pos));
    }
}
