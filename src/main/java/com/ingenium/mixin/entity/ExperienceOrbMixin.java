package com.ingenium.mixin.entity;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.Ingenium;
import com.ingenium.core.IngeniumGovernor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    private static final double MERGE_RADIUS = 0.5D;
    private static final int MAX_XP_PER_ORB = 32_000;

    @Shadow public abstract Level level();
    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract boolean isRemoved();
    @Shadow public abstract void discard();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ingenium_onTick(CallbackInfo ci) {
        if (!IngeniumConfig.getInstance().xpOrbCoalescingEnabled) {
            return;
        }

        var runtime = Ingenium.runtime();
        var governor = runtime.governor();
        if (!governor.allow(IngeniumGovernor.SubsystemType.XP_ORBS)) {
            return;
        }

        long before = System.nanoTime();
        ingenium_mergeNearbyOrbsCountAware();
        long after = System.nanoTime();
        governor.recordSubsystemTime(IngeniumGovernor.SubsystemType.XP_ORBS, after - before);
    }

    private void ingenium_mergeNearbyOrbsCountAware() {
        if (isRemoved()) return;

        var level = level();
        if (level.isClientSide) return;

        int selfValue = ((ExperienceOrbAccessor) this).ingenium_getValue();
        if (selfValue <= 0) return;
        if (selfValue >= MAX_XP_PER_ORB) return;

        var box = getBoundingBox().inflate(MERGE_RADIUS);
        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, box, orb -> orb != null && !orb.isRemoved());

        if (orbs.size() <= 1) return;

        for (var other : orbs) {
            if ((Object) other == this) continue;
            if (other.isRemoved()) continue;

            int otherValue = ((ExperienceOrbAccessor) other).ingenium_getValue();
            if (otherValue <= 0) continue;

            int capacity = MAX_XP_PER_ORB - selfValue;
            if (capacity <= 0) return;

            int taken = Math.min(capacity, otherValue);
            if (taken <= 0) continue;

            selfValue += taken;
            otherValue -= taken;

            ((ExperienceOrbAccessor) this).ingenium_setValue(selfValue);

            if (otherValue <= 0) {
                other.discard();
            } else {
                ((ExperienceOrbAccessor) other).ingenium_setValue(otherValue);
            }

            if (selfValue >= MAX_XP_PER_ORB) return;
        }
    }
}
