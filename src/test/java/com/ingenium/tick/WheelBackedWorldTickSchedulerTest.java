package com.ingenium.tick;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WheelBackedWorldTickSchedulerTest {

    @Test
    void testScheduleAndDrain() {
        WheelBackedWorldTickScheduler<String> scheduler = new WheelBackedWorldTickScheduler<>(32, 1, 100000, 100000);
        List<String> executed = new ArrayList<>();

        // Schedule some ticks
        scheduler.schedule(10, "type1");
        scheduler.schedule(10, "type2");
        scheduler.schedule(11, "type3");
        scheduler.schedule(100, "future");

        // Drain at t=10
        int count = scheduler.drainDue(10, 10, executed::add);
        assertEquals(2, count);
        assertEquals(List.of("type1", "type2"), executed);

        executed.clear();
        // Drain at t=11
        count = scheduler.drainDue(11, 10, executed::add);
        assertEquals(1, count);
        assertEquals(List.of("type3"), executed);

        executed.clear();
        // Drain at t=100
        count = scheduler.drainDue(100, 10, executed::add);
        assertEquals(1, count);
        assertTrue(executed.contains("future"));
    }

    @Test
    void testOverflowHandling() {
        // wheelBits for 32 is 5. Cycles are every 32 ticks.
        WheelBackedWorldTickScheduler<String> scheduler = new WheelBackedWorldTickScheduler<>(32, 1, 100000, 100000);
        List<String> executed = new ArrayList<>();

        // t=100 is in cycle 100 / 32 = 3.
        // current cycle at t=0 is 0.
        scheduler.schedule(100, "overflow");

        // Drain at t=100. This should advance cycles and move from overflow to bucket.
        int count = scheduler.drainDue(100, 10, executed::add);
        assertEquals(1, count);
        assertEquals(List.of("overflow"), executed);
    }

    @Test
    void testTimeJump() {
        WheelBackedWorldTickScheduler<String> scheduler = new WheelBackedWorldTickScheduler<>(32, 1, 100000, 100000);
        List<String> executed = new ArrayList<>();

        // Schedule in multiple future cycles
        scheduler.schedule(40, "c1"); // cycle 1
        scheduler.schedule(80, "c2"); // cycle 2

        // Jump straight to t=80. Cycles 1 and 2 should both be processed.
        int count = scheduler.drainDue(80, 10, executed::add);
        assertEquals(2, count);
        assertTrue(executed.contains("c1"));
        assertTrue(executed.contains("c2"));
    }
}
