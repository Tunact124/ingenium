package com.ingenium.tick;

import com.ingenium.config.IngeniumConfig;

/**
 * Per-world storage of wheel-backed schedulers for block + fluid.
 *
 * Thread ownership:
 * - Server thread only.
 */
public final class WheelStore {

    public final WheelBackedWorldTickScheduler<net.minecraft.block.Block> blockWheel;
    public final WheelBackedWorldTickScheduler<net.minecraft.fluid.Fluid> fluidWheel;

    /** If true, we will flush wheel -> vanilla scheduler on next save tick. */
    public volatile boolean offboardRequested = false;

    public WheelStore() {
        int buckets = IngeniumConfig.getInstance().timingWheelBucketCount;
        this.blockWheel = new WheelBackedWorldTickScheduler<>(buckets);
        this.fluidWheel = new WheelBackedWorldTickScheduler<>(buckets);
    }

    public void requestOffboard() {
        offboardRequested = true;
        blockWheel.requestOffboard();
        fluidWheel.requestOffboard();
    }

    public boolean isWheelEnabled() {
        return blockWheel.isWheelEnabled() && fluidWheel.isWheelEnabled();
    }
}
