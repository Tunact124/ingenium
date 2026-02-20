package com.ingenium.command;

import com.ingenium.tick.WheelStore;
import net.minecraft.server.world.ServerWorld;

/**
 * A tiny “bridge” so command code can locate the per-world WheelStore without reflection.
 */
public final class IngeniumWheelLookup {
    private static final java.util.WeakHashMap<ServerWorld, WheelStore> MAP = new java.util.WeakHashMap<>();

    public static void put(ServerWorld world, WheelStore store) {
        MAP.put(world, store);
    }

    public static WheelStore get(ServerWorld world) {
        return MAP.get(world);
    }

    private IngeniumWheelLookup() {}
}
