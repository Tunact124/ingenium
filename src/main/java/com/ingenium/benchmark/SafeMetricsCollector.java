package com.ingenium.benchmark;

import net.minecraft.client.MinecraftClient;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SafeMetricsCollector {
    private final List<MetricSnapshot> snapshots = new CopyOnWriteArrayList<>();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private long startTime;
    private int tickCounter = 0;
    private long lastTickTime = 0;
    
    public static class MetricSnapshot {
        public final long timestamp;
        public final long memoryUsedMB;
        public final long memoryCommittedMB;
        public final int fps;
        public final double tickTimeMs;
        
        public MetricSnapshot(long memUsed, long memCommitted, int fps, double tickMs) {
            this.timestamp = System.currentTimeMillis();
            this.memoryUsedMB = memUsed;
            this.memoryCommittedMB = memCommitted;
            this.fps = fps;
            this.tickTimeMs = tickMs;
        }
    }
    
    public void start() {
        startTime = System.currentTimeMillis();
        snapshots.clear();
        tickCounter = 0;
        lastTickTime = System.nanoTime();
    }
    
    public void recordTick() {
        long now = System.nanoTime();
        if (tickCounter > 0) {
            long diff = now - lastTickTime;
            // Convert to ms, cap at 1000ms (would indicate frozen game)
            double tickMs = Math.min(diff / 1_000_000.0, 1000.0);
            
            // Only sample every 20 ticks (1 second) to avoid overhead
            if (tickCounter % 20 == 0) {
                sample(tickMs);
            }
        }
        lastTickTime = now;
        tickCounter++;
    }
    
    private void sample(double tickMs) {
        try {
            long used = memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
            long committed = memoryBean.getHeapMemoryUsage().getCommitted() / 1024 / 1024;
            
            // Safely get FPS - only if client exists
            int fps = 0;
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    fps = client.getCurrentFps();
                }
            } catch (NoClassDefFoundError | Exception ignored) {}
            
            snapshots.add(new MetricSnapshot(used, committed, fps, tickMs));
            
        } catch (Exception e) {
            // Fail silently - don't crash benchmark
            System.err.println("[Benchmark] Sample failed: " + e.getMessage());
        }
    }
    
    public List<MetricSnapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }
    
    public long getDurationSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
    
    public double getAverageFps() {
        return snapshots.stream().mapToInt(s -> s.fps).average().orElse(0.0);
    }
    
    public double getAverageTickTime() {
        return snapshots.stream().mapToDouble(s -> s.tickTimeMs).average().orElse(0.0);
    }
    
    public double getAverageTps() {
        return snapshots.stream()
            .mapToDouble(s -> Math.min(20.0, 1000.0 / Math.max(s.tickTimeMs, 1.0)))
            .average().orElse(20.0);
    }
    
    public long getPeakMemory() {
        return snapshots.stream().mapToLong(s -> s.memoryUsedMB).max().orElse(0);
    }
    
    public long getFinalMemory() {
        if (snapshots.isEmpty()) return 0;
        return snapshots.get(snapshots.size() - 1).memoryUsedMB;
    }
}
