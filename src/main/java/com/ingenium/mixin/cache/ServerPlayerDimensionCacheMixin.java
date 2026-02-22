package com.ingenium.mixin.cache;

import com.ingenium.cache.DimensionCacheRegistry;
import com.ingenium.cache.DimensionCacheRegistry.DimensionCacheSnapshot;
import com.ingenium.core.Ingenium;
import com.ingenium.core.IngeniumSafetySystem;
import com.ingenium.offheap.OffHeapBlockEntityStore;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.Entity;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDimensionCacheMixin {

    /**
     * Park caches before changing dimension.
     */
    @Inject(
        method = "teleportTo",
        at = @At("HEAD")
    )
    private void ingenium_parkCachesBeforeDimensionChange(ServerLevel destination, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        IngeniumSafetySystem.guard("dimension_cache_park", () -> {
            ServerPlayer self = (ServerPlayer)(Object) this;
            ServerLevel currentLevel = (ServerLevel) self.level();
            ResourceKey<Level> currentDim = currentLevel.dimension();
            
            DimensionCacheRegistry registry = Ingenium.runtime().dimensionCacheRegistry();
            if (registry != null) {
                registry.parkDimension(
                    currentDim,
                    Ingenium.runtime().governor(),
                    OffHeapBlockEntityStore.get()
                );
            }
        });
    }

    /**
     * Restore caches after changing dimension.
     */
    @Inject(
        method = "teleportTo",
        at = @At("RETURN")
    )
    private void ingenium_restoreCachesAfterDimensionChange(ServerLevel destination, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        IngeniumSafetySystem.guard("dimension_cache_restore", () -> {
            ResourceKey<Level> targetDim = destination.dimension();
            
            DimensionCacheRegistry registry = Ingenium.runtime().dimensionCacheRegistry();
            if (registry == null) return;
            
            DimensionCacheSnapshot snapshot = registry.restoreDimension(targetDim);
            if (snapshot != null) {
                // Restore Governor profile
                Ingenium.runtime().governor().restoreFromSnapshot(
                    snapshot.governorProfile(),
                    snapshot.recentMsptAverage(),
                    snapshot.profileStabilityTicks()
                );
                
                // Restore BE metadata
                if (snapshot.beSnapshot() != null) {
                    OffHeapBlockEntityStore.get().restoreFromSnapshot(snapshot.beSnapshot());
                }
            }
        });
    }
}
