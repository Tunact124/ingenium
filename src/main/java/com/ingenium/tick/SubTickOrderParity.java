package com.ingenium.tick;

import com.ingenium.mixin.access.WorldTickSchedulerAccess;
import net.minecraft.world.ticks.LevelTicks;

/**
 * Server-thread only.
 * Allocates subTickOrder IDs by consuming vanilla WorldTickScheduler.nextId.
 *
 * This guarantees parity with vanilla ordering when multiple ticks share the same triggerTick.
 */
public final class SubTickOrderParity {

    public static long next(LevelTicks<?> scheduler) {
        long id = WorldTickSchedulerAccess.tryGetNextId(scheduler);
        if (id != -1L) {
            WorldTickSchedulerAccess.trySetNextId(scheduler, id + 1L);
        }
        return id;
    }

    private SubTickOrderParity() {}
}
