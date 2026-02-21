package com.ingenium.mixin.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngine_FastFrustumMixin {

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z")
    )
    private boolean ingenium$fastParticleFrustum(Frustum frustum, AABB aabb) {
        // Safety: never cull weird/infinite boxes
        if (aabb == null) return true;
        if (!Double.isFinite(aabb.minX) || !Double.isFinite(aabb.maxX)) return true;

        // Sphere approx: center + radius
        final double cx = (aabb.minX + aabb.maxX) * 0.5;
        final double cy = (aabb.minY + aabb.maxY) * 0.5;
        final double cz = (aabb.minZ + aabb.maxZ) * 0.5;

        final double dx = (aabb.maxX - aabb.minX) * 0.5;
        final double dy = (aabb.maxY - aabb.minY) * 0.5;
        final double dz = (aabb.maxZ - aabb.minZ) * 0.5;

        final float r = (float) Math.max(dx, Math.max(dy, dz));

        // Mojang Frustum has `intersection` internally; if you’re using accessors, wire them.
        // If not, you can keep this redirect to the original method. For now, safest fallback:
        try {
            // Most MC frustum impls contain intersection testSphere; but not public.
            // If you already added an accessor mixin for `intersection`, use it here.
            return frustum.isVisible(aabb); // fallback-safe
        } catch (Throwable t) {
            return true;
        }
    }
}
