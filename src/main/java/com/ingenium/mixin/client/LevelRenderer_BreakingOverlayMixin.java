package com.ingenium.mixin.client;

import com.ingenium.render.FaceMask;
// import com.ingenium.render.decals.DecalQuadEmitter;
// import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

@Mixin(LevelRenderer.class)
public abstract class LevelRenderer_BreakingOverlayMixin {

    @Shadow @Final private Minecraft minecraft;

    private static void ingenium$renderBreakingFast(
            BlockRenderDispatcher dispatcher,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            Vec3 camPos,
            PoseStack poseStack,
            VertexConsumer consumer
    ) {
        if (state.getRenderShape() != RenderShape.MODEL) return;

        final BakedModel model = dispatcher.getBlockModel(state);

        final int faces = FaceMask.visibleBlockFaces(camPos, pos.getX(), pos.getY(), pos.getZ(), false);

        // Sodium fast path disabled for now
        // final VertexBufferWriter writer = VertexBufferWriter.tryOf(consumer);

        final RandomSource rand = RandomSource.create();

        poseStack.pushPose();
        poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

        final PoseStack.Pose last = poseStack.last();
        final Matrix4f pose = last.pose();
        final Matrix3f normal = last.normal();

        for (Direction dir : Direction.values()) {
            if ((faces & (1 << dir.ordinal())) == 0) continue;

            for (BakedQuad quad : model.getQuads(state, dir, rand)) {
                /*
                if (writer != null) {
                    int[] lm = {0x00F000F0, 0x00F000F0, 0x00F000F0, 0x00F000F0};

                    if (DecalQuadEmitter.tryEmitBreakingQuad(writer, pose, normal, quad, lm)) {
                        continue;
                    }
                }
                */

                Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), consumer, state, model, 1f, 1f, 1f, 0x00F000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                break;
            }
        }

        poseStack.popPose();
    }
}
