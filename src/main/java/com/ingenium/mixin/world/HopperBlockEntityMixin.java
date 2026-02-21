package com.ingenium.mixin.world;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.Ingenium;
import com.ingenium.core.IngeniumGovernor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HopperBlockEntity.class)
public final class HopperBlockEntityMixin {
    private static final int COOLDOWN_TICKS_WHEN_FULL = 8;

    @Inject(method = "pushItemsTick", at = @At("HEAD"), cancellable = true)
    private static void ingenium_onPushItemsTick(
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
        var governor = runtime.governor();
        if (!governor.allow(IngeniumGovernor.SubsystemType.HOPPER)) {
            return;
        }

        if (!pLevel.isClientSide) {
            var throttler = runtime.hopperThrottler();
            var gameTime = pLevel.getGameTime();
            var packedPos = pPos.asLong();

            if (throttler.isCoolingDown(packedPos, gameTime)) {
                ci.cancel();
                return;
            }
        }
    }
}
