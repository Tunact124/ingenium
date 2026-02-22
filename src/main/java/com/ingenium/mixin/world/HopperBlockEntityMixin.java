package com.ingenium.mixin.world;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.Ingenium;
import com.ingenium.core.IngeniumGovernor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HopperBlockEntity.class)
public final class HopperBlockEntityMixin {

    @Inject(method = "pushItemsTick", at = @At("HEAD"), cancellable = true)
    private static void ingenium$onPushItemsTick(
        Level pLevel,
        BlockPos pPos,
        BlockState pState,
        HopperBlockEntity pBlockEntity,
        CallbackInfo ci
    ) {
        if (!IngeniumConfig.getInstance().hopperOptimizationEnabled) {
            return;
        }

        var runtime = Ingenium.runtime();
        if (runtime == null) return;

        var governor = runtime.governor();
        if (governor == null || !governor.allow(IngeniumGovernor.SubsystemType.HOPPER)) {
            return;
        }

        if (!pLevel.isClientSide) {
            var throttler = runtime.hopperThrottler();
            var gameTime = pLevel.getGameTime();
            var packedPos = pPos.asLong();

            if (throttler.isCoolingDown(packedPos, gameTime)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "pushItemsTick", at = @At("RETURN"))
    private static void ingenium$afterPushItemsTick(
        Level pLevel,
        BlockPos pPos,
        BlockState pState,
        HopperBlockEntity pBlockEntity,
        CallbackInfo ci
    ) {
        if (!pLevel.isClientSide) {
            var runtime = Ingenium.runtime();
            if (runtime == null) return;

            var throttler = runtime.hopperThrottler();
            if (throttler.shouldApplyCooldown(pPos.asLong(), pLevel.getGameTime())) {
                throttler.cooldown(pPos.asLong(), pLevel.getGameTime());
            }
        }
    }
}
