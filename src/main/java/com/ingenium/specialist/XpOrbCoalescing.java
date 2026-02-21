package com.ingenium.specialist;

public final class XpOrbCoalescing {
    private static final int MERGE_EVERY_N_TICKS = 5;
    private static final int MAX_VALUE_PER_ORB = 2477;

    private XpOrbCoalescing() {}

    public static boolean shouldAttemptMerge(int tickCount) {
        return (tickCount % MERGE_EVERY_N_TICKS) == 0;
    }

    public static int clampOrbValue(int value) {
        return Math.min(value, MAX_VALUE_PER_ORB);
    }
}
