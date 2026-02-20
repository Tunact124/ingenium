package com.ingenium.tick;

import com.ingenium.tick.WheelStore.LongObjMap;
import com.ingenium.tick.mpsc.CancelQueue;
import com.ingenium.tick.mpsc.CancelQueues;

import java.util.ArrayList;
import java.util.List;

/**
 * Netty-style hashed timing wheel.
 *
 * <p>Concept:
 * - Time is bucketed: slot = (tickTime / resolution) & mask
 * - Entries beyond one wheel rotation go into an overflow map keyed by "cycle".
 * - Cancellation is MPSC via a queue; consumer thread applies cancels.
 * - Reinsertion caps bound work per tick during drain/advance.
 *
 * <p>This is core logic only (no Minecraft dependencies). A thin adapter should translate
 * Minecraft OrderedTick / BlockPos etc into the payload type.
 */
public final class WheelBackedWorldTickScheduler<T> {

    /** A scheduled entry; payload is immutable-ish; cancellation via handle. */
    public record Entry<T>(long dueTick, T payload, ScheduledTickHandle handle) {}

    @FunctionalInterface
    public interface TickConsumer<T> {
        void accept(T payload);
    }

    private final int wheelSize;
    private final int mask;
    private final int resolution; // ticks per slot
    private final List<Entry<T>>[] buckets;

    private final LongObjMap<List<Entry<T>>> overflowByCycle;
    private final CancelQueue<ScheduledTickHandle> cancelQueue;

    private long currentTick;   // maintained by owner (world time)
    private long currentCycle;  // currentTick / (wheelSize*resolution)

    private final int maxReinsertsPerDrain;
    private final int maxCancelsPerDrain;

    @SuppressWarnings("unchecked")
    public WheelBackedWorldTickScheduler(int wheelSizePowerOfTwo,
                                         int resolutionTicks,
                                         int maxReinsertsPerDrain,
                                         int maxCancelsPerDrain) {
        if (Integer.bitCount(wheelSizePowerOfTwo) != 1) {
            throw new IllegalArgumentException("wheelSize must be power-of-two");
        }
        this.wheelSize = wheelSizePowerOfTwo;
        this.mask = wheelSizePowerOfTwo - 1;
        this.resolution = Math.max(1, resolutionTicks);
        this.maxReinsertsPerDrain = Math.max(0, maxReinsertsPerDrain);
        this.maxCancelsPerDrain = Math.max(0, maxCancelsPerDrain);

        this.buckets = (List<Entry<T>>[]) new List[wheelSizePowerOfTwo];
        for (int i = 0; i < buckets.length; i++) buckets[i] = new ArrayList<>(16);

        this.overflowByCycle = WheelStore.createOverflowMap();
        this.cancelQueue = CancelQueues.create();
    }

    /**
     * Enqueue a cancellation from any thread.
     * The server thread will apply cancellation by skipping entries while draining.
     */
    public void cancelAsync(ScheduledTickHandle handle) {
        cancelQueue.offer(handle);
    }

    /**
     * Schedules payload at dueTick.
     *
     * @return handle for cancellation
     */
    public ScheduledTickHandle schedule(long dueTick, T payload) {
        ScheduledTickHandle h = new ScheduledTickHandle();
        enqueueInternal(new Entry<>(dueTick, payload, h));
        return h;
    }

    /**
     * Advances internal notion of time and drains due work.
     *
     * <p>Call on the server thread once per tick (or per drain cycle).
     *
     * @param nowTick world time (ticks)
     * @param limit max payloads to execute
     * @param consumer executed for each due payload
     * @return number executed
     */
    public int drainDue(long nowTick, int limit, TickConsumer<T> consumer) {
        if (limit <= 0) return 0;

        // Apply cancellation queue (bounded)
        int cancels = 0;
        while (cancels < maxCancelsPerDrain) {
            ScheduledTickHandle h = cancelQueue.poll();
            if (h == null) break;
            // Mark cancelled; entries will be skipped.
            h.cancel();
            cancels++;
        }

        // Advance tick/cycle and reinsert overflow buckets if cycle rolled
        advanceTo(nowTick);

        // Drain current slot
        int slot = slotForTick(nowTick);
        List<Entry<T>> bucket = buckets[slot];

        int executed = 0;
        for (int i = 0; i < bucket.size() && executed < limit; i++) {
            Entry<T> e = bucket.get(i);
            if (e.handle().isCancelled()) continue;
            if (e.dueTick() > nowTick) continue; // resolution effects / late placement

            // Claim execution
            if (!e.handle().expire()) continue; // lost race to cancel/expire

            consumer.accept(e.payload());
            executed++;
        }

        // Compact bucket in-place (no iterator allocation)
        if (!bucket.isEmpty()) {
            int w = 0;
            for (int r = 0; r < bucket.size(); r++) {
                Entry<T> e = bucket.get(r);
                // Keep entries that are not expired AND not due yet (future in same slot), and not cancelled (optional).
                if (!e.handle().isExpired() && !e.handle().isCancelled() && e.dueTick() > nowTick) {
                    bucket.set(w++, e);
                }
            }
            // Clear tail
            for (int k = bucket.size() - 1; k >= w; k--) bucket.remove(k);
        }

        return executed;
    }

    private void enqueueInternal(Entry<T> e) {
        long cycle = cycleForTick(e.dueTick());
        if (cycle == currentCycle) {
            buckets[slotForTick(e.dueTick())].add(e);
        } else {
            // overflow by cycle
            List<Entry<T>> list = overflowByCycle.get(cycle);
            if (list == null) {
                list = new ArrayList<>(16);
                overflowByCycle.put(cycle, list);
            }
            list.add(e);
        }
    }

    private void advanceTo(long nowTick) {
        if (nowTick < currentTick) {
            // World time went backwards (time set). Reset conservatively.
            currentTick = nowTick;
            currentCycle = cycleForTick(nowTick);
            return;
        }

        currentTick = nowTick;
        long newCycle = cycleForTick(nowTick);
        if (newCycle == currentCycle) return;

        // We rolled one or more cycles. Reinsert up to cap to avoid storms.
        long targetCycle = newCycle;
        long c = currentCycle + 1;

        int reinserts = 0;
        while (c <= targetCycle && reinserts < maxReinsertsPerDrain) {
            List<Entry<T>> list = overflowByCycle.remove(c);
            if (list != null) {
                for (int i = 0; i < list.size() && reinserts < maxReinsertsPerDrain; i++) {
                    Entry<T> e = list.get(i);
                    if (e.handle().isCancelled()) continue;
                    // Now within wheel horizon; place into current cycle buckets
                    buckets[slotForTick(e.dueTick())].add(e);
                    reinserts++;
                }
                // if list had leftovers, keep remainder (rare); re-put
                if (reinserts >= maxReinsertsPerDrain && list.size() > 0) {
                    // keep whatever we didn't process
                    List<Entry<T>> remainder = new ArrayList<>(Math.max(1, list.size() - reinserts));
                    for (int i = reinserts; i < list.size(); i++) remainder.add(list.get(i));
                    overflowByCycle.put(c, remainder);
                }
            }
            c++;
        }

        currentCycle = newCycle;
    }

    private int slotForTick(long tick) {
        long t = tick / resolution;
        return (int) (t & mask);
    }

    private long cycleForTick(long tick) {
        long t = tick / resolution;
        long ticksPerCycle = wheelSize;
        return t / ticksPerCycle;
    }
}
