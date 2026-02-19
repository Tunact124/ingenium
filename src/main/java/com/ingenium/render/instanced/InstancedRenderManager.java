package com.ingenium.render.instanced;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.render.FrustumCache;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class InstancedRenderManager {
 
    public static final Set<BlockEntityType<?>> INSTANCED_TYPES = Set.of(
        BlockEntityType.CHEST,
        BlockEntityType.TRAPPED_CHEST,
        BlockEntityType.ENDER_CHEST,
        BlockEntityType.SIGN
    );
 
    private static final Map<BlockEntityType<?>, InstanceBuffer> BUFFERS =
        new HashMap<>();
 
    static {
        INSTANCED_TYPES.forEach(t -> BUFFERS.put(t, new InstanceBuffer(64)));
    }
 
    public static boolean isInstanced(BlockEntity be) {
        return IngeniumConfig.get().enableInstancedRendering
            && INSTANCED_TYPES.contains(be.getType());
    }
 
    /**
     * Collect one BE into its type's buffer.
     * Called from the MixinBlockEntityRenderDispatcher when isInstanced() is true.
     */
    public static void collect(BlockEntity be) {
        // ── Frustum cull ────────────────────────────────────────────
        if (IngeniumConfig.get().enableFrustumCulling) {
            BlockPos pos = be.getPos();
            if (!FrustumCache.isVisible(pos.getX(), pos.getY(), pos.getZ())) return;
        }
 
        InstanceBuffer buf = BUFFERS.get(be.getType());
        if (buf == null) return;
 
        BlockPos pos  = be.getPos();
        float    rotY = extractYRotation(be); // chest facing, item frame direction
        float    open = extractOpenness(be);   // 0 for non-animated types
        // WorldRenderer.getLightmapCoordinates already returns packed light
        float light = WorldRenderer.getLightmapCoordinates((World) be.getWorld(), pos);
 
        buf.add(pos.getX(), pos.getY(), pos.getZ(), rotY, light, open);
    }
 
    /**
     * Called once per frame in WorldRenderEvents.LAST to flush all buffers.
     */
    public static void flushAll() {
        BUFFERS.forEach((type, buf) -> {
            if (buf.count() == 0) { buf.reset(); return; }
            buf.uploadIfDirty(); // dirty-region check inside
            // InstancedShader shader = ShaderRegistry.get(type);
            // if (shader != null) shader.drawInstanced(buf.count());
            buf.reset();
        });
    }
 
    private static float extractYRotation(BlockEntity be) {
        if (be.getCachedState().contains(Properties.HORIZONTAL_FACING)) {
            return be.getCachedState().get(Properties.HORIZONTAL_FACING)
                       .asRotation();
        }
        return 0f;
    }
 
    private static float extractOpenness(BlockEntity be) {
        if (be instanceof ChestBlockEntity c) return c.getAnimationProgress(0f);
        return 0f;
    }
}
