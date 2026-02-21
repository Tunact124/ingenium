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
        ExperienceOrb self = (ExperienceOrb) (Object) this;
        if (self.isRemoved()) return;

        Level level = self.level();
        if (level.isClientSide()) return;

        ExperienceOrbAccessor selfAcc = (ExperienceOrbAccessor) self;
        int selfValue = selfAcc.ingenium_getValue();
        if (selfValue <= 0) return;
        if (selfValue >= MAX_XP_PER_ORB) return;

        var box = self.getBoundingBox().inflate(MERGE_RADIUS);
        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, box, orb -> orb != null && !orb.isRemoved());

        if (orbs.size() <= 1) return;

        for (ExperienceOrb other : orbs) {
            if (other == self) continue;
            if (other.isRemoved()) continue;

            ExperienceOrbAccessor otherAcc = (ExperienceOrbAccessor) other;
            int otherValue = otherAcc.ingenium_getValue();
            if (otherValue <= 0) continue;

            // Simple merging: only merge if they have the same value (like vanilla but more aggressive scan)
            if (otherValue == selfValue) {
                selfAcc.ingenium_setCount(selfAcc.ingenium_getCount() + otherAcc.ingenium_getCount());
                other.discard();
            } else if (IngeniumGovernor.get().profile() == IngeniumGovernor.OptimizationProfile.EMERGENCY) {
                // In EMERGENCY, we might want to merge different values into a single large value
                // but that requires more complex math to preserve total XP.
                // For now, let's just stick to same-value merging.
            }

            if (self.isRemoved()) return;
        }
    }
}
