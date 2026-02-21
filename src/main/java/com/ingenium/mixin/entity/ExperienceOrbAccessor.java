package com.ingenium.mixin.entity;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExperienceOrb.class)
public interface ExperienceOrbAccessor {
    @Accessor("value")
    int ingenium_getValue();

    @Accessor("value")
    void ingenium_setValue(int value);
}
