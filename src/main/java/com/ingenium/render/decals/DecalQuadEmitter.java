package com.ingenium.render.decals;

import com.ingenium.core.Ingenium;
import com.ingenium.core.IngeniumGovernor;
import com.ingenium.render.ScratchBuffers;
import com.ingenium.render.culling.FaceMask;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.LightAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.NormalAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public final class DecalQuadEmitter {
    private DecalQuadEmitter() {}

    private static final VertexFormatDescription BLOCK_FORMAT =
            VertexFormatRegistry.instance().get(DefaultVertexFormat.BLOCK);

    private static final int STRIDE = 32;

    public static int visibleFacesMask(Vec3 viewDir) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            double dot = d.getStepX() * viewDir.x + d.getStepY() * viewDir.y + d.getStepZ() * viewDir.z;
            if (dot < 0.0) mask |= FaceMask.of(d);
        }
        return mask == 0 ? FaceMask.ALL : mask;
    }

    public static boolean tryEmitBreakingQuad(
            VertexBufferWriter writer,
            Matrix4f pose,
            Matrix3f normalMat,
            BakedQuad quad,
            int[] lightmap
    ) {
        final IngeniumGovernor gov = Ingenium.runtime().governor();
        if (!gov.allow(IngeniumGovernor.SubsystemType.CORE_COMMIT_QUEUE)) {
            return false;
        }

        final int bytes = 4 * STRIDE;
        final ByteBuffer buf = ScratchBuffers.get(bytes);
        long base = org.lwjgl.system.MemoryUtil.memAddress(buf);

        final int[] v = quad.getVertices();
        if (v == null || v.length < 32) return false;

        long ptr = base;

        for (int i = 0; i < 4; i++) {
            final int vi = i * 8;

            final float x = Float.intBitsToFloat(v[vi]);
            final float y = Float.intBitsToFloat(v[vi + 1]);
            final float z = Float.intBitsToFloat(v[vi + 2]);

            final float xt = MatrixHelper.transformPositionX(pose, x, y, z);
            final float yt = MatrixHelper.transformPositionY(pose, x, y, z);
            final float zt = MatrixHelper.transformPositionZ(pose, x, y, z);

            final int color = 0xFFFFFFFF;

            final int rawNormal = v[vi + 7];
            final float nx = NormI8.unpackX(rawNormal);
            final float ny = NormI8.unpackY(rawNormal);
            final float nz = NormI8.unpackZ(rawNormal);

            final float u = planarU(nx, ny, nz, x, y, z);
            final float vv = planarV(nx, ny, nz, x, y, z);

            final int bakedLight = v[vi + 6];
            final int lm = lightmap[i];
            final int bakedSwapped = ((bakedLight & 0xFFFF) << 16) | (bakedLight >>> 16);
            final int packedLight = Math.max(bakedSwapped, lm);

            final int normalPacked = MatrixHelper.transformNormal(normalMat, rawNormal);

            PositionAttribute.put(ptr + 0L, xt, yt, zt);
            ColorAttribute.set(ptr + 12L, color);
            TextureAttribute.put(ptr + 16L, u, vv);
            LightAttribute.set(ptr + 24L, packedLight);
            NormalAttribute.set(ptr + 28L, normalPacked);

            ptr += STRIDE;
        }

        try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
            writer.push(stack, base, 4, BLOCK_FORMAT);
        } catch (Throwable t) {
            return false;
        }

        return true;
    }

    private static float planarU(float nx, float ny, float nz, float x, float y, float z) {
        final Direction d = Direction.getNearest(nx, ny, nz);
        return switch (d) {
            case DOWN, UP -> x;
            case NORTH -> -x;
            case SOUTH -> x;
            case WEST -> z;
            case EAST -> -z;
        };
    }

    private static float planarV(float nx, float ny, float nz, float x, float y, float z) {
        final Direction d = Direction.getNearest(nx, ny, nz);
        return switch (d) {
            case DOWN -> -z;
            case UP -> z;
            case NORTH, SOUTH, WEST, EAST -> -y;
        };
    }

    public static RenderType breakingLayer(net.minecraft.resources.ResourceLocation location) {
        return RenderType.crumbling(location);
    }
}
