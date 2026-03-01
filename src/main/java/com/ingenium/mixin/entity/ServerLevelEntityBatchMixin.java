package com.ingenium.mixin.entity;

import com.ingenium.core.Ingenium;
import com.ingenium.core.IngeniumSafetySystem;
import com.ingenium.entity.EntitySpatialBatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Intercepts entity ticking to apply spatial batching.
 * Spatially coherent ticking improves CPU cache locality for collision and
 * interaction checks.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelEntityBatchMixin {

    /**
     * Redirect the forEach call on the entity tick list to apply spatial batching.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
    private void ingenium_batchEntityTicks(EntityTickList instance, Consumer<Entity> action) {
        IngeniumSafetySystem.guard("entity_spatial_batching", () -> {
            // Rather than trying to gather ALL entities into a mega-batch (which is O(N)
            // array allocation and traversal), we just let Vanilla stream them, but we
            // ask our spatial batcher to process them efficiently without heavy object
            // creation.
            EntitySpatialBatcher batcher = Ingenium.runtime().entitySpatialBatcher();
            batcher.processSpatially(instance, action);
        });
    }
}
