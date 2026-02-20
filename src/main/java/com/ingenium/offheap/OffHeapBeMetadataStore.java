package com.ingenium.offheap;

import com.ingenium.config.IngeniumConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Off-heap metadata store backed by a direct ByteBuffer "arena" of fixed-size records.
 *
 * <p>Record layout (aligned to 32 bytes):
 * <pre>
 * 0  : long packedPos
 * 8  : long lastTickTime
 * 16 : int  skipCount
 * 20 : int  flags
 * 24 : int  nextFree (free list pointer, -1 if used)
 * 28 : int  reserved
 * </pre>
 *
 * <p>Threading: intended for server thread access. If you must access from other threads,
 * guard with a lock or queue updates to server thread.
 */
public final class OffHeapBeMetadataStore implements IBeMetadataStore {
    private static final int RECORD_BYTES = 32;

    private static final int OFF_PACKED_POS = 0;
    private static final int OFF_LAST_TICK  = 8;
    private static final int OFF_SKIP_COUNT = 16;
    private static final int OFF_FLAGS      = 20;
    private static final int OFF_NEXT_FREE  = 24;

    private final boolean enabled;
    private final ByteBuffer buf; // direct
    private final int capacityRecords;

    private final LongToIntIndex index;

    private int size;
    private int freeHead; // record id, -1 if none

    public OffHeapBeMetadataStore(int capacityRecords) {
        boolean on = false;
        ByteBuffer arena = null;

        if (IngeniumConfig.get().offHeapBlockEntityDataEnabled) {
            try {
                arena = ByteBuffer.allocateDirect(Math.max(1, capacityRecords) * RECORD_BYTES)
                        .order(ByteOrder.LITTLE_ENDIAN);
                on = true;
            } catch (Throwable t) {
                // Allocation failed: fall back to disabled store behavior.
                on = false;
                arena = null;
            }
        }

        this.enabled = on;
        this.buf = arena;
        this.capacityRecords = Math.max(1, capacityRecords);
        this.index = LongToIntIndex.createPreferKoloboke();
        this.size = 0;
        this.freeHead = -1;

        if (enabled) {
            // Initialize free list to empty (we grow linearly until capacity).
            // We do not pre-fill nextFree fields to keep init time low.
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int ensureRecord(long packedPos) {
        if (!enabled) return -1;

        int id = index.getOrDefault(packedPos, LongToIntIndex.NO_VALUE);
        if (id != LongToIntIndex.NO_VALUE) return id;

        id = allocateRecord();
        if (id < 0) return -1;

        writeLong(id, OFF_PACKED_POS, packedPos);
        writeLong(id, OFF_LAST_TICK, 0L);
        writeInt(id, OFF_SKIP_COUNT, 0);
        writeInt(id, OFF_FLAGS, 0);
        writeInt(id, OFF_NEXT_FREE, -1);

        index.put(packedPos, id);
        return id;
    }

    @Override
    public long getLastTickTime(long packedPos) {
        if (!enabled) return 0L;
        int id = index.getOrDefault(packedPos, LongToIntIndex.NO_VALUE);
        if (id == LongToIntIndex.NO_VALUE) return 0L;
        return readLong(id, OFF_LAST_TICK);
    }

    @Override
    public void setLastTickTime(long packedPos, long worldTime) {
        if (!enabled) return;
        int id = ensureRecord(packedPos);
        if (id < 0) return;
        writeLong(id, OFF_LAST_TICK, worldTime);
    }

    @Override
    public int getSkipCount(long packedPos) {
        if (!enabled) return 0;
        int id = index.getOrDefault(packedPos, LongToIntIndex.NO_VALUE);
        if (id == LongToIntIndex.NO_VALUE) return 0;
        return readInt(id, OFF_SKIP_COUNT);
    }

    @Override
    public void setSkipCount(long packedPos, int skipCount) {
        if (!enabled) return;
        int id = ensureRecord(packedPos);
        if (id < 0) return;
        writeInt(id, OFF_SKIP_COUNT, skipCount);
    }

    @Override
    public void orFlags(long packedPos, int flags) {
        if (!enabled) return;
        int id = ensureRecord(packedPos);
        if (id < 0) return;
        int cur = readInt(id, OFF_FLAGS);
        writeInt(id, OFF_FLAGS, cur | flags);
    }

    @Override
    public void clearFlags(long packedPos, int flags) {
        if (!enabled) return;
        int id = index.getOrDefault(packedPos, LongToIntIndex.NO_VALUE);
        if (id == LongToIntIndex.NO_VALUE) return;
        int cur = readInt(id, OFF_FLAGS);
        writeInt(id, OFF_FLAGS, cur & ~flags);
    }

    @Override
    public int getFlags(long packedPos) {
        if (!enabled) return 0;
        int id = index.getOrDefault(packedPos, LongToIntIndex.NO_VALUE);
        if (id == LongToIntIndex.NO_VALUE) return 0;
        return readInt(id, OFF_FLAGS);
    }

    @Override
    public void remove(long packedPos) {
        if (!enabled) return;
        int id = index.remove(packedPos);
        if (id == LongToIntIndex.NO_VALUE) return;

        // Tombstone & push to free list.
        int flags = readInt(id, OFF_FLAGS);
        writeInt(id, OFF_FLAGS, flags | BeMetadata.FLAG_TOMBSTONE);
        freeRecord(id);
    }

    @Override
    public BeMetadata snapshot(long packedPos) {
        if (!enabled) return new BeMetadata(packedPos, 0L, 0, 0);
        int id = index.getOrDefault(packedPos, LongToIntIndex.NO_VALUE);
        if (id == LongToIntIndex.NO_VALUE) return new BeMetadata(packedPos, 0L, 0, 0);

        return new BeMetadata(
                readLong(id, OFF_PACKED_POS),
                readLong(id, OFF_LAST_TICK),
                readInt(id, OFF_SKIP_COUNT),
                readInt(id, OFF_FLAGS)
        );
    }

    @Override
    public void close() {
        // DirectByteBuffer will be reclaimed by GC; explicit cleaner is intentionally avoided for safety.
        index.clear();
    }

    private int allocateRecord() {
        // Prefer free list reuse
        if (freeHead != -1) {
            int id = freeHead;
            int next = readInt(id, OFF_NEXT_FREE);
            freeHead = next;
            return id;
        }

        // Otherwise grow linearly
        if (size >= capacityRecords) return -1;
        return size++;
    }

    private void freeRecord(int id) {
        writeInt(id, OFF_NEXT_FREE, freeHead);
        freeHead = id;
    }

    private int baseOffset(int recordId) {
        return recordId * RECORD_BYTES;
    }

    private void writeLong(int recordId, int fieldOff, long v) {
        buf.putLong(baseOffset(recordId) + fieldOff, v);
    }

    private long readLong(int recordId, int fieldOff) {
        return buf.getLong(baseOffset(recordId) + fieldOff);
    }

    private void writeInt(int recordId, int fieldOff, int v) {
        buf.putInt(baseOffset(recordId) + fieldOff, v);
    }

    private int readInt(int recordId, int fieldOff) {
        return buf.getInt(baseOffset(recordId) + fieldOff);
    }
}
