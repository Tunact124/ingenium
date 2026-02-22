package com.ingenium.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Buddy Logic — Ingenium's mod compatibility detection system.
 */
public final class BuddyLogic {

    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/BuddyLogic");

    public enum KnownMod {
        SODIUM("sodium"),
        LITHIUM("lithium"),
        FERRITECORE("ferritecore"),
        KRYPTON("krypton"),
        C2ME("c2me"),
        IRIS("iris"),
        EFFICIENT_HASHING("efficient_hashing"),
        MODERN_FIX("modernfix"),
        ENTITY_CULLING("entityculling"),
        FLEROVIUM("flerovium"),
        CERULEAN("cerulean"),
        IMMEDIATELY_FAST("immediatelyfast"),
        NOISIUM("noisium"),
        CLUMPS("clumps"),
        ALTERNATE_CURRENT("alternate_current");

        public final String modId;

        KnownMod(String modId) {
            this.modId = modId;
        }
    }

    private static final Map<String, Boolean> presenceCache = new ConcurrentHashMap<>();
    private static final EnumMap<KnownMod, ModDetectionResult> detectionResults = 
        new EnumMap<>(KnownMod.class);

    private static volatile boolean earlyInitDone = false;
    private static volatile boolean fullInitDone = false;

    public static void earlyInit() {
        if (earlyInitDone) return;
        synchronized (BuddyLogic.class) {
            if (earlyInitDone) return;

            LOG.info("[Ingenium] BuddyLogic early init — scanning mod list");

            for (KnownMod mod : KnownMod.values()) {
                boolean present = FabricLoader.getInstance().isModLoaded(mod.modId);
                presenceCache.put(mod.modId, present);
                if (present) {
                    String version = FabricLoader.getInstance()
                        .getModContainer(mod.modId)
                        .map(c -> c.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown");
                    LOG.info("[Ingenium]   ✓ {} detected (version {})", mod.modId, version);
                } else {
                    LOG.debug("[Ingenium]   ✗ {} not found", mod.modId);
                }
            }

            earlyInitDone = true;
        }
    }

    public static void fullInit() {
        if (fullInitDone) return;
        if (!earlyInitDone) earlyInit();

        synchronized (BuddyLogic.class) {
            if (fullInitDone) return;

            LOG.info("[Ingenium] BuddyLogic full init — analyzing compatibility");

            if (isModLoaded("ferritecore")) analyzeFerriteCore();
            if (isModLoaded("sodium")) analyzeSodium();
            if (isModLoaded("lithium")) analyzeLithium();

            logCompatibilitySummary();
            fullInitDone = true;
        }
    }

    public static boolean isModLoaded(String modId) {
        Boolean cached = presenceCache.get(modId);
        if (cached != null) return cached;
        boolean present = FabricLoader.getInstance().isModLoaded(modId);
        presenceCache.put(modId, present);
        return present;
    }

    public static boolean isPresent(KnownMod mod) {
        return isModLoaded(mod.modId);
    }

    public static ModDetectionResult getResult(KnownMod mod) {
        return detectionResults.getOrDefault(mod, ModDetectionResult.NOT_ANALYZED);
    }

    public static void logYield(String ingeniumSystem, String yieldingTo, String reason) {
        LOG.info("[Ingenium] YIELD: {} → {} ({})", ingeniumSystem, yieldingTo, reason);
    }

    public static void logEnhance(String ingeniumSystem, String enhancing, String reason) {
        LOG.info("[Ingenium] ENHANCE: {} + {} ({})", ingeniumSystem, enhancing, reason);
    }

    public static void logStack(String ingeniumSystem, String stackingWith, String reason) {
        LOG.info("[Ingenium] STACK: {} ∥ {} ({})", ingeniumSystem, stackingWith, reason);
    }

    private static void analyzeFerriteCore() {
        ModDetectionResult.Builder result = new ModDetectionResult.Builder("ferritecore");
        try {
            Class.forName("malte0811.ferritecore.fastmap.FastMapStateHolder");
            result.addCapability("fastmap", true);
            LOG.info("[Ingenium]   FerriteCore FastMap: ACTIVE — stateIndex available");
        } catch (ClassNotFoundException e) {
            result.addCapability("fastmap", false);
        }
        try {
            Class.forName("malte0811.ferritecore.impl.BlockStateCacheImpl");
            result.addCapability("dedup_cache", true);
            LOG.info("[Ingenium]   FerriteCore Cache Dedup: ACTIVE — VoxelShape == valid");
        } catch (ClassNotFoundException e) {
            result.addCapability("dedup_cache", false);
        }
        detectionResults.put(KnownMod.FERRITECORE, result.build());
    }

    private static void analyzeSodium() {
        ModDetectionResult.Builder result = new ModDetectionResult.Builder("sodium");
        try {
            Class.forName("net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter");
            result.addCapability("vertex_writer_api", true);
            LOG.info("[Ingenium]   Sodium VertexBufferWriter API: AVAILABLE");
        } catch (ClassNotFoundException e) {
            result.addCapability("vertex_writer_api", false);
        }
        detectionResults.put(KnownMod.SODIUM, result.build());
    }

    private static void analyzeLithium() {
        ModDetectionResult.Builder result = new ModDetectionResult.Builder("lithium");
        try {
            Class.forName("me.jellysquid.mods.lithium.common.world.scheduler.LithiumWorldTickScheduler");
            result.addCapability("tick_scheduler", true);
            LOG.info("[Ingenium]   Lithium tick scheduler: ACTIVE");
        } catch (ClassNotFoundException e) {
            result.addCapability("tick_scheduler", false);
        }
        detectionResults.put(KnownMod.LITHIUM, result.build());
    }

    private static void logCompatibilitySummary() {
        LOG.info("[Ingenium] ═══════════════════════════════════════");
        LOG.info("[Ingenium] Compatibility Summary:");
        int detected = 0;
        for (KnownMod mod : KnownMod.values()) {
            if (isPresent(mod)) detected++;
        }
        LOG.info("[Ingenium]   Mods detected: {}/{}", detected, KnownMod.values().length);
        LOG.info("[Ingenium] ═══════════════════════════════════════");
    }

    private BuddyLogic() {}

    public static final class ModDetectionResult {
        public static final ModDetectionResult NOT_ANALYZED = new ModDetectionResult("none", Map.of());
        private final String modId;
        private final Map<String, Boolean> capabilities;
        private ModDetectionResult(String modId, Map<String, Boolean> capabilities) {
            this.modId = modId;
            this.capabilities = capabilities;
        }
        public boolean hasCapability(String capability) {
            return capabilities.getOrDefault(capability, false);
        }
        public String getModId() { return modId; }

        static class Builder {
            private final String modId;
            private final Map<String, Boolean> capabilities = new java.util.HashMap<>();
            Builder(String modId) { this.modId = modId; }
            Builder addCapability(String name, boolean present) {
                capabilities.put(name, present);
                return this;
            }
            ModDetectionResult build() {
                return new ModDetectionResult(modId, Map.copyOf(capabilities));
            }
        }
    }
}
