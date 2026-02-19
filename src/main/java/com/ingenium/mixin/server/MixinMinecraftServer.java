package com.ingenium.mixin.server;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.governor.IngeniumGovernor;
import com.ingenium.governor.OptimizationProfile;
import com.ingenium.threading.IngeniumExecutors;
import com.ingenium.util.IngeniumLogger;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {
 
    @Inject(
        method = "save(ZZZ)Z",
        at = @At("HEAD")
    )
    private void onSave(boolean suppressLogs, boolean flush, boolean force,
                        CallbackInfoReturnable<Boolean> cir) {
        // Safe placeholder: avoid async saving as it triggers LegacyRandomSource thread checks
    }
}
