package com.ingenium.core.hw;

public record HardwareProfile(
        int logicalProcessors,
        long maxHeapBytes,
        double calibrationOpsPerMs,
        int qualityScore,
        HardwareTier tier
) {}
