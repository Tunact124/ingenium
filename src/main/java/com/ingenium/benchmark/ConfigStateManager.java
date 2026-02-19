package com.ingenium.benchmark;

import com.ingenium.config.IngeniumConfig;
import java.util.HashMap;
import java.util.Map;

public class ConfigStateManager {
    private final Map<String, Boolean> originalBooleans = new HashMap<>();
    private final Map<String, Integer> originalInts = new HashMap<>();
    
    public void captureState() {
        IngeniumConfig config = IngeniumConfig.get();
        originalBooleans.clear();
        originalInts.clear();
        
        originalBooleans.put("enableGovernor", config.enableGovernor);
        originalBooleans.put("enableAsyncPathfinding", config.enableAsyncPathfinding);
        originalBooleans.put("enableAsyncWorldSave", config.enableAsyncWorldSave);
        originalBooleans.put("enableInstancedRendering", config.enableInstancedRendering);
        originalBooleans.put("enableFrustumCulling", config.enableFrustumCulling);
        originalBooleans.put("enableDirtyTracking", config.enableDirtyTracking);
        originalBooleans.put("enableObjectPooling", config.enableObjectPooling);
        originalBooleans.put("enableGcHints", config.enableGcHints);
        
        originalInts.put("msptEmergencyThreshold", config.msptEmergencyThreshold);
        originalInts.put("msptBalancedThreshold", config.msptBalancedThreshold);
        originalInts.put("hysteresisTickWindow", config.hysteresisTickWindow);
        originalInts.put("computePoolSize", config.computePoolSize);
        originalInts.put("maxCommitsPerTick", config.maxCommitsPerTick);
        originalInts.put("blockPosPoolCapacity", config.blockPosPoolCapacity);
    }
    
    public void disableAllFeatures() {
        IngeniumConfig config = IngeniumConfig.get();
        config.setAllFeaturesEnabled(false);
        IngeniumConfig.HANDLER.save();
    }
    
    public void enableAllFeatures() {
        IngeniumConfig config = IngeniumConfig.get();
        config.setAllFeaturesEnabled(true);
        IngeniumConfig.HANDLER.save();
    }
    
    public void restoreOriginalState() {
        IngeniumConfig config = IngeniumConfig.get();
        
        originalBooleans.forEach((key, value) -> {
            switch (key) {
                case "enableGovernor" -> config.enableGovernor = value;
                case "enableAsyncPathfinding" -> config.enableAsyncPathfinding = value;
                case "enableAsyncWorldSave" -> config.enableAsyncWorldSave = value;
                case "enableInstancedRendering" -> config.enableInstancedRendering = value;
                case "enableFrustumCulling" -> config.enableFrustumCulling = value;
                case "enableDirtyTracking" -> config.enableDirtyTracking = value;
                case "enableObjectPooling" -> config.enableObjectPooling = value;
                case "enableGcHints" -> config.enableGcHints = value;
            }
        });
        
        originalInts.forEach((key, value) -> {
            switch (key) {
                case "msptEmergencyThreshold" -> config.msptEmergencyThreshold = value;
                case "msptBalancedThreshold" -> config.msptBalancedThreshold = value;
                case "hysteresisTickWindow" -> config.hysteresisTickWindow = value;
                case "computePoolSize" -> config.computePoolSize = value;
                case "maxCommitsPerTick" -> config.maxCommitsPerTick = value;
                case "blockPosPoolCapacity" -> config.blockPosPoolCapacity = value;
            }
        });
        
        IngeniumConfig.HANDLER.save();
        System.out.println("[Ingenium Benchmark] Config restored to original state");
    }
}
