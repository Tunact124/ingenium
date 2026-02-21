package com.ingenium.collections;

import java.util.function.LongFunction;

public interface LongObjectMap<V> {
    V get(long key);
    V put(long key, V value);
    V remove(long key);
    boolean containsKey(long key);
    int size();

    default V computeIfAbsent(long key, LongFunction<? extends V> factory) {
        var existing = get(key);
        if (existing != null) return existing;
        var created = factory.apply(key);
        put(key, created);
        return created;
    }
}
