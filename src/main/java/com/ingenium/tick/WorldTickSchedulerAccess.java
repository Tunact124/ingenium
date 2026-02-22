package com.ingenium.tick;

import net.minecraft.world.ticks.LevelTicks;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

public final class WorldTickSchedulerAccess {
    private static volatile VarHandle ID_HANDLE; // may remain null
    private static volatile boolean INIT;

    private WorldTickSchedulerAccess() {}

    private static void init() {
        if (INIT) return;
        INIT = true;

        // Try a few common/likely field names across Yarn changes.
        // You can add to this list after you inspect the decompiled class locally.
        String[] candidates = {
                "nextId",
                "id",
                "tickId",
                "scheduledTickId",
                "counter"
        };

        for (String name : candidates) {
            try {
                Field f = LevelTicks.class.getDeclaredField(name);
                f.setAccessible(true);
                ID_HANDLE = MethodHandles.privateLookupIn(LevelTicks.class, MethodHandles.lookup())
                        .unreflectVarHandle(f);
                return;
            } catch (Throwable ignored) {
            }
        }

        // If none found, we keep ID_HANDLE = null and callers must handle it.
    }

    /** Returns -1 if not accessible on this version/mappings. */
    public static long tryGetNextId(LevelTicks<?> scheduler) {
        init();
        if (ID_HANDLE == null) return -1L;

        Object v = ID_HANDLE.get(scheduler);
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return (long) i;
        return -1L;
    }

    /** Sets the next ID if accessible. */
    public static void trySetNextId(LevelTicks<?> scheduler, long value) {
        init();
        if (ID_HANDLE == null) return;

        Class<?> type = ID_HANDLE.varType();
        if (type == long.class) {
            ID_HANDLE.set(scheduler, value);
        } else if (type == int.class) {
            ID_HANDLE.set(scheduler, (int) value);
        }
    }
}
