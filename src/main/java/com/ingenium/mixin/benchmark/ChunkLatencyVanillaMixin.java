package com.ingenium.mixin.benchmark;

import com.ingenium.benchmark.IngeniumBenchmarkService;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla-only latency proxy:
 * - recordRequest: when getChunk(...) is called with create=true (request path)
 * - recordReady: when getWorldChunk(...) returns non-null (ready-to-use)
 */
@Mixin(ServerChunkCache.class)
public abstract class ChunkLatencyVanillaMixin {

    @Inject(
            method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At("HEAD")
    )
    private void ingenium$recordRequest(int chunkX, int chunkZ,
                                        ChunkStatus status,
                                        boolean create,
                                        CallbackInfoReturnable<ChunkAccess> cir) {
        if (!create) return;
        ServerChunkCache scm = (ServerChunkCache) (Object) this;
        IngeniumBenchmarkService svc = IngeniumBenchmarkService.get();
        ServerLevel world = svc.getBoundWorldForChunkManager(scm);
        if (world == null) return;

        long now = System.nanoTime();
        long packed = ChunkPos.asLong(chunkX, chunkZ);
        svc.getChunkLatency().recordRequest(world.dimension(), packed, now);
    }

    @Inject(
            method = "getChunkNow(II)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("RETURN")
    )
    private void ingenium$recordReady(int chunkX, int chunkZ,
                                      CallbackInfoReturnable<LevelChunk> cir) {
        if (cir.getReturnValue() == null) return;
        ServerChunkCache scm = (ServerChunkCache) (Object) this;

        IngeniumBenchmarkService svc = IngeniumBenchmarkService.get();
        ServerLevel world = svc.getBoundWorldForChunkManager(scm);
        if (world == null) return;

        long now = System.nanoTime();
        long packed = ChunkPos.asLong(chunkX, chunkZ);
        svc.getChunkLatency().recordReady(world.dimension(), packed, now);
    }
}
