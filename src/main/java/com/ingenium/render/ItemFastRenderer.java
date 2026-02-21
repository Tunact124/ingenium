package com.ingenium.render;

import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.List;

public final class ItemFastRenderer {
    private static final int QUAD_VERTICES = 4;
    private static final int VANILLA_STRIDE_INTS = 8;

    private static final int BUFFER_MAX_VERTICES = 48;
    private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64, (long) BUFFER_MAX_VERTICES * ModelVertex.STRIDE);

    private static long scratchPtr = SCRATCH_BUFFER;
    private static int bufferedVertices;

    private static int lastTintIndex = Integer.MIN_VALUE;
    private static int lastTintColor = 0;

    private ItemFastRenderer() {
    }

    public static void render(
            SimpleBakedModel model,
            int facesMask,
            ItemStack stack,
            int packedLight,
            int packedOverlay,
            PoseStack poseStack,
            VertexBufferWriter writer,
            ItemColors itemColors
    ) {
        var pose = poseStack.last();
        ItemColor colorProvider = stack.isEmpty() ? null : itemColors.getColor(stack, 0) == 0 ? null : itemColors::getColor;

        lastTintIndex = Integer.MIN_VALUE;
        bufferedVertices = 0;
        scratchPtr = SCRATCH_BUFFER;

        for (Direction direction : Direction.values()) {
            if ((facesMask & (1 << direction.ordinal())) == 0) continue;
            emitQuadList(pose, writer, model.getQuads(null, direction, null), stack, colorProvider, packedLight, packedOverlay);
        }

        emitQuadList(pose, writer, model.getQuads(null, null, null), stack, colorProvider, packedLight, packedOverlay);
        flush(writer);
    }

    private static void emitQuadList(
            PoseStack.Pose pose,
            VertexBufferWriter writer,
            List<BakedQuad> quads,
            ItemStack stack,
            ItemColor colorProvider,
            int packedLight,
            int packedOverlay
    ) {
        if (quads == null || quads.isEmpty()) return;

        Matrix4f position = pose.pose();
        Matrix3f normalMat = pose.normal();

        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            if (vertices.length != QUAD_VERTICES * VANILLA_STRIDE_INTS) continue;

            int color = -1;
            int tintIndex = quad.getTintIndex();
            if (colorProvider != null && tintIndex >= 0) {
                color = getTintAbgr(stack, tintIndex, colorProvider);
            }

            int bakedNormal = vertices[7];
            float n0 = NormI8.unpackX(bakedNormal);
            float n1 = NormI8.unpackY(bakedNormal);
            float n2 = NormI8.unpackZ(bakedNormal);

            float nx = MatrixHelper.transformNormalX(normalMat, n0, n1, n2);
            float ny = MatrixHelper.transformNormalY(normalMat, n0, n1, n2);
            float nz = MatrixHelper.transformNormalZ(normalMat, n0, n1, n2);

            int packedNormal = packNormal(nx, ny, nz);

            for (int i = 0; i < 4; i++) {
                int base = i * VANILLA_STRIDE_INTS;

                float x = Float.intBitsToFloat(vertices[base]);
                float y = Float.intBitsToFloat(vertices[base + 1]);
                float z = Float.intBitsToFloat(vertices[base + 2]);

                float xt = MatrixHelper.transformPositionX(position, x, y, z);
                float yt = MatrixHelper.transformPositionY(position, x, y, z);
                float zt = MatrixHelper.transformPositionZ(position, x, y, z);

                int bakedColor = vertices[base + 3];
                int finalColor = color == -1 ? bakedColor : multiplyRgb8(color, bakedColor);

                float u = Float.intBitsToFloat(vertices[base + 4]);
                float v = Float.intBitsToFloat(vertices[base + 5]);

                int bakedLight = vertices[base + 6];
                int mergedLight = Math.max(((bakedLight & 0xffff) << 16) | (bakedLight >>> 16), packedLight);

                ModelVertex.write(scratchPtr, xt, yt, zt, finalColor, u, v, packedOverlay, mergedLight, packedNormal);
                scratchPtr += ModelVertex.STRIDE;

                bufferedVertices++;
                if (bufferedVertices >= BUFFER_MAX_VERTICES) {
                    flush(writer);
                }
            }
        }
    }

    private static void flush(VertexBufferWriter writer) {
        if (bufferedVertices == 0) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            writer.push(stack, SCRATCH_BUFFER, bufferedVertices, ModelVertex.FORMAT);
        }

        scratchPtr = SCRATCH_BUFFER;
        bufferedVertices = 0;
    }

    private static int getTintAbgr(ItemStack stack, int tintIndex, ItemColor colorProvider) {
        if (tintIndex == lastTintIndex) return lastTintColor;

        int tint = colorProvider.getColor(stack, tintIndex);
        lastTintIndex = tintIndex;
        lastTintColor = ColorARGB.toABGR(tint, 255);
        return lastTintColor;
    }

    private static int multiplyRgb8(int a, int b) {
        int r = ((a & 0xff) * (b & 0xff) + 127) / 255;
        int g = (((a >>> 8) & 0xff) * ((b >>> 8) & 0xff) + 127) / 255;
        int bl = (((a >>> 16) & 0xff) * ((b >>> 16) & 0xff) + 127) / 255;
        return r | (g << 8) | (bl << 16) | (b & 0xff000000);
    }

    private static int packNormal(float x, float y, float z) {
        float inv = org.joml.Math.invsqrt(org.joml.Math.fma(x, x, org.joml.Math.fma(y, y, z * z)));
        int nx = (int) (x * inv * 127.0f) & 255;
        int ny = (int) (y * inv * 127.0f) & 255;
        int nz = (int) (z * inv * 127.0f) & 255;
        return (nz << 16) | (ny << 8) | nx;
    }
}
