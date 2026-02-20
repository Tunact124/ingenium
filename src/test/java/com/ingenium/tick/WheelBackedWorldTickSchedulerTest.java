package com.ingenium.tick;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.TickPriority;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WheelBackedWorldTickSchedulerTest {

    @Test
    void testScheduleAndDrain() {
        WheelBackedWorldTickScheduler<String> scheduler = new WheelBackedWorldTickScheduler<>(32);
        List<String> executed = new ArrayList<>();

        BlockPos pos = BlockPos.ORIGIN;
        
        // Schedule some ticks
        scheduler.schedule(new OrderedTick<>("type1", pos, 10, TickPriority.NORMAL, 0), 0);
        scheduler.schedule(new OrderedTick<>("type2", pos, 10, TickPriority.NORMAL, 1), 0);
        scheduler.schedule(new OrderedTick<>("type3", pos, 11, TickPriority.NORMAL, 0), 0);
        scheduler.schedule(new OrderedTick<>("future", pos, 100, TickPriority.NORMAL, 0), 0);

        // Drain at t=10
        int count = scheduler.drainDue(10, 10, (p, type, priority) -> executed.add(type));
        assertEquals(2, count);
        assertEquals(List.of("type1", "type2"), executed);

        executed.clear();
        // Drain at t=11
        count = scheduler.drainDue(11, 10, (p, type, priority) -> executed.add(type));
        assertEquals(1, count);
        assertEquals(List.of("type3"), executed);
        
        // Drain at t=100
        count = scheduler.drainDue(100, 10, (p, type, priority) -> executed.add(type));
        assertEquals(1, count);
        assertTrue(executed.contains("future"));
    }

    @Test
    void testOverflowHandling() {
        // wheelBits for 32 is 5. Cycles are every 32 ticks.
        WheelBackedWorldTickScheduler<String> scheduler = new WheelBackedWorldTickScheduler<>(32);
        List<String> executed = new ArrayList<>();

        BlockPos pos = BlockPos.ORIGIN;
        
        // t=100 is in cycle 100 >>> 5 = 3.
        // current cycle at t=0 is 0.
        scheduler.schedule(new OrderedTick<>("overflow", pos, 100, TickPriority.NORMAL, 0), 0);
        
        assertEquals(1, scheduler.getOverflowed());

        // Drain at t=100. This should advance cycles and move from overflow to bucket.
        int count = scheduler.drainDue(100, 10, (p, type, priority) -> executed.add(type));
        assertEquals(1, count);
        assertEquals(List.of("overflow"), executed);
    }

    @Test
    void testTimeJump() {
        WheelBackedWorldTickScheduler<String> scheduler = new WheelBackedWorldTickScheduler<>(32);
        List<String> executed = new ArrayList<>();
        BlockPos pos = BlockPos.ORIGIN;

        // Schedule in multiple future cycles
        scheduler.schedule(new OrderedTick<>("c1", pos, 40, TickPriority.NORMAL, 0), 0); // cycle 1
        scheduler.schedule(new OrderedTick<>("c2", pos, 80, TickPriority.NORMAL, 0), 0); // cycle 2
        
        // Jump straight to t=80. Cycles 1 and 2 should both be processed.
        int count = scheduler.drainDue(80, 10, (p, type, priority) -> executed.add(type));
        assertEquals(2, count);
        assertTrue(executed.contains("c1"));
        assertTrue(executed.contains("c2"));
    }
}
