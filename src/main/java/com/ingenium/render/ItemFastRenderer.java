package com.ingenium.render;

import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
    private static final int COLOR_OPAQUE_MASK = 0xFF000000;
    private static final int NO_TINT_INDEX = -1;

    private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64,
            (long) BUFFER_MAX_VERTICES * ModelVertex.STRIDE);

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
            ItemColors itemColors) {

        if (model == null)
            throw new IllegalStateException("Cannot render a null model");
        if (writer == null)
            throw new IllegalStateException("VertexBufferWriter is required for rendering");

        final PoseStack.Pose pose = poseStack.last();
        final ItemColor colorProvider = stack.isEmpty() ? null : itemColors::getColor;

        lastTintIndex = Integer.MIN_VALUE;
        bufferedVertices = 0;
        scratchPtr = SCRATCH_BUFFER;

        for (Direction direction : Direction.values()) {
            if ((facesMask & (1 << direction.ordinal())) == 0)
                continue;
            emitQuadList(pose, writer, model.getQuads(null, direction, null), stack, colorProvider, packedLight,
                    packedOverlay);
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
            int packedOverlay) {

        if (quads == null || quads.isEmpty())
            return;

        final Matrix4f position = pose.pose();
        final Matrix3f normalMat = pose.normal();

        for (BakedQuad quad : quads) {
            final int[] vertices = quad.getVertices();
            if (vertices.length != QUAD_VERTICES * VANILLA_STRIDE_INTS)
                continue;

            final int color = resolveColor(quad, stack, colorProvider);
            final int packedNormal = computePackedNormal(quad, normalMat);

            for (int i = 0; i < QUAD_VERTICES; i++) {
                final int base = i * VANILLA_STRIDE_INTS;

                final float x = Float.intBitsToFloat(vertices[base]);
                final float y = Float.intBitsToFloat(vertices[base + 1]);
                final float z = Float.intBitsToFloat(vertices[base + 2]);

                final float xt = MatrixHelper.transformPositionX(position, x, y, z);
                final float yt = MatrixHelper.transformPositionY(position, x, y, z);
                final float zt = MatrixHelper.transformPositionZ(position, x, y, z);

                final int bakedColor = vertices[base + 3];
                final int finalColor = color == NO_TINT_INDEX ? bakedColor : multiplyRgb8(color, bakedColor);

                final float u = Float.intBitsToFloat(vertices[base + 4]);
                final float v = Float.intBitsToFloat(vertices[base + 5]);

                final int bakedLight = vertices[base + 6];
                final int mergedLight = mergeLight(bakedLight, packedLight);

                ModelVertex.write(scratchPtr, xt, yt, zt, finalColor, u, v, packedOverlay, mergedLight, packedNormal);
                scratchPtr += ModelVertex.STRIDE;

                bufferedVertices++;
                if (bufferedVertices >= BUFFER_MAX_VERTICES) {
                    flush(writer);
                }
            }
        }
    }

    private static int resolveColor(BakedQuad quad, ItemStack stack, ItemColor colorProvider) {
        final int tintIndex = quad.getTintIndex();
        if (colorProvider == null || tintIndex < 0)
            return NO_TINT_INDEX;
        return getTintAbgr(stack, tintIndex, colorProvider);
    }

    private static int computePackedNormal(BakedQuad quad, Matrix3f normalMat) {
        final Vec3i directionNormal = quad.getDirection().getNormal();
        final float n0 = directionNormal.getX();
        final float n1 = directionNormal.getY();
        final float n2 = directionNormal.getZ();

        final float nx = MatrixHelper.transformNormalX(normalMat, n0, n1, n2);
        final float ny = MatrixHelper.transformNormalY(normalMat, n0, n1, n2);
        final float nz = MatrixHelper.transformNormalZ(normalMat, n0, n1, n2);

        return packNormal(nx, ny, nz);
    }

    private static int mergeLight(int bakedLight, int packedLight) {
        final int bB = bakedLight & 0xFFFF;
        final int bS = (bakedLight >>> 16) & 0xFFFF;
        final int pB = packedLight & 0xFFFF;
        final int pS = (packedLight >>> 16) & 0xFFFF;
        return (Math.max(bS, pS) << 16) | Math.max(bB, pB);
    }

    private static void flush(VertexBufferWriter writer) {
        if (bufferedVertices == 0)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            writer.push(stack, SCRATCH_BUFFER, bufferedVertices, ModelVertex.FORMAT);
        }

        scratchPtr = SCRATCH_BUFFER;
        bufferedVertices = 0;
    }

    private static int getTintAbgr(ItemStack stack, int tintIndex, ItemColor colorProvider) {
        if (tintIndex == lastTintIndex)
            return lastTintColor;

        final int tint = colorProvider.getColor(stack, tintIndex);
        lastTintIndex = tintIndex;
        lastTintColor = ColorARGB.toABGR(tint, 255);
        return lastTintColor;
    }

    private static int multiplyRgb8(int a, int b) {
        final int r = ((a & 0xff) * (b & 0xff) + 127) / 255;
        final int g = (((a >>> 8) & 0xff) * ((b >>> 8) & 0xff) + 127) / 255;
        final int bl = (((a >>> 16) & 0xff) * ((b >>> 16) & 0xff) + 127) / 255;
        return r | (g << 8) | (bl << 16) | (b & COLOR_OPAQUE_MASK);
    }

    private static int packNormal(float x, float y, float z) {
        final float inv = org.joml.Math.invsqrt(org.joml.Math.fma(x, x, org.joml.Math.fma(y, y, z * z)));
        final int nx = (int) (x * inv * 127.0f) & 255;
        final int ny = (int) (y * inv * 127.0f) & 255;
        final int nz = (int) (z * inv * 127.0f) & 255;
        return (nz << 16) | (ny << 8) | nx;
    }
}
