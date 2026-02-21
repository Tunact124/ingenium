package com.ingenium.worldgen;

public final class WorldgenThreadLocals {
    private WorldgenThreadLocals() {}

    public static final ThreadLocal<NoiseKernelScratch> NOISE_SCRATCH =
            ThreadLocal.withInitial(NoiseKernelScratch::new);
}
