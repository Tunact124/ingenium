package com.ingenium.render;
 
import com.ingenium.mixin.client.FrustumAccessor;
import net.minecraft.client.render.Frustum;
 
public final class FrustumCache {
 
    // Updated once per frame in a ClientTickEvents or WorldRenderEvents hook.
    private static Frustum currentFrustum = null;
 
    public static void update(Frustum f) { currentFrustum = f; }
 
    /**
     * Returns true if a 1x1x1 block at (x,y,z) is inside the view frustum.
     * If frustum is unavailable (e.g., during GUI rendering), defaults to true.
     */
    public static boolean isVisible(double x, double y, double z) {
        if (currentFrustum == null) return true;
        return ((FrustumAccessor) currentFrustum).callIsVisible(x, y, z, x + 1.0, y + 1.0, z + 1.0);
    }
}
