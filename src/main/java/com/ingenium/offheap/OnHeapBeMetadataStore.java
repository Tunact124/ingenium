package com.ingenium.offheap;

import com.ingenium.config.IngeniumConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback store when off-heap is disabled or unavailable.
 * Not optimized; intended as a safety fallback to preserve vanilla behavior.
 */
public final class OnHeapBeMetadataStore implements IBeMetadataStore {
    private final boolean enabled;
    private final Map<Long, BeMetadata> map = new HashMap<>();

    public OnHeapBeMetadataStore() {
        this.enabled = IngeniumConfig.get().offHeapBlockEntityDataEnabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int ensureRecord(long packedPos) {
        map.computeIfAbsent(packedPos, p -> new BeMetadata(p, 0L, 0, 0));
        return 0;
    }

    @Override
    public long getLastTickTime(long packedPos) {
        return map.getOrDefault(packedPos, new BeMetadata(packedPos, 0L, 0, 0)).lastTickTime();
    }

    @Override
    public void setLastTickTime(long packedPos, long worldTime) {
        BeMetadata cur = map.getOrDefault(packedPos, new BeMetadata(packedPos, 0L, 0, 0));
        map.put(packedPos, new BeMetadata(packedPos, worldTime, cur.skipCount(), cur.flags()));
    }

    @Override
    public int getSkipCount(long packedPos) {
        return map.getOrDefault(packedPos, new BeMetadata(packedPos, 0L, 0, 0)).skipCount();
    }

    @Override
    public void setSkipCount(long packedPos, int skipCount) {
        BeMetadata cur = map.getOrDefault(packedPos, new BeMetadata(packedPos, 0L, 0, 0));
        map.put(packedPos, new BeMetadata(packedPos, cur.lastTickTime(), skipCount, cur.flags()));
    }

    @Override
    public void orFlags(long packedPos, int flags) {
        BeMetadata cur = map.getOrDefault(packedPos, new BeMetadata(packedPos, 0L, 0, 0));
        map.put(packedPos, new BeMetadata(packedPos, cur.lastTickTime(), cur.skipCount(), cur.flags() | flags));
    }

    @Override
    public void clearFlags(long packedPos, int flags) {
        BeMetadata cur = map.get(packedPos);
        if (cur == null) return;
        map.put(packedPos, new BeMetadata(packedPos, cur.lastTickTime(), cur.skipCount(), cur.flags() & ~flags));
    }

    @Override
    public int getFlags(long packedPos) {
        return map.getOrDefault(packedPos, new BeMetadata(packedPos, 0L, 0, 0)).flags();
    }

    @Override
    public void remove(long packedPos) {
        map.remove(packedPos);
    }

    @Override
    public BeMetadata snapshot(long packedPos) {
        return map.getOrDefault(packedPos, new BeMetadata(packedPos, 0L, 0, 0));
    }

    @Override
    public void close() {
        map.clear();
    }
}
