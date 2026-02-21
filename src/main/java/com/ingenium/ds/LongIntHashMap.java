package com.ingenium.ds;

import java.util.Arrays;

public final class LongIntHashMap {
    private static final byte STATE_EMPTY = 0;
    private static final byte STATE_FULL = 1;
    private static final byte STATE_TOMBSTONE = 2;

    private static final float MAX_LOAD_FACTOR = 0.60f;

    private long[] keys;
    private int[] values;
    private byte[] states;

    private int size;
    private int resizeThreshold;
    private int mask;

    public LongIntHashMap(int expectedSize) {
        var capacity = tableSizeFor((int) Math.ceil(expectedSize / MAX_LOAD_FACTOR));
        this.keys = new long[capacity];
        this.values = new int[capacity];
        this.states = new byte[capacity];
        this.mask = capacity - 1;
        this.resizeThreshold = (int) (capacity * MAX_LOAD_FACTOR);
    }

    public int size() {
        return size;
    }

    public int getOrDefault(long key, int defaultValue) {
        var index = findSlot(key);
        return index >= 0 ? values[index] : defaultValue;
    }

    public boolean containsKey(long key) {
        return findSlot(key) >= 0;
    }

    public int put(long key, int value) {
        if (size >= resizeThreshold) {
            rehash(keys.length << 1);
        }

        var slot = probeForInsert(key);
        if (states[slot] == STATE_FULL) {
            var previous = values[slot];
            values[slot] = value;
            return previous;
        }

        keys[slot] = key;
        values[slot] = value;
        states[slot] = STATE_FULL;
        size++;
        return 0;
    }

    public int remove(long key) {
        var index = findSlot(key);
        if (index < 0) return 0;

        states[index] = STATE_TOMBSTONE;
        size--;
        return values[index];
    }

    public void clear() {
        Arrays.fill(states, STATE_EMPTY);
        size = 0;
    }

    private int findSlot(long key) {
        var slot = mix64(key) & mask;
        while (true) {
            var state = states[slot];
            if (state == STATE_EMPTY) return -1;
            if (state == STATE_FULL && keys[slot] == key) return slot;
            slot = (slot + 1) & mask;
        }
    }

    private int probeForInsert(long key) {
        var slot = mix64(key) & mask;
        var firstTombstone = -1;

        while (true) {
            var state = states[slot];
            if (state == STATE_EMPTY) {
                return firstTombstone >= 0 ? firstTombstone : slot;
            }
            if (state == STATE_TOMBSTONE) {
                if (firstTombstone < 0) firstTombstone = slot;
            } else if (keys[slot] == key) {
                return slot;
            }
            slot = (slot + 1) & mask;
        }
    }

    private void rehash(int newCapacity) {
        var capacity = tableSizeFor(newCapacity);

        var oldKeys = keys;
        var oldValues = values;
        var oldStates = states;

        keys = new long[capacity];
        values = new int[capacity];
        states = new byte[capacity];
        mask = capacity - 1;
        resizeThreshold = (int) (capacity * MAX_LOAD_FACTOR);

        var oldSize = size;
        size = 0;

        for (var i = 0; i < oldKeys.length; i++) {
            if (oldStates[i] != STATE_FULL) continue;
            put(oldKeys[i], oldValues[i]);
        }

        if (size != oldSize) {
            throw new IllegalStateException("LongIntHashMap rehash size mismatch: " + size + " != " + oldSize);
        }
    }

    private static int tableSizeFor(int capacity) {
        var n = Math.max(8, capacity);
        n--;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n + 1);
    }

    private static int mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        z = z ^ (z >>> 33);
        return (int) z;
    }
}
