package com.ingenium.tick;

import com.ingenium.core.IngeniumGovernor;
import com.ingenium.core.IngeniumGovernor.Subsystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.TickPriority;

import java.util.Objects;

/**
 * WheelBackedWorldTickScheduler
 *
 * Full replacement backing store for scheduled ticks (block + fluid).
 *
 * Key properties:
 * - Stores OrderedTick<T> directly (no wrappers).
 * - Zero-allocation drain: no sorting during drain; buckets are kept ordered on insert.
 * - Overflow handled by cycle-key bucketing: cycleKey = triggerTick >>> WHEEL_BITS.
 *
 * Thread ownership:
 * - Designed for Server Thread ownership (vanilla scheduleTick paths are server-thread).
 * - If some mod schedules off-thread, you MUST gate via your own main-thread dispatch.
 */
public final class WheelBackedWorldTickScheduler<T> {

    // Wheel size must be power-of-two.
    private final int bucketCount;
    private final int bucketMask;
    private final int wheelBits;

    // Buckets for current cycle: [0..bucketCount-1]
    private final ObjectArrayList<OrderedTick<T>>[] buckets;

    // Immediate bucket for delay 0/1 (still ordered on insert; drained first)
    private final ObjectArrayList<OrderedTick<T>> immediate;

    /**
     * Overflow: cycleKey -> list of ticks that belong to that cycle.
     * cycleKey = triggerTick >>> wheelBits
     *
     * Not on hot path except when scheduling very long delays or when cycle advances.
     * We still keep it allocation-light via ObjectArrayList.
     */
    private final LongObjMap<ObjectArrayList<OrderedTick<T>>> overflowByCycle;

    // World time tracking
    private long currentWorldTick = 0;
    private long currentCycleKey  = 0;

    // State machine for offboarding
    private volatile boolean wheelEnabled = true;
    private volatile boolean offboardRequested = false;

    // Diagnostics
    private long scheduled = 0;
    private long drained   = 0;
    private long overflowed = 0;

    @SuppressWarnings("unchecked")
    public WheelBackedWorldTickScheduler(int bucketCount) {
        int pow2 = Integer.highestOneBit(bucketCount);
        if (pow2 < bucketCount) pow2 <<= 1;
        if (pow2 < 32) pow2 = 32;

        this.bucketCount = pow2;
        this.bucketMask  = pow2 - 1;
        this.wheelBits   = 31 - Integer.numberOfLeadingZeros(pow2);

        this.buckets = (ObjectArrayList<OrderedTick<T>>[]) new ObjectArrayList<?>[pow2];
        for (int i = 0; i < pow2; i++) this.buckets[i] = new ObjectArrayList<>(8);

        this.immediate = new ObjectArrayList<>(64);
        this.overflowByCycle = new LongObjMap<>(64);
    }

    // ------------------------------------------------------------
    // State
    // ------------------------------------------------------------

    public boolean isWheelEnabled() {
        return wheelEnabled;
    }

    public boolean isOffboardRequested() {
        return offboardRequested;
    }

    /** Request offboarding; actual persistence happens on next save unless admin forces a save. */
    public void requestOffboard() {
        this.offboardRequested = true;
        this.wheelEnabled = false; // stop routing new schedules into wheel immediately
    }

    // ------------------------------------------------------------
    // Scheduling (Server Thread)
    // ------------------------------------------------------------

    /**
     * Schedule a tick directly into the wheel store.
     *
     * IMPORTANT: tick.subTickOrder MUST already be parity-assigned using vanilla WorldTickScheduler.nextId.
     */
    public void schedule(OrderedTick<T> tick, long worldTimeNow) {
        Objects.requireNonNull(tick, "tick");
        scheduled++;

        if (!wheelEnabled) {
            // Caller should route to vanilla scheduler when disabled.
            // We do nothing here intentionally.
            return;
        }

        // Ensure internal time is aligned (safe even if caller calls out-of-order)
        alignWorldTime(worldTimeNow);

        final long trigger = tick.triggerTick();
        final long delay = trigger - currentWorldTick;

        if (delay <= 1) {
            // immediate list, ordered on insert by vanilla comparator
            orderedInsert(immediate, tick);
            return;
        }

        final long cycleKey = trigger >>> wheelBits;
        if (cycleKey == currentCycleKey) {
            final int slot = (int) (trigger & bucketMask);
            orderedInsert(buckets[slot], tick);
        } else {
            overflowed++;
            ObjectArrayList<OrderedTick<T>> list = overflowByCycle.get(cycleKey);
            if (list == null) {
                list = new ObjectArrayList<>(8);
                overflowByCycle.put(cycleKey, list);
            }
            // Keep per-cycle list ordered as well to preserve parity on reinsertion.
            orderedInsert(list, tick);
        }
    }

    // ------------------------------------------------------------
    // Drain (Server Thread) — zero allocation
    // ------------------------------------------------------------

    /**
     * Drain due ticks for this world tick.
     *
     * Zero-allocation:
     * - No sorting here. All lists are maintained ordered on insert.
     * - No iterators.
     *
     * @param worldTimeNow world.getTime()
     * @param limit maximum ticks to execute this call (budget gate).
     * @param consumer tick executor
     * @return number executed
     */
    public int drainDue(long worldTimeNow, int limit, TickConsumer<T> consumer) {
        if (limit <= 0) return 0;
        alignWorldTime(worldTimeNow);

        // Advance cycle if needed and reinsert overflow for the now-active cycle.
        advanceCycleIfNeeded();

        int executed = 0;

        // 1) Drain immediate (delay 0/1)
        if (!immediate.isEmpty()) {
            executed += drainList(immediate, limit - executed, worldTimeNow, consumer);
            if (executed >= limit) return executed;
        }

        // 2) Drain current bucket
        final int slot = (int) (currentWorldTick & bucketMask);
        final ObjectArrayList<OrderedTick<T>> bucket = buckets[slot];
        if (!bucket.isEmpty()) {
            executed += drainList(bucket, limit - executed, worldTimeNow, consumer);
        }

        return executed;
    }

    private int drainList(ObjectArrayList<OrderedTick<T>> list,
                          int limit,
                          long worldTimeNow,
                          TickConsumer<T> consumer) {

        int executed = 0;

        // Lists are ordered by vanilla comparator. Due ticks are at the front.
        // We scan until we find a not-due tick; then stop.
        for (int i = 0, n = list.size(); i < n && executed < limit; i++) {
            OrderedTick<T> t = list.get(i);
            if (t.triggerTick() > worldTimeNow) break;

            consumer.accept(t.pos(), t.type(), t.priority());
            drained++;
            executed++;
        }

        if (executed == 0) return 0;

        // Remove the executed prefix with minimal overhead:
        // shift tail left (ObjectArrayList exposes elements array only via internals; use removeElements).
        list.removeElements(0, executed);

        return executed;
    }

    // ------------------------------------------------------------
    // Offboarding / flush (Server Thread)
    // ------------------------------------------------------------

    /**
     * Flush ALL ticks still owned by the wheel into the provided "vanilla sink".
     * This is zero-allocation on traversal; the sink decides how to insert.
     *
     * IMPORTANT:
     * - This flush does NOT force-save; persistence happens on next world save,
     *   unless admin uses --force to trigger an immediate save.
     */
    public void flushAllInto(VanillaSink<T> sink, long worldTimeNow) {
        alignWorldTime(worldTimeNow);
        advanceCycleIfNeeded();

        // Immediate
        flushList(immediate, sink);

        // Buckets
        for (int i = 0; i < buckets.length; i++) {
            flushList(buckets[i], sink);
        }

        // Overflow by cycle
        overflowByCycle.forEach((cycleKey, list) -> flushList(list, sink));
        overflowByCycle.clear();
    }

    private void flushList(ObjectArrayList<OrderedTick<T>> list, VanillaSink<T> sink) {
        if (list.isEmpty()) return;

        for (int i = 0, n = list.size(); i < n; i++) {
            sink.accept(list.get(i));
        }
        list.clear();
    }

    // ------------------------------------------------------------
    // Internal: time + cycle handling
    // ------------------------------------------------------------

    private void alignWorldTime(long worldTimeNow) {
        if (worldTimeNow < currentWorldTick) {
            // Inconsistency: world time went backwards (shouldn't happen in vanilla).
            // Request offboard for safety; do not allocate.
            requestOffboard();
            // Clamp to avoid negative delay math:
            currentWorldTick = worldTimeNow;
        } else {
            currentWorldTick = worldTimeNow;
        }
    }

    private void advanceCycleIfNeeded() {
        long newCycleKey = currentWorldTick >>> wheelBits;
        if (newCycleKey <= currentCycleKey) return;

        // Catch up to current cycle by pulling all intermediate cycles from overflow.
        while (currentCycleKey < newCycleKey) {
            currentCycleKey++;
            ObjectArrayList<OrderedTick<T>> dueCycle = overflowByCycle.remove(currentCycleKey);
            if (dueCycle != null && !dueCycle.isEmpty()) {
                for (int i = 0, n = dueCycle.size(); i < n; i++) {
                    OrderedTick<T> t = dueCycle.get(i);
                    int slot = (int) (t.triggerTick() & bucketMask);
                    orderedInsert(buckets[slot], t);
                }
                dueCycle.clear();
            }
        }
    }

    /**
     * Ordered insertion by vanilla comparator with no allocation.
     *
     * This makes drain zero-allocation (no sort).
     */
    private void orderedInsert(ObjectArrayList<OrderedTick<T>> list, OrderedTick<T> tick) {
        final int size = list.size();
        if (size == 0) {
            list.add(tick);
            return;
        }

        // Fast path append if it belongs at end.
        if (compareOrderedTick(list.get(size - 1), tick) <= 0) {
            list.add(tick);
            return;
        }

        // Binary search insertion index.
        int lo = 0, hi = size - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            OrderedTick<T> m = list.get(mid);
            int cmp = compareOrderedTick(m, tick);
            if (cmp <= 0) lo = mid + 1;
            else hi = mid - 1;
        }
        list.add(lo, tick);
    }

    /**
     * Vanilla parity comparator:
     * OrderedTick compares by:
     * - triggerTick
     * - priority
     * - subTickOrder
     *
     * MAPPING CHECK: if OrderedTick implements Comparable in your Yarn, you can use tick.compareTo().
     * This explicit comparator avoids method refs/lambdas.
     */
    private static <T> int compareOrderedTick(OrderedTick<T> a, OrderedTick<T> b) {
        int cmp = Long.compare(a.triggerTick(), b.triggerTick());
        if (cmp != 0) return cmp;

        // TickPriority ordering: vanilla uses enum ordinal ordering (higher priority first or last depending).
        // In 1.20.1 Yarn: TickPriority has compareTo with desired semantics.
        cmp = a.priority().compareTo(b.priority());
        if (cmp != 0) return cmp;

        return Long.compare(a.subTickOrder(), b.subTickOrder());
    }

    // ------------------------------------------------------------
    // Interfaces
    // ------------------------------------------------------------

    @FunctionalInterface
    public interface VanillaSink<T> {
        void accept(OrderedTick<T> tick);
    }

    @FunctionalInterface
    public interface TickConsumer<T> {
        void accept(BlockPos pos, T type, TickPriority priority);
    }

    // ------------------------------------------------------------
    // Diagnostics
    // ------------------------------------------------------------

    public long getScheduled() { return scheduled; }
    public long getDrained() { return drained; }
    public long getOverflowed() { return overflowed; }

    // ------------------------------------------------------------
    // Minimal long->object map (no boxing, not thread-safe; server-thread owned)
    // ------------------------------------------------------------

    static final class LongObjMap<V> {
        private static final long EMPTY = Long.MIN_VALUE;

        private long[] keys;
        private Object[] vals;
        private int size;
        private int mask;
        private int growThreshold;

        LongObjMap(int expected) {
            int cap = 16;
            while (cap < (expected * 2)) cap <<= 1;
            keys = new long[cap];
            vals = new Object[cap];
            mask = cap - 1;
            growThreshold = (int) (cap * 0.65f);
            java.util.Arrays.fill(keys, EMPTY);
        }

        @SuppressWarnings("unchecked")
        V get(long k) {
            int p = mix64to32(k) & mask;
            while (true) {
                long kk = keys[p];
                if (kk == EMPTY) return null;
                if (kk == k) return (V) vals[p];
                p = (p + 1) & mask;
            }
        }

        void put(long k, V v) {
            if (size >= growThreshold) rehash(keys.length << 1);

            int p = mix64to32(k) & mask;
            while (true) {
                long kk = keys[p];
                if (kk == EMPTY) {
                    keys[p] = k;
                    vals[p] = v;
                    size++;
                    return;
                }
                if (kk == k) {
                    vals[p] = v;
                    return;
                }
                p = (p + 1) & mask;
            }
        }

        @SuppressWarnings("unchecked")
        V remove(long k) {
            int p = mix64to32(k) & mask;
            while (true) {
                long kk = keys[p];
                if (kk == EMPTY) return null;
                if (kk == k) {
                    V prev = (V) vals[p];
                    shiftKeys(p);
                    size--;
                    return prev;
                }
                p = (p + 1) & mask;
            }
        }

        void clear() {
            java.util.Arrays.fill(keys, EMPTY);
            java.util.Arrays.fill(vals, null);
            size = 0;
        }

        interface LongObjConsumer<V> { void accept(long k, V v); }

        @SuppressWarnings("unchecked")
        void forEach(LongObjConsumer<V> c) {
            for (int i = 0; i < keys.length; i++) {
                long k = keys[i];
                if (k != EMPTY) c.accept(k, (V) vals[i]);
            }
        }

        private void rehash(int newCap) {
            long[] oldK = keys;
            Object[] oldV = vals;

            keys = new long[newCap];
            vals = new Object[newCap];
            java.util.Arrays.fill(keys, EMPTY);

            mask = newCap - 1;
            growThreshold = (int) (newCap * 0.65f);
            size = 0;

            for (int i = 0; i < oldK.length; i++) {
                long k = oldK[i];
                if (k != EMPTY) put(k, (V) oldV[i]);
            }
        }

        private void shiftKeys(int pos) {
            long[] k = keys;
            Object[] v = vals;

            int last, slot;
            long curr;
            while (true) {
                last = pos;
                pos = (pos + 1) & mask;
                while (true) {
                    curr = k[pos];
                    if (curr == EMPTY) {
                        k[last] = EMPTY;
                        v[last] = null;
                        return;
                    }
                    slot = mix64to32(curr) & mask;
                    if (last <= pos ? (last >= slot || slot > pos) : (last >= slot && slot > pos)) break;
                    pos = (pos + 1) & mask;
                }
                k[last] = curr;
                v[last] = v[pos];
            }
        }

        private static int mix64to32(long x) {
            x ^= (x >>> 33);
            x *= 0xff51afd7ed558ccdL;
            x ^= (x >>> 33);
            x *= 0xc4ceb9fe1a85ec53L;
            x ^= (x >>> 33);
            return (int) x;
        }
    }
}
