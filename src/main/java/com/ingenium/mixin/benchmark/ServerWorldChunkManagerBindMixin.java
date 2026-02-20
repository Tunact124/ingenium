package com.ingenium.mixin.benchmark;

import com.ingenium.benchmark.IngeniumBenchmarkService;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldChunkManagerBindMixin {

    @Shadow public abstract ServerChunkManager getChunkManager();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ingenium$bind(CallbackInfo ci) {
        ServerWorld world = (ServerWorld)(Object)this;
        IngeniumBenchmarkService.get().bindChunkManager(this.getChunkManager(), world);
    }
}
