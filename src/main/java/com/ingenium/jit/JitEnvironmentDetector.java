package com.ingenium.jit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.ArrayList;

/**
 * JIT Environment Detector
 * 
 * Detects which JVM and JIT compiler the user is running,
 * then provides vendor-specific recommendations.
 */
public final class JitEnvironmentDetector {
    
    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/JIT");
    
    public enum JvmVendor {
        HOTSPOT, OPENJ9, GRAALVM, UNKNOWN
    }
    
    public enum JitTier {
        C1_ONLY, TIERED, C2_ONLY, GRAAL_JIT, UNKNOWN
    }
    
    private final JvmVendor vendor;
    private final JitTier jitTier;
    private final int availableProcessors;
    private final long maxHeapMB;
    private final String javaVersion;
    private final boolean isServer;
    
    public JitEnvironmentDetector() {
        this.javaVersion = System.getProperty("java.version", "unknown");
        this.vendor = detectVendor();
        this.jitTier = detectJitTier();
        this.availableProcessors = Runtime.getRuntime().availableProcessors();
        this.maxHeapMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        this.isServer = detectIfServer();
        
        LOG.info("[Ingenium/JIT] Environment detected:");
        LOG.info("  Java:       {}", javaVersion);
        LOG.info("  JVM:        {}", vendor);
        LOG.info("  JIT:        {}", jitTier);
        LOG.info("  CPUs:       {}", availableProcessors);
        LOG.info("  Max Heap:   {}MB", maxHeapMB);
        LOG.info("  Server:     {}", isServer);
    }
    
    private JvmVendor detectVendor() {
        String vmName = System.getProperty("java.vm.name", "").toLowerCase();
        if (vmName.contains("graalvm") || vmName.contains("graal")) return JvmVendor.GRAALVM;
        if (vmName.contains("openj9") || vmName.contains("j9")) return JvmVendor.OPENJ9;
        if (vmName.contains("hotspot") || vmName.contains("openjdk")) return JvmVendor.HOTSPOT;
        return JvmVendor.UNKNOWN;
    }
    
    private JitTier detectJitTier() {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : jvmArgs) {
            if (arg.contains("TieredStopAtLevel=1") || arg.contains("-Xint")) return JitTier.C1_ONLY;
        }
        if (vendor == JvmVendor.GRAALVM) return JitTier.GRAAL_JIT;
        if (vendor == JvmVendor.HOTSPOT) return JitTier.TIERED;
        return JitTier.UNKNOWN;
    }
    
    private boolean detectIfServer() {
        try {
            Class.forName("net.minecraft.server.dedicated.DedicatedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public List<String> generateRecommendations() {
        List<String> recommendations = new ArrayList<>();
        if (jitTier == JitTier.C1_ONLY) {
            recommendations.add("CRITICAL: JIT is limited to C1. Remove -XX:TieredStopAtLevel=1 for 2-5x performance.");
        }
        if (maxHeapMB < 2048) {
            recommendations.add("WARNING: Max heap is " + maxHeapMB + "MB. Recommend at least 3GB (-Xmx3G).");
        }
        if (vendor == JvmVendor.HOTSPOT) {
            recommendations.add("SUGGESTION: Consider -XX:+UseZGC for Java 17+ to reduce GC pauses.");
        }
        return recommendations;
    }
}
