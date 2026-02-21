package com.ingenium.mixin.benchmark;

import com.ingenium.benchmark.IngeniumBenchmarkService;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerWorldChunkManagerBindMixin {

    @Shadow public abstract ServerChunkCache getChunkSource();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ingenium$bind(CallbackInfo ci) {
        ServerLevel world = (ServerLevel)(Object)this;
        IngeniumBenchmarkService.get().bindChunkManager(this.getChunkSource(), world);
    }
}
