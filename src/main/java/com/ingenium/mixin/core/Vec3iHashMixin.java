package com.ingenium.mixin.core;

import com.ingenium.hash.PhiMixHash;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Optimizes hashCode distribution for all coordinate-based objects (BlockPos, etc).
 */
@Mixin(Vec3i.class)
public abstract class Vec3iHashMixin {
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int z;

    /**
     * @author Ingenium (absorbed from EfficientHashing by ZZZank)
     * @reason Phi-mix hashing reduces collision rate by >90% vs vanilla
     */
    @Overwrite
    public int hashCode() {
        return PhiMixHash.hashCoordinates(this.x, this.y, this.z);
    }
}
