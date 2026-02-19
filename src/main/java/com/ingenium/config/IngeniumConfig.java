package com.ingenium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class IngeniumConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "ingenium.json";

    private static volatile IngeniumConfig INSTANCE;

    public static void init() {
        INSTANCE = load();
    }

    public static IngeniumConfig get() {
        return INSTANCE;
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    // --------------------------
    // Core / Governor
    // --------------------------
    public volatile boolean masterEnabled = true;

    /** "Governor Emergency Threshold: MSPT value at which non-critical block entities begin skipping ticks. [Default: 45ms, Range: 20-100]" */
    public volatile float emergencyMsptThreshold = 45.0f;

    /** "GC Coordination: Monitors heap occupancy each tick... [Default: ON]" */
    public volatile boolean enableGcCoordination = true;

    // --------------------------
    // Scheduled ticks (Wheel replacement)
    // --------------------------
    /** "Scheduled Tick Bucketing: Replaces vanilla's O(log n) tree queue with O(1) hierarchical timing wheels... [Default: ON]" */
    public volatile boolean scheduledTickWheelEnabled = true;

    /** wheel slot count; power-of-two strongly recommended */
    public volatile int timingWheelBucketCount = 256;

    // --------------------------
    // Block entity throttling
    // --------------------------
    /** "Block Entity Throttling: Tiered ticking... [Default: ON]" */
    public volatile boolean blockEntityThrottlingEnabled = true;

    /** Radius within which BEs are CRITICAL */
    public volatile int blockEntityCriticalRadius = 32;

    /** Enables occlusion heuristic (experimental; default false) */
    public volatile boolean blockEntityOcclusionHeuristic = false;

    // --------------------------
    // Off-heap BE metadata
    // --------------------------
    /** "Off-Heap Block Entity Cache... [Default: OFF]" */
    public volatile boolean offHeapBlockEntityDataEnabled = false;

    // --------------------------
    // SIMD (experimental)
    // --------------------------
    public volatile boolean simdPaletteEnabled = false;

    // --------------------------
    // Debug / Offboarding
    // --------------------------
    /** If true, wheel will flush-to-vanilla on every save (recommended for safety) */
    public volatile boolean offboardOnSaveEnabled = true;

    @Override
    public String toString() {
        return "IngeniumConfig{" +
                "master=" + masterEnabled +
                ", emergencyMspt=" + emergencyMsptThreshold +
                ", wheel=" + scheduledTickWheelEnabled +
                ", wheelBuckets=" + timingWheelBucketCount +
                ", beThrottle=" + blockEntityThrottlingEnabled +
                ", offHeapBE=" + offHeapBlockEntityDataEnabled +
                ", gcCoord=" + enableGcCoordination +
                ", simd=" + simdPaletteEnabled +
                ", offboardOnSave=" + offboardOnSaveEnabled +
                '}';
    }

    public void save() {
        try {
            Files.writeString(path(), GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // avoid logger dependency here; upstream will report in practice
            e.printStackTrace();
        }
    }

    private static IngeniumConfig load() {
        Path p = path();
        try {
            if (!Files.exists(p)) {
                IngeniumConfig cfg = new IngeniumConfig();
                cfg.save();
                return cfg;
            }
            String json = Files.readString(p, StandardCharsets.UTF_8);
            IngeniumConfig cfg = GSON.fromJson(json, IngeniumConfig.class);
            return (cfg != null) ? cfg : new IngeniumConfig();
        } catch (Exception e) {
            e.printStackTrace();
            return new IngeniumConfig();
        }
    }

    // For compatibility with some snippets that use getInstance()
    public static IngeniumConfig getInstance() {
        return get();
    }

    // Adding some fields that appear later in the description
    public volatile boolean scheduledTickBucketingEnabled = true; // used in some snippets instead of scheduledTickWheelEnabled
    public volatile boolean offboardRequested = false;
}
