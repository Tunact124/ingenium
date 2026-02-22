package com.ingenium.mixin.worldgen;

import com.ingenium.simd.SIMDCapability;
import com.ingenium.worldgen.SimdLerp;
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
    @Shadow private double noise000;
    @Shadow private double noise001;
    @Shadow private double noise100;
    @Shadow private double noise101;
    @Shadow private double noise010;
    @Shadow private double noise011;
    @Shadow private double noise110;
    @Shadow private double noise111;

    // Intermediate lerp values
    @Shadow private double valueXZ00;
    @Shadow private double valueXZ10;
    @Shadow private double valueXZ01;
    @Shadow private double valueXZ11;

    @Shadow private double valueZ0;
    @Shadow private double valueZ1;

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
        if (!SIMDCapability.isAvailable()) {
            this.valueXZ00 = Math.fma(delta, this.noise010 - this.noise000, this.noise000);
            this.valueXZ10 = Math.fma(delta, this.noise110 - this.noise100, this.noise100);
            this.valueXZ01 = Math.fma(delta, this.noise011 - this.noise001, this.noise001);
            this.valueXZ11 = Math.fma(delta, this.noise111 - this.noise101, this.noise101);
            ci.cancel();
            return;
        }

        // SIMD 4-wide lerp
        SimdLerp.lerp4Fma(delta,
                this.noise000, this.noise010,
                this.noise100, this.noise110,
                this.noise001, this.noise011,
                this.noise101, this.noise111,
                this.ingenium$out4);

        this.valueXZ00 = this.ingenium$out4.o0;
        this.valueXZ10 = this.ingenium$out4.o1;
        this.valueXZ01 = this.ingenium$out4.o2;
        this.valueXZ11 = this.ingenium$out4.o3;

        ci.cancel();
    }

    /**
     * updateForX(delta):
     * v0 = lerp(delta, v00, v10)
     * v1 = lerp(delta, v01, v11)
     */
    @Inject(method = "updateForX(D)V", at = @At("HEAD"), cancellable = true)
    private void ingenium$updateForX(double delta, CallbackInfo ci) {
        this.valueZ0 = Math.fma(delta, this.valueXZ10 - this.valueXZ00, this.valueXZ00);
        this.valueZ1 = Math.fma(delta, this.valueXZ11 - this.valueXZ01, this.valueXZ01);
        ci.cancel();
    }

    /**
     * updateForZ(delta):
     * value = lerp(delta, v0, v1)
     */
    @Inject(method = "updateForZ(D)V", at = @At("HEAD"), cancellable = true)
    private void ingenium$updateForZ(double delta, CallbackInfo ci) {
        this.value = Math.fma(delta, this.valueZ1 - this.valueZ0, this.valueZ0);
        ci.cancel();
    }
}
