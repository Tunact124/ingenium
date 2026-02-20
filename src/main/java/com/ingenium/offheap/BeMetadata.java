package com.ingenium.offheap;

/**
 * Immutable snapshot of BE throttle/profiling metadata.
 * Kept as a record to encourage explicit, allocation-aware usage.
 *
 * <p>Hot paths should avoid constructing this repeatedly; prefer direct getters in the store.
 */
public record BeMetadata(
        long packedPos,
        long lastTickTime,
        int skipCount,
        int flags
) {
    /** Flag bit: BE is forced to tick regardless of policy (e.g., critical). */
    public static final int FLAG_FORCE_TICK = 1 << 0;

    /** Flag bit: entry has been logically removed (for debugging / safety). */
    public static final int FLAG_TOMBSTONE = 1 << 1;
}
