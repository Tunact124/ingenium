package com.ingenium.collections;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class FastutilLongObjectMap<V> implements LongObjectMap<V> {
    private final Long2ObjectOpenHashMap<V> delegate;

    public FastutilLongObjectMap(int expectedSize) {
        this.delegate = new Long2ObjectOpenHashMap<>(expectedSize, 0.75f);
    }

    @Override
    public V get(long key) {
        return delegate.get(key);
    }

    @Override
    public V put(long key, V value) {
        return delegate.put(key, value);
    }

    @Override
    public V remove(long key) {
        return delegate.remove(key);
    }

    @Override
    public boolean containsKey(long key) {
        return delegate.containsKey(key);
    }

    @Override
    public int size() {
        return delegate.size();
    }
}
