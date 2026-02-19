package com.ingenium.mixin;

import com.ingenium.compat.CompatibilityBridge;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class IngeniumMixinPlugin implements IMixinConfigPlugin {
 
    // ── Mixins to suppress when Lithium is present ──────────────────
    // Lithium has superior implementations of all of these.
    private static final Set<String> LITHIUM_CONFLICTS = Set.of(
        "com.ingenium.mixin.server.MixinEntityNavigation",
        "com.ingenium.mixin.server.MixinGoalSelector",
        "com.ingenium.mixin.server.MixinBrain",
        "com.ingenium.mixin.world.MixinChunkTicketManager",
        "com.ingenium.mixin.world.MixinServerChunkLoadingManager"
    );
 
    // ── Mixins to suppress when Sodium is present ───────────────────
    private static final Set<String> SODIUM_CONFLICTS = Set.of(
        "com.ingenium.mixin.client.MixinChunkRenderCache",
        "com.ingenium.mixin.client.MixinBuiltChunkStorage"
    );
 
    // ── Mixins to suppress when Starlight is present ────────────────
    private static final Set<String> STARLIGHT_CONFLICTS = Set.of(
        "com.ingenium.mixin.world.MixinLightingProvider",
        "com.ingenium.mixin.world.MixinChunkLightProvider"
    );

    // ── Mixins to suppress when Fabric Item API is present ───────────
    private static final Set<String> FABRIC_ITEM_API_CONFLICTS = Set.of(
        "com.ingenium.mixin.ItemMixin",
        "com.ingenium.mixin.ItemStackMixin",
        "com.ingenium.mixin.EquipmentSlotMixin",
        "com.ingenium.mixin.compat.ItemAttributeCompatMixin"
    );
 
    @Override
    public boolean shouldApplyMixin(String targetClass, String mixinClass) {
        if (CompatibilityBridge.HAS_LITHIUM && LITHIUM_CONFLICTS.contains(mixinClass)) {
            log("Suppressing " + simpleName(mixinClass) + " [Lithium]");
            return false;
        }
        if (CompatibilityBridge.HAS_SODIUM && SODIUM_CONFLICTS.contains(mixinClass)) {
            log("Suppressing " + simpleName(mixinClass) + " [Sodium]");
            return false;
        }
        if (CompatibilityBridge.HAS_STARLIGHT && STARLIGHT_CONFLICTS.contains(mixinClass)) {
            log("Suppressing " + simpleName(mixinClass) + " [Starlight]");
            return false;
        }
        if (CompatibilityBridge.HAS_FABRIC_ITEM_API && FABRIC_ITEM_API_CONFLICTS.contains(mixinClass)) {
            log("Suppressing " + simpleName(mixinClass) + " [Fabric Item API]");
            return false;
        }
        return true;
    }
 
    private static void log(String msg) {
        // Use LoggerFactory directly — IngeniumConfig may not be loaded yet
        LoggerFactory.getLogger("Ingenium/Compat").info(msg);
    }
 
    private static String simpleName(String fqcn) {
        return fqcn.substring(fqcn.lastIndexOf('.') + 1);
    }
 
    // Required IMixinConfigPlugin interface methods
    @Override public void onLoad(String pkg) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String tc, ClassNode tn, String mc, IMixinInfo mi) {}
    @Override public void postApply(String tc, ClassNode tn, String mc, IMixinInfo mi) {}
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
}
