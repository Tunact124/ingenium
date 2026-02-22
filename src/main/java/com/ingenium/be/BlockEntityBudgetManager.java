package com.ingenium.be;

import com.ingenium.core.IngeniumGovernor.OptimizationProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the tick budget for block entity processing.
 */
public final class BlockEntityBudgetManager {

    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/BEBudget");

    private static final int MAX_CONSECUTIVE_SKIPS = 4;

    private static final float[] BUDGET_PERCENTAGES = {
        0.30f,  // AGGRESSIVE
        0.40f,  // BALANCED
        0.50f,  // REACTIVE
        0.20f   // EMERGENCY
    };

    public static boolean shouldTickBlockEntity(
            double distanceSquared,
            int skipCount,
            long worldTime,
            OptimizationProfile profile) {

        if (skipCount >= MAX_CONSECUTIVE_SKIPS) {
            return true;
        }

        if (distanceSquared <= 1024.0) { // 32 blocks
            return true;
        }

        int divisor = computeDivisor(distanceSquared, profile);
        return (worldTime % divisor) == 0;
    }

    private static int computeDivisor(double distSq, OptimizationProfile profile) {
        int baseDivisor;
        if (distSq <= 4096.0) {       // 64 blocks
            baseDivisor = 2;
        } else if (distSq <= 16384.0) { // 128 blocks
            baseDivisor = 4;
        } else {
            baseDivisor = 8;
        }

        float multiplier = switch (profile) {
            case AGGRESSIVE -> 2.0f;
            case BALANCED -> 1.0f;
            case REACTIVE -> 0.5f;
            case EMERGENCY -> 3.0f;
        };

        return Math.max(1, Math.round(baseDivisor * multiplier));
    }

    public static long getBudgetNanos(OptimizationProfile profile, long totalTickBudgetNs) {
        float percentage = BUDGET_PERCENTAGES[profile.ordinal()];
        return (long) (totalTickBudgetNs * percentage);
    }

    private BlockEntityBudgetManager() {}
}
