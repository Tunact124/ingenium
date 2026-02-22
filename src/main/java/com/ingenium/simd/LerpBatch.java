package com.ingenium.simd;

/**
 * Batch staging state for noise interpolation.
 */
public final class LerpBatch {
    public int n;
    public final float[] a = new float[8];
    public final float[] b = new float[8];
    public final float[] t = new float[8];
    public final float[] out = new float[8];

    public void reset() {
        this.n = 0;
    }
}
