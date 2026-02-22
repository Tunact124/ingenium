package com.ingenium.tick;

import com.ingenium.collections.IngeniumCollections;
import com.ingenium.collections.LongObjectMap;
import com.ingenium.core.IngeniumGovernor;

public final class HopperThrottler {
    private static final int DEFAULT_CAPACITY = 16_384;

    private final LongObjectMap<Entry> entries = IngeniumCollections.newRemovalHeavyLongObjectMap(DEFAULT_CAPACITY);

    public boolean isCoolingDown(long hopperPosLong, long gameTime) {
        var e = entries.get(hopperPosLong);
        return e != null && gameTime < e.cooldownUntilTick;
    }

    public void cooldown(long hopperPosLong, long gameTime) {
        int divisor = IngeniumGovernor.get().getProfile().beDivisor;
        if (divisor > 1) {
            entries.put(hopperPosLong, new Entry(gameTime + divisor));
        }
    }

    public boolean shouldApplyCooldown(long hopperPosLong, long gameTime) {
        // Apply cooldown if the server is under stress (governor profile > AGGRESSIVE)
        return IngeniumGovernor.get().getProfile() != IngeniumGovernor.OptimizationProfile.AGGRESSIVE;
    }

    private record Entry(long cooldownUntilTick) {}
}
