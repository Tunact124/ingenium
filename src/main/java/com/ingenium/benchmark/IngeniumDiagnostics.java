package com.ingenium.benchmark;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.atomic.LongAdder;

public final class IngeniumDiagnostics {
    private static final IngeniumDiagnostics INSTANCE = new IngeniumDiagnostics();

    private static final LongAdder SCHEDULED_BLOCK_TICKS = new LongAdder();
    private static final LongAdder SCHEDULED_FLUID_TICKS = new LongAdder();

    private IngeniumDiagnostics() {
    }

    public static IngeniumDiagnostics get() {
        return INSTANCE;
    }

    public static void onScheduledBlockTick(ServerWorld world, BlockPos pos, int delay) {
        SCHEDULED_BLOCK_TICKS.increment();
    }

    public static void onScheduledFluidTick(ServerWorld world, BlockPos pos, int delay) {
        SCHEDULED_FLUID_TICKS.increment();
    }

    public void onServerStartThreadCaptured() {
    }

    public void onTickEnd(long tickNs) {
    }

    public void onChunkMainThreadWait(long durationNs) {
    }

    public double lastTickMs() {
        return 0.0;
    }

    public long allocBytesDeltaWindow() {
        return 0L;
    }

    public long gcTimeDeltaWindowMs() {
        return 0L;
    }

    public long gcCountDeltaWindow() {
        return 0L;
    }

    public long chunkRequestCount() {
        return 0L;
    }

    public long chunkReadyCount() {
        return 0L;
    }

    public double chunkLatencyAvgMs() {
        return 0.0;
    }

    public double chunkLatencyMaxMs() {
        return 0.0;
    }

    public double chunkMainThreadWaitAvgMs() {
        return 0.0;
    }

    public double chunkMainThreadWaitMaxMs() {
        return 0.0;
    }

    public String governorSummary() {
        return "governor=unavailable";
    }

    public String asyncQueueSummary() {
        return "asyncQueue=unavailable";
    }

    public String wheelSummary() {
        return "wheel=unavailable";
    }

    public long scheduledBlockTickCount() {
        return SCHEDULED_BLOCK_TICKS.sum();
    }

    public long scheduledFluidTickCount() {
        return SCHEDULED_FLUID_TICKS.sum();
    }
}
