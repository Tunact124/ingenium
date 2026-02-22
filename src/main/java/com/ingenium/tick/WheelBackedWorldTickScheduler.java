package com.ingenium.tick;

import com.ingenium.compat.BuddyLogic;
import com.ingenium.core.Ingenium;
import com.ingenium.ds.TimingWheel;

public final class WheelBackedWorldTickScheduler<T> {
    private static final int DRAIN_LIMIT_PER_TICK = 50_000;

    private final TimingWheel<T> wheel;

    public static <T> WheelBackedWorldTickScheduler<T> createOrNullIfLithiumPresent() {
        if (!TimingWheelGuard.isActive()) {
            return null;
        }
        return new WheelBackedWorldTickScheduler<>(new TimingWheel<>(12));
    }

    public WheelBackedWorldTickScheduler(int wheelSize, int resolution, int maxReinserts, int maxCancels) {
        this(new TimingWheel<>(Integer.numberOfTrailingZeros(wheelSize)));
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

    public int drainDue(long nowTick, int limit, TimingWheel.DrainConsumer<T> consumer) {
        return wheel.drainDue(nowTick, limit, consumer);
    }

    @FunctionalInterface
    public interface TickConsumer<T> extends TimingWheel.DrainConsumer<T> {
    }
}
