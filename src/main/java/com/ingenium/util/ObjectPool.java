package com.ingenium.util;

import java.util.ArrayDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ObjectPool<T> {
    private final ArrayDeque<T> pool = new ArrayDeque<>(32);
    private final Supplier<T> factory;
    private final Consumer<T> resetter;

    public ObjectPool(Supplier<T> factory, Consumer<T> resetter) {
        this.factory = factory;
        this.resetter = resetter;
    }

    public T acquire() {
        T obj = pool.pollFirst();
        return obj != null ? obj : factory.get();
    }

    public void release(T obj) {
        resetter.accept(obj);
        if (pool.size() < 32) pool.addFirst(obj);  // Cap pool size
    }
}
