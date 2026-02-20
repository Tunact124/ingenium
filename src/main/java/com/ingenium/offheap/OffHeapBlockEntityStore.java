package com.ingenium.offheap;

import com.ingenium.config.IngeniumConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OffHeapBlockEntityStore
 * - Stores minimal BE tick metadata off-heap to reduce heap graph size / GC scanning.
 * - Java 17: DirectByteBuffer slabs.
 *
 * Layout per entry (24 bytes):
 *   0..7   lastTickedAt (long)
 *   8..11  syncCounter  (int)
 *   12     tierOrdinal  (byte)
 *   13     dirtyFlag    (byte)
 *   16..23 chunkPos     (long)
 *
 * Thread ownership: Server thread only.
 */
public final class OffHeapBlockEntityStore {
    public static final int ENTRY_SIZE = 24;

    private static final int SLAB_ENTRIES = 1024;
    private static final int SLAB_BYTES = SLAB_ENTRIES * ENTRY_SIZE;

    private static final int OFF_LAST_TICK = 0;
    private static final int OFF_SYNC = 8;
    private static final int OFF_TIER = 12;
    private static final int OFF_DIRTY = 13;
    private static final int OFF_CHUNK_POS = 16;

    private static final OffHeapBlockEntityStore INSTANCE = new OffHeapBlockEntityStore();
    public static OffHeapBlockEntityStore get() { return INSTANCE; }

    private final ArrayDeque<Integer> freeSlots = new ArrayDeque<>(256);
    private final ArrayList<ByteBuffer> slabs = new ArrayList<>(8);
    private final ArrayList<long[]> slotIndex = new ArrayList<>(256); // slot -> {slabIdx, offset}

    private int curSlab = -1;
    private int nextInSlab = SLAB_ENTRIES;

    private final AtomicLong allocated = new AtomicLong(0);
    private final AtomicLong freed = new AtomicLong(0);

    private OffHeapBlockEntityStore() {}

    public boolean enabled() {
        return IngeniumConfig.get().offHeapBlockEntityDataEnabled;
    }

    public int allocate() {
        if (!enabled()) return -1;

        Integer recycled = freeSlots.pollFirst();
        if (recycled != null) return recycled;

        if (nextInSlab >= SLAB_ENTRIES) {
            ByteBuffer slab = ByteBuffer.allocateDirect(SLAB_BYTES).order(ByteOrder.nativeOrder());
            slabs.add(slab);
            curSlab = slabs.size() - 1;
            nextInSlab = 0;
        }

        int slotInSlab = nextInSlab++;
        int offset = slotInSlab * ENTRY_SIZE;
        int globalSlot = curSlab * SLAB_ENTRIES + slotInSlab;

        while (slotIndex.size() <= globalSlot) slotIndex.add(null);
        slotIndex.set(globalSlot, new long[]{curSlab, offset});

        zero(globalSlot);
        allocated.incrementAndGet();
        return globalSlot;
    }

    public void free(int slot) {
        if (!enabled()) return;
        if (slot < 0 || slot >= slotIndex.size()) return;
        if (slotIndex.get(slot) == null) return;

        zero(slot);
        freeSlots.addFirst(slot);
        freed.incrementAndGet();
    }

    public long getLastTickedAt(int slot) { return buf(slot).getLong(off(slot) + OFF_LAST_TICK); }
    public void setLastTickedAt(int slot, long t) { buf(slot).putLong(off(slot) + OFF_LAST_TICK, t); }

    public int getSyncCounter(int slot) { return buf(slot).getInt(off(slot) + OFF_SYNC); }
    public void incrementSyncCounter(int slot) {
        int o = off(slot) + OFF_SYNC;
        ByteBuffer b = buf(slot);
        b.putInt(o, b.getInt(o) + 1);
    }

    public byte getTierOrdinal(int slot) { return buf(slot).get(off(slot) + OFF_TIER); }
    public void setTierOrdinal(int slot, byte v) { buf(slot).put(off(slot) + OFF_TIER, v); }

    public boolean isDirty(int slot) { return buf(slot).get(off(slot) + OFF_DIRTY) != 0; }
    public void setDirty(int slot, boolean dirty) { buf(slot).put(off(slot) + OFF_DIRTY, dirty ? (byte)1 : (byte)0); }

    public long getChunkPos(int slot) { return buf(slot).getLong(off(slot) + OFF_CHUNK_POS); }
    public void setChunkPos(int slot, long packed) { buf(slot).putLong(off(slot) + OFF_CHUNK_POS, packed); }

    public long liveCount() { return allocated.get() - freed.get(); }
    public long nativeBytes() { return (long) slabs.size() * SLAB_BYTES; }

    private void zero(int slot) {
        ByteBuffer b = buf(slot);
        int o = off(slot);
        for (int i = 0; i < ENTRY_SIZE; i++) b.put(o + i, (byte) 0);
    }

    private ByteBuffer buf(int slot) {
        long[] info = slotIndex.get(slot);
        return slabs.get((int) info[0]);
    }

    private int off(int slot) {
        return (int) slotIndex.get(slot)[1];
    }
}
