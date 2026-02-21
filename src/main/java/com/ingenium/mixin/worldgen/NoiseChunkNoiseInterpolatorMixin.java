package com.ingenium.mixin.worldgen;

import com.ingenium.worldgen.SimdLerp;
import com.ingenium.worldgen.VectorGuard;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Official Mojang mappings target:
 * net.minecraft.world.level.levelgen.NoiseChunk$NoiseInterpolator
 */
@Mixin(NoiseChunk.NoiseInterpolator.class)
public abstract class NoiseChunkNoiseInterpolatorMixin {

    // Corner values (Mojang naming in source)
    @Shadow private double v000;
    @Shadow private double v001;
    @Shadow private double v100;
    @Shadow private double v101;
    @Shadow private double v010;
    @Shadow private double v011;
    @Shadow private double v110;
    @Shadow private double v111;

    // Intermediate lerp values
    @Shadow private double v00;
    @Shadow private double v10;
    @Shadow private double v01;
    @Shadow private double v11;

    @Shadow private double v0;
    @Shadow private double v1;

    @Shadow private double value;

    @Unique
    private final SimdLerp.Out4 ingenium$out4 = new SimdLerp.Out4();

    /**
     * updateForY(delta):
     * v00 = lerp(delta, v000, v001)
     * v10 = lerp(delta, v100, v101)
     * v01 = lerp(delta, v010, v011)
     * v11 = lerp(delta, v110, v111)
     */
    @Inject(method = "updateForY(D)V", at = @At("HEAD"), cancellable = true)
    private void ingenium$updateForY(double delta, CallbackInfo ci) {
        // Fast scalar fallback (still FMA-lerp, just not SIMD)
        if (!VectorGuard.SIMD_AVAILABLE) {
            this.v00 = Math.fma(delta, this.v001 - this.v000, this.v000);
            this.v10 = Math.fma(delta, this.v101 - this.v100, this.v100);
            this.v01 = Math.fma(delta, this.v011 - this.v010, this.v010);
            this.v11 = Math.fma(delta, this.v111 - this.v110, this.v110);
            ci.cancel();
            return;
        }

        // SIMD 4-wide lerp
        SimdLerp.lerp4Fma(delta,
                this.v000, this.v001,
                this.v100, this.v101,
                this.v010, this.v011,
                this.v110, this.v111,
                this.ingenium$out4);

        this.v00 = this.ingenium$out4.o0;
        this.v10 = this.ingenium$out4.o1;
        this.v01 = this.ingenium$out4.o2;
        this.v11 = this.ingenium$out4.o3;

        ci.cancel();
    }

    /**
     * updateForX(delta):
     * v0 = lerp(delta, v00, v10)
     * v1 = lerp(delta, v01, v11)
     */
    @Inject(method = "updateForX(D)V", at = @At("HEAD"), cancellable = true)
    private void ingenium$updateForX(double delta, CallbackInfo ci) {
        this.v0 = Math.fma(delta, this.v10 - this.v00, this.v00);
        this.v1 = Math.fma(delta, this.v11 - this.v01, this.v01);
        ci.cancel();
    }

    /**
     * updateForZ(delta):
     * value = lerp(delta, v0, v1)
     */
    @Inject(method = "updateForZ(D)V", at = @At("HEAD"), cancellable = true)
    private void ingenium$updateForZ(double delta, CallbackInfo ci) {
        this.value = Math.fma(delta, this.v1 - this.v0, this.v0);
        ci.cancel();
    }
}
