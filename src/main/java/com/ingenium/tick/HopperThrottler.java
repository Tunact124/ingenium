package com.ingenium.tick;

import com.ingenium.collections.IngeniumCollections;
import com.ingenium.collections.LongObjectMap;

public final class HopperThrottler {
    private static final int DEFAULT_CAPACITY = 16_384;

    private final LongObjectMap<Entry> entries = IngeniumCollections.newRemovalHeavyLongObjectMap(DEFAULT_CAPACITY);

    public boolean isCoolingDown(long hopperPosLong, long gameTime) {
        var e = entries.get(hopperPosLong);
        return e != null && gameTime < e.cooldownUntilTick;
    }

    public void cooldown(long hopperPosLong, long cooldownUntilTick) {
        entries.put(hopperPosLong, new Entry(cooldownUntilTick));
    }

    public boolean shouldApplyCooldown(long hopperPosLong, long gameTime) {
        // You can replace this with a stronger signal (e.g., “moved items == false”)
        // from a deeper wrap of insertAndExtract if you later decide to target that call site.
        return true;
    }

    private record Entry(long cooldownUntilTick) {}
}
