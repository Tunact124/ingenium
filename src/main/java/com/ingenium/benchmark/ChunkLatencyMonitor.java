package com.ingenium.benchmark;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public final class ChunkLatencyMonitor {
    public Snapshot snapshotAndReset() {
        return new Snapshot(new Long2ObjectOpenHashMap<>());
    }

    // Best-effort no-op recorders to satisfy callers; full implementation can be added later.
    public void recordRequest(RegistryKey<World> dim, long packedChunkPos, long nowNs) {
    }

    public void recordReady(RegistryKey<World> dim, long packedChunkPos, long nowNs) {
    }

    public static final class Snapshot {
        public final Long2ObjectMap<LatencyStats> statsByDim;

        public Snapshot(Long2ObjectMap<LatencyStats> statsByDim) {
            this.statsByDim = statsByDim;
        }
    }

    public static final class LatencyStats {
        public long samples;
        private double avgMs;
        private double maxMs;

        public double avgMs() {
            return avgMs;
        }

        public double maxMs() {
            return maxMs;
        }
    }
}
