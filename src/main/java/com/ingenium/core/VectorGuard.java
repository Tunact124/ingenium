package com.ingenium.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VectorGuard: Hardware and JVM capability detection.
 * Ensures high-end features (SIMD, FFM API, Vulkan native) only run on
 * supported hardware.
 */
public class VectorGuard {
    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/VectorGuard");

    private static final boolean HAS_VECTOR_API;
    private static final boolean HAS_FFM_API;

    static {
        boolean vectorApi = false;
        boolean ffmApi = false;

        // Very basic checks for Java 21+ incubator/preview features
        try {
            Class.forName("jdk.incubator.vector.VectorSpecies");
            vectorApi = true;
        } catch (ClassNotFoundException e) {
            LOG.warn("Vector API not found. SIMD fast paths will be disabled.");
        }

        try {
            Class.forName("java.lang.foreign.MemorySegment");
            ffmApi = true;
        } catch (ClassNotFoundException e) {
            LOG.warn("Foreign Function & Memory API not found. Direct NIO to GPU streaming will be disabled.");
        }

        HAS_VECTOR_API = vectorApi;
        HAS_FFM_API = ffmApi;
    }

    public static boolean hasVectorAPI() {
        return HAS_VECTOR_API;
    }

    public static boolean hasForeignFunctionMemoryAPI() {
        return HAS_FFM_API;
    }
}
