package com.ingenium.mixin.access;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("level")
    Level ingenium_getLevel();

    @Accessor("bb")
    AABB ingenium_getBoundingBox();
}
