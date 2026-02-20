package com.ingenium.mixin.benchmark;

import com.ingenium.benchmark.IngeniumBenchmarkService;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla-only latency proxy:
 * - recordRequest: when getChunk(...) is called with create=true (request path)
 * - recordReady: when getWorldChunk(...) returns non-null (ready-to-use)
 */
@Mixin(ServerChunkManager.class)
public abstract class ChunkLatencyVanillaMixin {

    @Inject(
            method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;",
            at = @At("HEAD")
    )
    private void ingenium$recordRequest(int chunkX, int chunkZ,
                                        ChunkStatus status,
                                        boolean create,
                                        CallbackInfoReturnable<Chunk> cir) {
        if (!create) return;
        ServerChunkManager scm = (ServerChunkManager) (Object) this;
        IngeniumBenchmarkService svc = IngeniumBenchmarkService.get();
        ServerWorld world = svc.getBoundWorldForChunkManager(scm);
        if (world == null) return;

        long now = System.nanoTime();
        long packed = ChunkPos.toLong(chunkX, chunkZ);
        svc.getChunkLatency().recordRequest(world.getRegistryKey(), packed, now);
    }

    @Inject(
            method = "getWorldChunk(II)Lnet/minecraft/world/chunk/WorldChunk;",
            at = @At("RETURN")
    )
    private void ingenium$recordReady(int chunkX, int chunkZ,
                                      CallbackInfoReturnable<WorldChunk> cir) {
        if (cir.getReturnValue() == null) return;
        ServerChunkManager scm = (ServerChunkManager) (Object) this;

        IngeniumBenchmarkService svc = IngeniumBenchmarkService.get();
        ServerWorld world = svc.getBoundWorldForChunkManager(scm);
        if (world == null) return;

        long now = System.nanoTime();
        long packed = ChunkPos.toLong(chunkX, chunkZ);
        svc.getChunkLatency().recordReady(world.getRegistryKey(), packed, now);
    }
}
