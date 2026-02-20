package com.ingenium.mixin;

import com.ingenium.benchmark.IngeniumDiagnostics;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;

/**
 * Tracks main-thread wait time in chunk pipeline by wrapping CompletableFuture.join().
 */
@Mixin(ServerChunkManager.class)
public abstract class ChunkWaitTimeMixin {

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;")
    )
    private Object ingenium_trackChunkJoin(CompletableFuture<?> future, Operation<Object> original) {
        long start = System.nanoTime();
        try {
            return original.call(future);
        } finally {
            long dur = System.nanoTime() - start;
            // Only count "meaningful" waits
            if (dur > 50_000) {
                IngeniumDiagnostics.get().onChunkMainThreadWait(dur);
            }
        }
    }
}
