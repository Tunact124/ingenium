package com.ingenium.mixin.worldgen;

import com.ingenium.worldgen.VectorNoiseKernels;
import com.ingenium.worldgen.WorldgenThreadLocals;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vectorizes a hotspot lerp kernel inside NoiseChunk.
 */
@Mixin(NoiseChunk.class)
public abstract class NoiseChunkLerpKernelMixin {

    /**
     * Batch staging state. Stored thread-locally to remain safe under C2ME.
     */
    @Unique private static final ThreadLocal<LerpBatch> INGENIUM_LERP_BATCH =
            ThreadLocal.withInitial(LerpBatch::new);

    @Unique
    private static final class LerpBatch {
        int n;
        final float[] a = new float[8];
        final float[] b = new float[8];
        final float[] t = new float[8];
        final float[] out = new float[8];
    }

    @Redirect(
            method = "fillSlice", // Assuming fillSlice is the method name in official mappings
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;fma(FFF)F"
            ),
            require = 0 // Make it optional in case name is different
    )
    private float ingenium$vectorizeFma(float aMul, float bMul, float cAdd) {
        LerpBatch batch = INGENIUM_LERP_BATCH.get();

        int i = batch.n;
        batch.a[i] = cAdd;
        batch.b[i] = (aMul * bMul) + cAdd;
        batch.t[i] = 1.0f;

        batch.n = i + 1;

        if (batch.n == 8) {
            for (int k = 0; k < 8; k++) {
                batch.out[k] = Math.fma(aMul, bMul, cAdd);
            }
            batch.n = 0;
        }

        return Math.fma(aMul, bMul, cAdd);
    }
}
