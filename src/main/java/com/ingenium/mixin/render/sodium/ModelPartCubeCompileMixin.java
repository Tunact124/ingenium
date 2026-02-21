package com.ingenium.mixin.render.sodium;

import com.ingenium.core.Ingenium;
import com.ingenium.render.EntityBackfaceCulling;
import com.ingenium.mixin.render.ModelPartCubeAccessor;
import com.ingenium.mixin.render.ModelPartPolygonAccessor;
import com.ingenium.mixin.render.ModelPartVertexAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.model.geom.ModelPart$Cube")
public abstract class ModelPartCubeCompileMixin {

    // Hook: polygon compilation is where ModelPart emits vertices.
    // Culling here avoids per-vertex work for back-facing polygons.
    @Inject(
            method = "compile(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ingenium_compileCulled(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha,
            CallbackInfo ci
    ) {
        var runtime = Ingenium.runtime();
        if (runtime == null) return;

        var governor = runtime.governor();
        if (governor == null || !governor.allow(com.ingenium.core.IngeniumGovernor.SubsystemType.ENTITY_BACKFACE_CULLING)) return;

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null) return;

        ModelPart.Polygon[] polygons = ((ModelPartCubeAccessor) this).ingenium_polygons();
        if (polygons == null || polygons.length == 0) return;

        ci.cancel();

        for (ModelPart.Polygon polygon : polygons) {
            var polyAccessor = (ModelPartPolygonAccessor) polygon;
            var normal = polyAccessor.ingenium_normal();
            var vertices = polyAccessor.ingenium_vertices();
            if (vertices == null || vertices.length != 4) continue;

            float centerX = 0.0f;
            float centerY = 0.0f;
            float centerZ = 0.0f;

            for (int i = 0; i < 4; i++) {
                var v = (ModelPartVertexAccessor) vertices[i];
                var pos = v.ingenium_pos();
                centerX += pos.x();
                centerY += pos.y();
                centerZ += pos.z();
            }

            centerX *= 0.25f;
            centerY *= 0.25f;
            centerZ *= 0.25f;

            if (EntityBackfaceCulling.shouldCullPolygon(
                    pose,
                    camera,
                    centerX, centerY, centerZ,
                    normal.x(), normal.y(), normal.z()
            )) {
                continue;
            }

            for (int i = 0; i < 4; i++) {
                var v = (ModelPartVertexAccessor) vertices[i];
                var pos = v.ingenium_pos();

                consumer.vertex(pose.pose(), pos.x(), pos.y(), pos.z())
                        .color(red, green, blue, alpha)
                        .uv(v.ingenium_u(), v.ingenium_v())
                        .overlayCoords(packedOverlay)
                        .uv2(packedLight)
                        .normal(pose.normal(), normal.x(), normal.y(), normal.z())
                        .endVertex();
            }
        }
    }
}
