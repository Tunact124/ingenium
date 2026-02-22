package com.ingenium.mixin.render;

import com.ingenium.render.EntityBackfaceCulling;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Inject(method = "render", at = @At("HEAD"))
    private void ingenium$beforeRender(T entity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (entity instanceof ItemEntity || entity instanceof ItemFrame) {
            EntityBackfaceCulling.setSkipCulling(true);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ingenium$afterRender(T entity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        EntityBackfaceCulling.setSkipCulling(false);
    }
}
