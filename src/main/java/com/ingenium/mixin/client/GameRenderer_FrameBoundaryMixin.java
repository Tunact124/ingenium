package com.ingenium.mixin.client;

import com.ingenium.core.Ingenium;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRenderer_FrameBoundaryMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void ingenium$beginFrame(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        // Resets per-frame budgets so allow(...) calls don’t “carry over” across frames.
        Ingenium.runtime().governor().beginClientFrame();
    }
}
