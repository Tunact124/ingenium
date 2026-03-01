package com.ingenium.mixin.render.sodium;

import com.ingenium.core.Ingenium;
import com.ingenium.core.IngeniumSafetySystem;
import com.ingenium.render.ItemFacesMask;
import com.ingenium.render.ItemFastRenderer;
import com.ingenium.render.PlaceholderModels;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ItemRenderer.class, priority = 100)
public abstract class ItemRendererMixin {
        @Shadow
        @Final
        private ItemColors itemColors;

        // By modifying the BakedModel argument of renderModelLists to act as a dummy,
        // we prevent Vanilla from
        // rendering the model while injecting our custom high-performance
        // ItemFastRenderer instead.
        @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"))
        private BakedModel ingenium_onRenderModelLists(
                        BakedModel model,
                        ItemStack stack,
                        int light,
                        int overlay,
                        PoseStack poseStack,
                        VertexConsumer vertexConsumer,
                        @Local(ordinal = 0) BakedModel originalModel,
                        @Local ItemDisplayContext itemDisplayContext) {
                final var runtime = Ingenium.runtime();
                if (runtime == null)
                        return model;

                final var governor = runtime.governor();
                if (governor == null
                                || !governor.allow(com.ingenium.core.IngeniumGovernor.SubsystemType.ITEM_FAST_PATH))
                        return model;

                if (!(originalModel instanceof SimpleBakedModel originalSimple))
                        return model;
                if (!(model instanceof SimpleBakedModel simpleModel))
                        return model;

                final var writer = VertexBufferWriter.tryOf(vertexConsumer);
                if (writer == null)
                        return model;

                try {
                        final int facesMask = ItemFacesMask.decide(originalSimple.getTransforms(), itemDisplayContext,
                                        poseStack.last());

                        ItemFastRenderer.render(
                                        simpleModel,
                                        facesMask,
                                        stack,
                                        light,
                                        overlay,
                                        poseStack,
                                        writer,
                                        this.itemColors);

                        return PlaceholderModels.DUMMY;
                } catch (Throwable t) {
                        IngeniumSafetySystem.reportFailure("ItemFastRenderer", t);
                        return model;
                }
        }
}
