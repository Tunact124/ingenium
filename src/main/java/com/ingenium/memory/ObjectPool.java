package com.ingenium.memory;

import com.ingenium.config.IngeniumConfig;

import java.util.ArrayDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ObjectPool<T> {
 
    private final ThreadLocal<ArrayDeque<T>> pool =
        ThreadLocal.withInitial(ArrayDeque::new);
    private final Supplier<T>  factory;
    private final Consumer<T>  resetFn;
    private final int          maxCapacity; // per-thread cap
 
    public ObjectPool(Supplier<T> factory, Consumer<T> resetFn, int maxCapacity) {
        this.factory     = factory;
        this.resetFn     = resetFn;
        this.maxCapacity = maxCapacity;
    }
 
    /** Acquire. Always non-null. Zero allocation if pool has an entry. */
    public T acquire() {
        if (!IngeniumConfig.get().enableObjectPooling) return factory.get();
        T obj = pool.get().pollLast();
        return obj != null ? obj : factory.get();
    }
 
    /** Release. MUST be called in finally block or try-with-resources. */
    public void release(T obj) {
        if (!IngeniumConfig.get().enableObjectPooling) return;
        resetFn.accept(obj);
        ArrayDeque<T> q = pool.get();
        if (q.size() < maxCapacity) q.addLast(obj);
        // Over-cap objects are discarded — prevents unbounded pool growth.
    }
 
    /** Acquire as AutoCloseable — enables try-with-resources syntax. */
    public PooledObject<T> acquireAuto() {
        return new PooledObject<>(acquire(), this);
    }
 
    public record PooledObject<T>(T obj, ObjectPool<T> pool)
            implements AutoCloseable {
        @Override public void close() { pool.release(obj); }
    }
}
