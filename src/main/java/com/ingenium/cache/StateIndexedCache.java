package com.ingenium.cache;

import com.ingenium.compat.BuddyLogic;
import com.ingenium.compat.BuddyLogic.KnownMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A per-blockstate cache that automatically selects the fastest
 * storage backend based on whether FerriteCore is present.
 */
public final class StateIndexedCache<V> {

    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/StateCache");

    private interface CacheBackend<V> {
        V get(Object blockState);
        void put(Object blockState, V value);
        V computeIfAbsent(Object blockState, Function<Object, V> mapper);
        void clear();
        int size();
    }

    private final CacheBackend<V> backend;
    private final String name;

    public StateIndexedCache(String name, int expectedSize) {
        this.name = name;

        boolean ferriteCoreActive = BuddyLogic.isPresent(KnownMod.FERRITECORE)
            && BuddyLogic.getResult(KnownMod.FERRITECORE).hasCapability("fastmap");

        if (ferriteCoreActive) {
            this.backend = new FerriteCoreFastBackend<>();
            BuddyLogic.logEnhance("StateIndexedCache:" + name, "ferritecore", "Using stateIndex array access");
        } else {
            this.backend = new HashMapBackend<>(expectedSize);
            LOG.debug("[Ingenium] StateIndexedCache:{} using HashMap fallback", name);
        }
    }

    @SuppressWarnings("unchecked")
    public V get(Object blockState) {
        return backend.get(blockState);
    }

    public void put(Object blockState, V value) {
        backend.put(blockState, value);
    }

    public V computeIfAbsent(Object blockState, Function<Object, V> mapper) {
        return backend.computeIfAbsent(blockState, mapper);
    }

    public void clear() {
        backend.clear();
    }

    public int size() {
        return backend.size();
    }

    private static final class FerriteCoreFastBackend<V> implements CacheBackend<V> {
        private static final java.lang.reflect.Method GET_STATE_INDEX;
        private static final int DEFAULT_ARRAY_SIZE = 16384;

        static {
            java.lang.reflect.Method method = null;
            try {
                Class<?> holderInterface = Class.forName("malte0811.ferritecore.fastmap.FastMapStateHolder");
                method = holderInterface.getMethod("getStateIndex");
                method.setAccessible(true);
            } catch (Exception e) {
                LOG.error("[Ingenium] Failed to resolve FastMapStateHolder.getStateIndex()", e);
            }
            GET_STATE_INDEX = method;
        }

        private Object[] values;
        private int count;

        FerriteCoreFastBackend() {
            this.values = new Object[DEFAULT_ARRAY_SIZE];
            this.count = 0;
        }

        private int getIndex(Object blockState) {
            if (GET_STATE_INDEX == null) return -1;
            try {
                return (int) GET_STATE_INDEX.invoke(blockState);
            } catch (Exception e) {
                return -1;
            }
        }

        private void ensureCapacity(int index) {
            if (index >= values.length) {
                int newSize = Math.max(values.length * 2, index + 1);
                Object[] newValues = new Object[newSize];
                System.arraycopy(values, 0, newValues, 0, values.length);
                values = newValues;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public V get(Object blockState) {
            int index = getIndex(blockState);
            if (index < 0 || index >= values.length) return null;
            return (V) values[index];
        }

        @Override
        public void put(Object blockState, V value) {
            int index = getIndex(blockState);
            if (index < 0) return;
            ensureCapacity(index);
            if (values[index] == null && value != null) count++;
            if (values[index] != null && value == null) count--;
            values[index] = value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V computeIfAbsent(Object blockState, Function<Object, V> mapper) {
            int index = getIndex(blockState);
            if (index < 0) return mapper.apply(blockState);
            ensureCapacity(index);
            V existing = (V) values[index];
            if (existing != null) return existing;
            V computed = mapper.apply(blockState);
            values[index] = computed;
            if (computed != null) count++;
            return computed;
        }

        @Override
        public void clear() {
            java.util.Arrays.fill(values, null);
            count = 0;
        }

        @Override
        public int size() {
            return count;
        }
    }

    private static final class HashMapBackend<V> implements CacheBackend<V> {
        private final Map<Object, V> map;
        HashMapBackend(int expectedSize) {
            this.map = new HashMap<>(expectedSize, 0.75f);
        }
        @Override public V get(Object blockState) { return map.get(blockState); }
        @Override public void put(Object blockState, V value) { map.put(blockState, value); }
        @Override public V computeIfAbsent(Object blockState, Function<Object, V> mapper) {
            return map.computeIfAbsent(blockState, mapper);
        }
        @Override public void clear() { map.clear(); }
        @Override public int size() { return map.size(); }
    }
}
