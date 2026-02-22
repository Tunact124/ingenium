package com.ingenium.mixin.collision;

import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Exploits VoxelShape deduplication (e.g. from FerriteCore) for fast collision checks.
 */
@Mixin(Shapes.class)
public abstract class MixinVoxelShapeComparison {

    /**
     * joinIsNotEmpty is the hot path for Entity collision detection.
     */
    @Inject(
        method = "joinIsNotEmpty",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void ingenium$fastJoinIsNotEmptyCheck(
            VoxelShape first, VoxelShape second, BooleanOp op,
            CallbackInfoReturnable<Boolean> cir) {

        if (first == second) {
            if (op == BooleanOp.NOT_SAME || op == BooleanOp.ONLY_FIRST 
                    || op == BooleanOp.ONLY_SECOND) {
                cir.setReturnValue(false);
                return;
            }
            if (op == BooleanOp.AND || op == BooleanOp.OR) {
                cir.setReturnValue(!first.isEmpty());
                return;
            }
        }
    }

    @Inject(
        method = "joinUnoptimized",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void ingenium$fastJoinUnoptimizedCheck(
            VoxelShape first, VoxelShape second, BooleanOp op,
            CallbackInfoReturnable<VoxelShape> cir) {

        if (first == second) {
            if (op == BooleanOp.AND || op == BooleanOp.OR) {
                cir.setReturnValue(first);
                return;
            }
            if (op == BooleanOp.NOT_SAME || op == BooleanOp.ONLY_FIRST 
                    || op == BooleanOp.ONLY_SECOND) {
                cir.setReturnValue(Shapes.empty());
                return;
            }
        }
    }
}
