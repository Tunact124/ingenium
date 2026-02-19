package com.ingenium.mixin.client;

import com.ingenium.render.instanced.InstancedRenderManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class MixinBlockEntityRenderDispatcher {
 
    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true
    )
    private void ingenium$onBeRender(
            BlockEntity be, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider consumers,
            CallbackInfo ci) {
 
        if (InstancedRenderManager.isInstanced(be)) {
            InstancedRenderManager.collect(be);
            ci.cancel(); // vanilla per-instance draw suppressed
        }
    }
}
