package com.ingenium.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ChunkStampRegistry:
 * - version-stamps per chunk position to validate async snapshot commits.
 */
public final class ChunkStampRegistry {
    private static final ConcurrentHashMap<Long, AtomicLong> STAMPS = new ConcurrentHashMap<>();

    private ChunkStampRegistry() {}

    public static long currentVersion(long packedChunkPos) {
        return STAMPS.computeIfAbsent(packedChunkPos, k -> new AtomicLong(0)).get();
    }

    public static void invalidate(long packedChunkPos) {
        STAMPS.computeIfAbsent(packedChunkPos, k -> new AtomicLong(0)).incrementAndGet();
    }

    public static void remove(long packedChunkPos) {
        STAMPS.remove(packedChunkPos);
    }
}
