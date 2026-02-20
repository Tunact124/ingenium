package com.ingenium.mixin;

import com.ingenium.core.IngeniumGovernor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Wraps block entity ticking with governor checks.
 */
@Mixin(ServerWorld.class)
public abstract class BlockEntityTickThrottleMixin {

    @WrapOperation(
            method = "tickBlockEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/BlockEntityTickInvoker;tick()V"
            ),
            require = 0
    )
    private void ingenium$wrapBlockEntityTick(BlockEntityTickInvoker invoker, Operation<Void> original) {
        if (IngeniumGovernor.get().allowBlockEntityTick(invoker)) {
            original.call(invoker);
        }
    }
}
