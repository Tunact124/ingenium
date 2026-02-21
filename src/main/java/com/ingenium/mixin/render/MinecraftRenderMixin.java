package com.ingenium.mixin.render;

import com.ingenium.core.Ingenium;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftRenderMixin {

    // Injection point: begin-of-frame reset for per-frame token budgets
    @Inject(method = "runTick(Z)V", at = @At("HEAD"))
    private void ingenium_beginFrame(boolean tick, CallbackInfo ci) {
        var runtime = Ingenium.runtime();
        if (runtime == null) return;

        runtime.governor().beginClientFrame();
    }
}
