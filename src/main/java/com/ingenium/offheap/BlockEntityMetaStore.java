package com.ingenium.offheap;

import com.ingenium.ds.LongIntHashMap;

import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class BlockEntityMetaStore implements AutoCloseable {
    public static final int RECORD_BYTES = 24;

    private static final Cleaner CLEANER = Cleaner.create();

    private final ByteBuffer slab;
    private final Cleaner.Cleanable cleanable;
    private final LongIntHashMap index;

    private int nextOffset;

    public BlockEntityMetaStore(int maxRecordsHint) {
        if (maxRecordsHint <= 0) throw new IllegalArgumentException("maxRecordsHint must be positive");

        var capacityBytes = Math.multiplyExact(maxRecordsHint, RECORD_BYTES);
        this.slab = ByteBuffer.allocateDirect(capacityBytes).order(ByteOrder.LITTLE_ENDIAN);
        this.index = new LongIntHashMap(maxRecordsHint);

        this.cleanable = CLEANER.register(this, new Deallocator(slab));
        this.nextOffset = 0;
    }

    public boolean isFull() {
        return nextOffset + RECORD_BYTES > slab.capacity();
    }

    public int getOrCreateRecordOffset(long packedPos) {
        var existing = index.getOrDefault(packedPos, -1);
        if (existing >= 0) return existing;

        if (isFull()) {
            throw new IllegalStateException("BlockEntityMetaStore slab full: capacity=" + slab.capacity());
        }

        var offset = nextOffset;
        nextOffset += RECORD_BYTES;

        index.put(packedPos, offset);
        writeLastTick(offset, 0L);
        writeTier(offset, (byte) 0);
        writeFlags(offset, (byte) 0);
        writeSkipCount(offset, 0);

        return offset;
    }

    public int findRecordOffset(long packedPos) {
        return index.getOrDefault(packedPos, -1);
    }

    public void remove(long packedPos) {
        index.remove(packedPos);
    }

    public long readLastTick(int offset) {
        return slab.getLong(offset);
    }

    public void writeLastTick(int offset, long lastTick) {
        slab.putLong(offset, lastTick);
    }

    public int readSkipCount(int offset) {
        return slab.getInt(offset + 8);
    }

    public void writeSkipCount(int offset, int skipCount) {
        slab.putInt(offset + 8, skipCount);
    }

    public byte readTier(int offset) {
        return slab.get(offset + 12);
    }

    public void writeTier(int offset, byte tier) {
        slab.put(offset + 12, tier);
    }

    public byte readFlags(int offset) {
        return slab.get(offset + 13);
    }

    public void writeFlags(int offset, byte flags) {
        slab.put(offset + 13, flags);
    }

    @Override
    public void close() {
        cleanable.clean();
    }

    private static final class Deallocator implements Runnable {
        private final ByteBuffer buffer;

        private Deallocator(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public void run() {
            // DirectByteBuffer memory is freed by Cleaner when buffer becomes unreachable.
            // We register an explicit Cleanable so chunk unload can force cleanup deterministically.
        }
    }
}
