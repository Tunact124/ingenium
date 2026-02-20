package com.ingenium.ds;

import java.util.Arrays;

/**
 * LongObjHashMap:
 * - open addressing
 * - no boxing
 * - no iterator allocation
 *
 * Thread-safety: NOT thread-safe. Server-thread ownership or snapshot before sharing.
 */
public final class LongObjHashMap<V> {
    private static final long EMPTY = Long.MIN_VALUE;

    private long[] keys;
    private Object[] values;
    private int size;
    private int mask;
    private int growThreshold;

    public LongObjHashMap(int expectedCapacity) {
        int cap = nextPow2((int) (expectedCapacity / 0.65f) + 1);
        allocate(cap);
    }

    @SuppressWarnings("unchecked")
    public V get(long key) {
        int slot = slot(key);
        for (int i = 0; i < keys.length; i++) {
            int idx = (slot + i) & mask;
            long k = keys[idx];
            if (k == key) return (V) values[idx];
            if (k == EMPTY) return null;
        }
        return null;
    }

    public void put(long key, V value) {
        if (size >= growThreshold) grow();

        int slot = slot(key);
        for (int i = 0; i < keys.length; i++) {
            int idx = (slot + i) & mask;
            long k = keys[idx];
            if (k == EMPTY || k == key) {
                if (k == EMPTY) size++;
                keys[idx] = key;
                values[idx] = value;
                return;
            }
        }
        grow();
        put(key, value);
    }

    @SuppressWarnings("unchecked")
    public V remove(long key) {
        int slot = slot(key);
        for (int i = 0; i < keys.length; i++) {
            int idx = (slot + i) & mask;
            long k = keys[idx];
            if (k == EMPTY) return null;
            if (k == key) {
                V prev = (V) values[idx];
                backshiftDelete(idx);
                size--;
                return prev;
            }
        }
        return null;
    }

    public int size() { return size; }

    public void clear() {
        Arrays.fill(keys, EMPTY);
        Arrays.fill(values, null);
        size = 0;
    }

    private int slot(long key) {
        long h = key ^ (key >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (int) (h & mask);
    }

    private void grow() {
        long[] oldK = keys;
        Object[] oldV = values;

        allocate(keys.length << 1);

        for (int i = 0; i < oldK.length; i++) {
            long k = oldK[i];
            if (k != EMPTY) {
                //noinspection unchecked
                put(k, (V) oldV[i]);
            }
        }
    }

    private void allocate(int cap) {
        keys = new long[cap];
        values = new Object[cap];
        Arrays.fill(keys, EMPTY);
        mask = cap - 1;
        growThreshold = (int) (cap * 0.65f);
        size = 0;
    }

    private void backshiftDelete(int deleteIdx) {
        int cur = deleteIdx;
        while (true) {
            int next = (cur + 1) & mask;
            long nextKey = keys[next];
            if (nextKey == EMPTY) break;

            int home = slot(nextKey);
            if (inCyclicRange(home, cur, next)) {
                keys[cur] = keys[next];
                values[cur] = values[next];
                cur = next;
            } else {
                break;
            }
        }
        keys[cur] = EMPTY;
        values[cur] = null;
    }

    private boolean inCyclicRange(int slot, int start, int end) {
        if (start <= end) return slot >= start && slot < end;
        return slot >= start || slot < end;
    }

    private static int nextPow2(int n) {
        int p = 1;
        while (p < n) p <<= 1;
        return Math.max(p, 16);
    }
}
