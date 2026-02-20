package com.ingenium.offheap;

/**
 * Off-heap oriented metadata store for per-block-entity state used by throttling / diagnostics.
 *
 * <p>All methods are intended to be called from the server thread unless otherwise noted.
 * Cross-thread access must be done via external synchronization or a command queue.
 */
public interface IBeMetadataStore extends AutoCloseable {

    /**
     * Returns true if this store is operational (feature enabled + allocation succeeded).
     */
    boolean isEnabled();

    /**
     * Ensure a record exists for {@code packedPos}, returning its internal record id.
     */
    int ensureRecord(long packedPos);

    /**
     * Returns lastTickTime for the record associated with packedPos, or 0 if missing.
     */
    long getLastTickTime(long packedPos);

    /**
     * Sets lastTickTime for the record associated with packedPos (creating record if needed).
     */
    void setLastTickTime(long packedPos, long worldTime);

    /**
     * Reads skipCount for packedPos, or 0 if missing.
     */
    int getSkipCount(long packedPos);

    /**
     * Sets skipCount for packedPos (creating record if needed).
     */
    void setSkipCount(long packedPos, int skipCount);

    /**
     * Bitwise OR flags into current flags field (creating record if needed).
     */
    void orFlags(long packedPos, int flags);

    /**
     * Clears bits in flags for packedPos (no-op if missing).
     */
    void clearFlags(long packedPos, int flags);

    /**
     * Reads flags for packedPos, or 0 if missing.
     */
    int getFlags(long packedPos);

    /**
     * Removes the record for packedPos. Implementation may tombstone to avoid reshuffling.
     */
    void remove(long packedPos);

    /**
     * Optional: snapshot for debugging/diagnostics (avoid calling in hot path).
     */
    BeMetadata snapshot(long packedPos);

    @Override
    void close();
}
