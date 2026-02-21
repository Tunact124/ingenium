package com.ingenium.worldgen;

public final class NoiseKernelScratch {
    // 8-wide is common on AVX2; but we will use VectorSpecies.PREFERRED at runtime anyway.
    // These arrays are “scratch staging” for scalar -> vector loads, kept thread-local.
    final float[] a = new float[8];
    final float[] b = new float[8];
    final float[] t = new float[8];
    final float[] out = new float[8];
}
