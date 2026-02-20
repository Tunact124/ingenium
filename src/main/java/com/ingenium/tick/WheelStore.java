package com.ingenium.tick;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Storage abstraction for long->Object maps used in remove/update-heavy overflow buckets.
 *
 * <p>Auditor target: Koloboke HashLongObjMap. We use reflection so builds can still run without it,
 * and your Architect can later hard-wire Koloboke (preferred) if desired.
 */
public final class WheelStore {

    /**
     * Creates a long->V map, preferring Koloboke if present.
     *
     * <p>Operations we need: get/put/remove/forEach/clear.
     */
    public static <V> LongObjMap<V> createOverflowMap() {
        // Try Koloboke reflectively:
        // com.koloboke.collect.map.hash.HashLongObjMap
        // com.koloboke.collect.map.hash.HashLongObjMaps#newMutableMap()
        try {
            Class<?> maps = Class.forName("com.koloboke.collect.map.hash.HashLongObjMaps");
            Object kolobokeMap = maps.getMethod("newMutableMap").invoke(null);

            return new LongObjMap<>() {
                final Object m = kolobokeMap;
                final Class<?> c = m.getClass();

                @Override public V get(long k) {
                    try {
                        @SuppressWarnings("unchecked")
                        V v = (V) c.getMethod("get", long.class).invoke(m, k);
                        return v;
                    } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override public V put(long k, V v) {
                    try {
                        @SuppressWarnings("unchecked")
                        V prev = (V) c.getMethod("put", long.class, Object.class).invoke(m, k, v);
                        return prev;
                    } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override public V remove(long k) {
                    try {
                        @SuppressWarnings("unchecked")
                        V prev = (V) c.getMethod("remove", long.class).invoke(m, k);
                        return prev;
                    } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override public void clear() {
                    try {
                        c.getMethod("clear").invoke(m);
                    } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override public void forEach(LongObjConsumer<V> consumer) {
                    // Koloboke has forEach(LongObjConsumer) but type differs; use entrySet iterator reflectively.
                    try {
                        Object es = c.getMethod("entrySet").invoke(m);
                        for (Object e : (Iterable<?>) es) {
                            long key = (long) e.getClass().getMethod("getLongKey").invoke(e);
                            @SuppressWarnings("unchecked")
                            V val = (V) e.getClass().getMethod("getValue").invoke(e);
                            consumer.accept(key, val);
                        }
                    } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        } catch (Throwable ignored) {
            // fallback
        }

        return new JdkLongObjMap<>();
    }

    public interface LongObjConsumer<V> {
        void accept(long key, V value);
    }

    public interface LongObjMap<V> {
        V get(long k);
        V put(long k, V v);
        V remove(long k);
        void clear();
        void forEach(LongObjConsumer<V> consumer);
    }

    /** Fallback: boxed long keys. Slower, but safe if Koloboke absent. */
    static final class JdkLongObjMap<V> implements LongObjMap<V> {
        private final Map<Long, V> m = new HashMap<>(256);

        @Override public V get(long k) { return m.get(k); }
        @Override public V put(long k, V v) { return m.put(k, v); }
        @Override public V remove(long k) { return m.remove(k); }
        @Override public void clear() { m.clear(); }
        @Override public void forEach(LongObjConsumer<V> consumer) {
            for (var e : m.entrySet()) consumer.accept(e.getKey(), e.getValue());
        }
    }
}
