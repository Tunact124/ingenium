package com.ingenium.metrics;

/**
 * Fixed-size ring buffer for longs.
 * - No allocation after construction
 * - Overwrites old entries
 * - Maintains a running count
 */
public final class LongRingBuffer {
    private final long[] data;
    private final int mask; // capacity must be power-of-two
    private int writeIndex;
    private int size;

    public LongRingBuffer(int capacityPow2) {
        if (capacityPow2 <= 0 || (capacityPow2 & (capacityPow2 - 1)) != 0) {
            throw new IllegalArgumentException("capacity must be power-of-two");
        }
        this.data = new long[capacityPow2];
        this.mask = capacityPow2 - 1;
    }

    public void add(long value) {
        data[writeIndex & mask] = value;
        writeIndex++;
        if (size < data.length) size++;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return data.length;
    }

    public long getNewest() {
        if (size == 0) return 0L;
        return data[(writeIndex - 1) & mask];
    }

    public long getAtNewestOffset(int offset) {
        // offset=0 => newest, offset=1 => previous, etc.
        if (offset < 0 || offset >= size) return 0L;
        return data[(writeIndex - 1 - offset) & mask];
    }

    public void clear() {
        // Do not Arrays.fill() in hot code; allow stale values; reset indices.
        writeIndex = 0;
        size = 0;
    }
}
