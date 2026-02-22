package com.ingenium.render;

import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class EntityBackfaceCulling {
    private static final ThreadLocal<Boolean> SKIP_CULLING = ThreadLocal.withInitial(() -> false);

    private EntityBackfaceCulling() {
    }

    public static void setSkipCulling(boolean skip) {
        SKIP_CULLING.set(skip);
    }

    public static boolean shouldCullPolygon(
            PoseStack.Pose pose,
            Camera camera,
            float centerX,
            float centerY,
            float centerZ,
            float normalX,
            float normalY,
            float normalZ
    ) {
        if (SKIP_CULLING.get()) return false;

        Matrix3f normalMatrix = pose.normal();
        float nx = Math.fma(normalMatrix.m00(), normalX, Math.fma(normalMatrix.m01(), normalY, normalMatrix.m02() * normalZ));
        float ny = Math.fma(normalMatrix.m10(), normalX, Math.fma(normalMatrix.m11(), normalY, normalMatrix.m12() * normalZ));
        float nz = Math.fma(normalMatrix.m20(), normalX, Math.fma(normalMatrix.m21(), normalY, normalMatrix.m22() * normalZ));

        Matrix4f positionMatrix = pose.pose();
        float cx = transformPositionX(positionMatrix, centerX, centerY, centerZ);
        float cy = transformPositionY(positionMatrix, centerX, centerY, centerZ);
        float cz = transformPositionZ(positionMatrix, centerX, centerY, centerZ);

        var camPos = camera.getPosition();
        float vx = (float) (camPos.x - cx);
        float vy = (float) (camPos.y - cy);
        float vz = (float) (camPos.z - cz);

        float dot = Math.fma(nx, vx, Math.fma(ny, vy, nz * vz));
        return dot < 0.0f;
    }

    public static float transformPositionX(Matrix4f m, float x, float y, float z) {
        return Math.fma(m.m00(), x, Math.fma(m.m10(), y, Math.fma(m.m20(), z, m.m30())));
    }

    public static float transformPositionY(Matrix4f m, float x, float y, float z) {
        return Math.fma(m.m01(), x, Math.fma(m.m11(), y, Math.fma(m.m21(), z, m.m31())));
    }

    public static float transformPositionZ(Matrix4f m, float x, float y, float z) {
        return Math.fma(m.m02(), x, Math.fma(m.m12(), y, Math.fma(m.m22(), z, m.m32())));
    }
}
