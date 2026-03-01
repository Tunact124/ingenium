package com.ingenium.mixin;

import com.ingenium.compat.BuddyLogic;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class IngeniumMixinPlugin implements IMixinConfigPlugin {

    private static final java.util.Map<String, String> CONFLICT_MAP = java.util.Map.of(
            // EfficientHashing @Overwrites Vec3i.hashCode() — same target as ours
            "com.ingenium.mixin.core.Vec3iHashMixin", "efficient_hashing",

            // Lithium replaces WorldTickScheduler — our timing wheel conflicts
            "com.ingenium.mixin.ScheduledTickWheelMixin", "lithium",

            // Clumps XP orb conflict
            "com.ingenium.mixin.entity.ExperienceOrbMixin", "clumps");

    @Override
    public void onLoad(String mixinPackage) {
        BuddyLogic.earlyInit();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".render.sodium.")) {
            if (BuddyLogic.isModLoaded("immediatelyfast") && mixinClassName.contains("ItemRendererMixin")) {
                BuddyLogic.logYield(mixinClassName, "immediatelyfast", "Yielding item rendering");
                return false;
            }
            return BuddyLogic.isModLoaded("sodium");
        }

        String conflictingMod = CONFLICT_MAP.get(mixinClassName);
        if (conflictingMod != null && BuddyLogic.isModLoaded(conflictingMod)) {
            BuddyLogic.logYield(
                    mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1),
                    conflictingMod,
                    "Mixin excluded to avoid conflict");
            return false;
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
