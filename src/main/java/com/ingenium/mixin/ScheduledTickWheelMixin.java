package com.ingenium.mixin;

import com.ingenium.benchmark.IngeniumDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.1 hook points for scheduled block/fluid ticks.
 *
 * Do not target TickPriority overloads here; those signatures are not present in 1.20.1 Yarn.
 */
@Mixin(ServerWorld.class)
public abstract class ScheduledTickWheelMixin {

    @Inject(
            method = "scheduleBlockTick(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;I)V",
            at = @At("HEAD"),
            require = 0
    )
    private void ingenium$onScheduleBlockTick(BlockPos pos, Block block, int delay, CallbackInfo ci) {
        IngeniumDiagnostics.onScheduledBlockTick((ServerWorld) (Object) this, pos, delay);
    }

    @Inject(
            method = "scheduleFluidTick(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/fluid/Fluid;I)V",
            at = @At("HEAD"),
            require = 0
    )
    private void ingenium$onScheduleFluidTick(BlockPos pos, Fluid fluid, int delay, CallbackInfo ci) {
        IngeniumDiagnostics.onScheduledFluidTick((ServerWorld) (Object) this, pos, delay);
    }
}
