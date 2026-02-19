package com.ingenium.config;

import dev.isxander.yacl3.config.ConfigInstance;
import dev.isxander.yacl3.config.GsonConfigInstance;
import net.fabricmc.loader.api.FabricLoader;

public class IngeniumConfig {
 
    public static final ConfigInstance<IngeniumConfig> HANDLER =
        GsonConfigInstance.createBuilder(IngeniumConfig.class)
            .setPath(FabricLoader.getInstance().getConfigDir().resolve("ingenium.json"))
            .build();
 
    public static IngeniumConfig get() { return HANDLER.getConfig(); }
 
    // ── Governor ──
    public boolean enableGovernor            = true;
    public int     msptEmergencyThreshold    = 45;   // Bug-fixed: was hardcoded 50
    public int     msptBalancedThreshold     = 30;
    public int     hysteresisTickWindow      = 60;   // ticks before profile switch
 
    // ── Threading ──
    public boolean enableAsyncPathfinding    = true;
    public boolean enableAsyncWorldSave      = true;
    public int     computePoolSize           = -1;   // -1 = auto (cpus - 2)
    public int     maxCommitsPerTick         = 20;
 
    // ── Rendering ──
    public boolean enableInstancedRendering  = true;
    public boolean enableFrustumCulling      = true;
    public boolean enableDirtyTracking       = true;
 
    // ── Memory ──
    public boolean enableObjectPooling       = true;
    public boolean enableGcHints             = true;
    public int     blockPosPoolCapacity      = 256;
 
    // ── Compatibility ──
    public boolean verboseCompatLogging      = false;

    /**
     * Toggles all optimization-related features.
     * Used by the benchmark to test baseline performance.
     */
    public void setAllFeaturesEnabled(boolean enabled) {
        this.enableGovernor = enabled;
        this.enableAsyncPathfinding = enabled;
        this.enableAsyncWorldSave = enabled;
        this.enableInstancedRendering = enabled;
        this.enableFrustumCulling = enabled;
        this.enableDirtyTracking = enabled;
        this.enableObjectPooling = enabled;
        this.enableGcHints = enabled;
    }
}
