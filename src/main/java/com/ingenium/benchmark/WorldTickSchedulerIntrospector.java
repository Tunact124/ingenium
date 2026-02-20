package com.ingenium.benchmark;

import net.minecraft.world.tick.WorldTickScheduler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection fallback for scheduler internals that are unstable across mappings.
 */
public final class WorldTickSchedulerIntrospector {
    private static final ConcurrentHashMap<Class<?>, MethodHandle> NEXT_ID_GETTERS = new ConcurrentHashMap<>();

    private WorldTickSchedulerIntrospector() {
    }

    public static long tryGetNextId(WorldTickScheduler<?> scheduler) {
        if (scheduler == null) {
            return -1L;
        }

        MethodHandle getter = NEXT_ID_GETTERS.computeIfAbsent(scheduler.getClass(), WorldTickSchedulerIntrospector::findGetter);
        if (getter == null) {
            return -1L;
        }

        try {
            Object value = getter.invoke(scheduler);
            if (value instanceof Long v) {
                return v;
            }
            if (value instanceof Integer v) {
                return v.longValue();
            }
        } catch (Throwable ignored) {
            return -1L;
        }

        return -1L;
    }

    private static MethodHandle findGetter(Class<?> owner) {
        String[] candidates = {"nextId", "nextTickId", "id", "tickId", "counter"};
        for (String candidate : candidates) {
            MethodHandle mh = unreflectGetter(owner, candidate);
            if (mh != null) {
                return mh;
            }
        }

        for (Field field : owner.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getType() != long.class && field.getType() != int.class) {
                continue;
            }
            String name = field.getName().toLowerCase();
            if (name.contains("id") || name.contains("count") || name.contains("counter") || name.contains("next")) {
                MethodHandle mh = unreflectGetter(owner, field.getName());
                if (mh != null) {
                    return mh;
                }
            }
        }

        return null;
    }

    private static MethodHandle unreflectGetter(Class<?> owner, String fieldName) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            return MethodHandles.lookup().unreflectGetter(field);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
