package com.ingenium.command;

import com.ingenium.tick.WheelStore;
import net.minecraft.server.level.ServerLevel;

/**
 * A tiny “bridge” so command code can locate the per-world WheelStore without reflection.
 */
public final class IngeniumWheelLookup {
    private static final java.util.WeakHashMap<ServerLevel, WheelStore> MAP = new java.util.WeakHashMap<>();

    public static void put(ServerLevel world, WheelStore store) {
        MAP.put(world, store);
    }

    public static WheelStore get(ServerLevel world) {
        return MAP.get(world);
    }

    private IngeniumWheelLookup() {}
}
