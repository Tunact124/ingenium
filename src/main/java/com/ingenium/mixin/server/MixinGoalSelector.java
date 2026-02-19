package com.ingenium.mixin.server;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.governor.IngeniumGovernor;
import com.ingenium.IngeniumMod;
import net.minecraft.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GoalSelector.class)
public abstract class MixinGoalSelector {
 
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ingenium$onTick(CallbackInfo ci) {
        if (!com.ingenium.config.IngeniumConfig.get().enableGovernor) return;
        
        com.ingenium.governor.OptimizationProfile p = com.ingenium.IngeniumMod.GOVERNOR.current();
        if (p == com.ingenium.governor.OptimizationProfile.BALANCED) return;
        
        // Adaptive ticking frequency logic
        int rate = switch (p) {
            case EMERGENCY  -> 4; // 1/4 speed
            case REACTIVE   -> 2; // 1/2 speed
            case AGGRESSIVE -> 1; // Full speed (idle case)
            default         -> 1;
        };
        
        // Use a simple tick skip. We don't have access to the entity here easily,
        // so we'll use the world time.
        // GoalSelector doesn't have a direct reference to the entity it belongs to in the Yarn mappings usually,
        // but it's often stored in a field or we can get it from context.
        // In 1.20.1 GoalSelector is basically a set of goals.
        // Ticking the goal selector is ticking the entity AI goals.
        
        // For simplicity and effectiveness in the stress test, we use world time.
        // This will skip goal ticking for ALL entities on the same tick, which is very effective.
        if (rate > 1 && (System.currentTimeMillis() / 50) % rate != 0) {
            ci.cancel();
        }
    }
}
