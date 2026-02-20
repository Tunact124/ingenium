package com.ingenium.offheap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

/**
 * A minimal abstraction for a hot long->int index.
 *
 * <p>Prefers Koloboke (no boxing) if present; falls back to a boxed HashMap otherwise.
 * The Architect can replace the fallback with fastutil if desired.
 */
public interface LongToIntIndex {
    int NO_VALUE = -1;

    int getOrDefault(long key, int defaultValue);

    int put(long key, int value);

    int remove(long key);

    void clear();

    int size();

    static LongToIntIndex createPreferKoloboke() {
        // Try Koloboke at runtime without hard linking.
        // Expected type: com.koloboke.collect.map.hash.HashLongIntMap (or similar).
        try {
            // Koloboke factory often: com.koloboke.collect.map.hash.HashLongIntMaps.newMutableMap()
            final Class<?> maps = Class.forName("com.koloboke.collect.map.hash.HashLongIntMaps");
            final var mh = MethodHandles.publicLookup().findStatic(
                    maps,
                    "newMutableMap",
                    MethodType.methodType(Class.forName("com.koloboke.collect.map.hash.MutableHashLongIntMap"))
            );
            final Object kolobokeMap = mh.invoke();

            return new KolobokeLongIntIndex(kolobokeMap);
        } catch (Throwable ignored) {
            return new JdkBoxedLongIntIndex();
        }
    }

    /**
     * Koloboke wrapper using reflection handles to avoid compile-time dependency.
     */
    final class KolobokeLongIntIndex implements LongToIntIndex {
        private final Object map;
        private final java.lang.invoke.MethodHandle getOrDefault;
        private final java.lang.invoke.MethodHandle put;
        private final java.lang.invoke.MethodHandle remove;
        private final java.lang.invoke.MethodHandle clear;
        private final java.lang.invoke.MethodHandle size;

        KolobokeLongIntIndex(Object map) throws Throwable {
            this.map = map;
            var lookup = MethodHandles.publicLookup();
            // We intentionally resolve methods off runtime class to match the concrete impl.
            Class<?> cls = map.getClass();

            this.getOrDefault = lookup.findVirtual(cls, "getOrDefault",
                    MethodType.methodType(int.class, long.class, int.class));
            this.put = lookup.findVirtual(cls, "put",
                    MethodType.methodType(int.class, long.class, int.class));
            this.remove = lookup.findVirtual(cls, "removeAsInt",
                    MethodType.methodType(int.class, long.class));
            this.clear = lookup.findVirtual(cls, "clear",
                    MethodType.methodType(void.class));
            this.size = lookup.findVirtual(cls, "size",
                    MethodType.methodType(int.class));
        }

        @Override
        public int getOrDefault(long key, int defaultValue) {
            try {
                return (int) getOrDefault.invoke(map, key, defaultValue);
            } catch (Throwable t) {
                return defaultValue;
            }
        }

        @Override
        public int put(long key, int value) {
            try {
                return (int) put.invoke(map, key, value);
            } catch (Throwable t) {
                return NO_VALUE;
            }
        }

        @Override
        public int remove(long key) {
            try {
                return (int) remove.invoke(map, key);
            } catch (Throwable t) {
                return NO_VALUE;
            }
        }

        @Override
        public void clear() {
            try {
                clear.invoke(map);
            } catch (Throwable ignored) { }
        }

        @Override
        public int size() {
            try {
                return (int) size.invoke(map);
            } catch (Throwable t) {
                return 0;
            }
        }
    }

    /**
     * Boxed fallback. Not ideal for hot paths, but safe and dependency-free.
     */
    final class JdkBoxedLongIntIndex implements LongToIntIndex {
        private final Map<Long, Integer> map = new HashMap<>();

        @Override
        public int getOrDefault(long key, int defaultValue) {
            return map.getOrDefault(key, defaultValue);
        }

        @Override
        public int put(long key, int value) {
            Integer prev = map.put(key, value);
            return prev == null ? NO_VALUE : prev;
        }

        @Override
        public int remove(long key) {
            Integer prev = map.remove(key);
            return prev == null ? NO_VALUE : prev;
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public int size() {
            return map.size();
        }
    }
}
