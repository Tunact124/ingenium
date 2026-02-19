package com.ingenium.threading;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.util.math.ChunkPos;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class ChunkStampRegistry {
 
    // One LongAdder per loaded chunk. ConcurrentHashMap for thread-safe access.
    private static final ConcurrentHashMap<Long, LongAdder> STAMPS =
        new ConcurrentHashMap<>(512);
 
    /** Get current version for a chunk. Creates entry if absent. */
    public static long getVersion(ChunkPos pos) {
        return STAMPS.computeIfAbsent(pack(pos), k -> new LongAdder()).sum();
    }
 
    /** Increment version. Call on every block change within the chunk. */
    public static void invalidate(ChunkPos pos) {
        STAMPS.computeIfAbsent(pack(pos), k -> new LongAdder()).increment();
    }
 
    /** Remove entry when chunk unloads. Prevents unbounded map growth. */
    public static void evict(ChunkPos pos) {
        STAMPS.remove(pack(pos));
    }
 
    /** Eviction registered via ServerChunkEvents.CHUNK_UNLOAD. */
    public static void registerChunkEvents() {
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) ->
            evict(chunk.getPos()));
    }
 
    private static long pack(ChunkPos p) {
        return (long) p.x << 32 | (p.z & 0xFFFFFFFFL);
    }
}
