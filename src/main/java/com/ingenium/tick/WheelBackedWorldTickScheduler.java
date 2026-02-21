package com.ingenium.tick;

import com.ingenium.compat.ModDetect;
import com.ingenium.core.Ingenium;
import com.ingenium.ds.TimingWheel;

public final class WheelBackedWorldTickScheduler<T> {
    private static final int DRAIN_LIMIT_PER_TICK = 50_000;

    private final TimingWheel<T> wheel;

    public static <T> WheelBackedWorldTickScheduler<T> createOrNullIfLithiumPresent() {
        if (ModDetect.isLithiumLoaded()) {
            Ingenium.LOGGER.info("[Ingenium] Lithium detected; disabling Ingenium timing wheel (domain owner).");
            return null;
        }
        return new WheelBackedWorldTickScheduler<>(new TimingWheel<>(12));
    }

    private WheelBackedWorldTickScheduler(TimingWheel<T> wheel) {
        this.wheel = wheel;
    }

    public TimingWheel.Handle schedule(long dueTick, T payload) {
        return wheel.schedule(dueTick, payload);
    }

    public int drain(long nowTick, TimingWheel.DrainConsumer<T> consumer) {
        return wheel.drainDue(nowTick, DRAIN_LIMIT_PER_TICK, consumer);
    }
}
