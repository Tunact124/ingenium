package com.ingenium.simd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SIMDCapability {
    private static final Logger LOGGER = LoggerFactory.getLogger("Ingenium/SIMD");
    private static final boolean AVAILABLE;
    private static final int INT_LANES;
    private static final int DOUBLE_LANES;

    static {
        boolean found = false;
        int iLanes = 1;
        int dLanes = 1;

        try {
            // Confirm the module is present
            Class.forName("jdk.incubator.vector.IntVector");
            found = true;
            // Now we can safely load VectorHelper which uses the actual classes
            iLanes = VectorHelper.getIntLanes();
            dLanes = VectorHelper.getDoubleLanes();
        } catch (Throwable e) {
            found = false;
            iLanes = 1;
            dLanes = 1;
        }
        
        AVAILABLE = found;
        INT_LANES = iLanes;
        DOUBLE_LANES = dLanes;

        if (AVAILABLE) {
            LOGGER.info("[Ingenium] Vector API: available, intLanes={}, doubleLanes={}", INT_LANES, DOUBLE_LANES);
        } else {
            LOGGER.warn("[Ingenium] Vector API not available. " +
                    "Falling back to scalar math. For best performance, use Java 21+ " +
                    "or add '--add-modules jdk.incubator.vector' to JVM flags.");
        }
    }

    private SIMDCapability() {}

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static int getIntLanes() {
        return INT_LANES;
    }

    public static int getDoubleLanes() {
        return DOUBLE_LANES;
    }
}
