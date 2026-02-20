package com.ingenium.benchmark;

import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.WeakHashMap;

public final class IngeniumBenchmarkService {
    private static final IngeniumBenchmarkService INSTANCE = new IngeniumBenchmarkService();
    private final ChunkLatencyMonitor chunkLatency = new ChunkLatencyMonitor();

    // Weak map to avoid leaking worlds/chunk managers across reloads
    private final Map<ServerChunkManager, ServerWorld> boundWorlds = new WeakHashMap<>();

    private IngeniumBenchmarkService() {
    }

    public static IngeniumBenchmarkService get() {
        return INSTANCE;
    }

    public void init() {
    }

    public ChunkLatencyMonitor getChunkLatency() {
        return chunkLatency;
    }

    public void bindChunkManager(ServerChunkManager manager, ServerWorld world) {
        if (manager != null && world != null) {
            boundWorlds.put(manager, world);
        }
    }

    public ServerWorld getBoundWorldForChunkManager(ServerChunkManager manager) {
        return boundWorlds.get(manager);
    }
}
