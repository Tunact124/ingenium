package com.ingenium.benchmark;

import com.ingenium.IngeniumMod;
import com.ingenium.compat.CompatibilityBridge;
import com.ingenium.governor.OptimizationProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SimpleBenchmark {
    private final ConfigStateManager configManager = new ConfigStateManager();
    private final SafeMetricsCollector metricsOff = new SafeMetricsCollector();
    private final SafeMetricsCollector metricsOn = new SafeMetricsCollector();
    private final List<Entity> spawnedEntities = new ArrayList<>();
    private boolean isRunning = false;
    
    public void run(ServerCommandSource source, int secondsPerPhase) {
        if (isRunning) {
            source.sendMessage(Text.literal("Benchmark already running!").formatted(Formatting.RED));
            return;
        }
        
        if (secondsPerPhase < 5 || secondsPerPhase > 300) {
            source.sendMessage(Text.literal("Duration must be 5-300 seconds").formatted(Formatting.RED));
            return;
        }
        
        isRunning = true;
        
        // Add shutdown hook to restore config if game crashes
        Thread restoreHook = new Thread(() -> {
            System.err.println("[Ingenium] Emergency config restore due to shutdown/crash");
            configManager.restoreOriginalState();
            IngeniumMod.GOVERNOR.releaseProfile();
        });
        Runtime.getRuntime().addShutdownHook(restoreHook);
        
        new Thread(() -> {
            try {
                source.sendMessage(Text.literal("§6§l════════════════════════════════════"));
                source.sendMessage(Text.literal("§e§l  INGENIUM PERFORMANCE BENCHMARK"));
                source.sendMessage(Text.literal("§6§l════════════════════════════════════"));
                source.sendMessage(Text.literal(""));
                
                // STEP 1: Warmup
                source.sendMessage(Text.literal("§7Step 1/5: Warmup (5s)..."));
                safeSleep(5000);

                // Capture current state
                configManager.captureState();

                // STEP 2: Entity Spawn
                source.sendMessage(Text.literal("§7Step 2/5: Spawning 300+ entities..."));
                spawnEntities(source.getWorld(), source.getWorld().getSpawnPos(), 300);
                safeSleep(2000); // Wait for entities to settle

                // STEP 3: Vanilla Run
                source.sendMessage(Text.literal("§c§lStep 3/5: PHASE 1 - Testing WITHOUT Ingenium optimizations"));
                source.sendMessage(Text.literal("§7Disabling all features in config..."));
                configManager.disableAllFeatures();
                IngeniumMod.GOVERNOR.releaseProfile();
                
                source.sendMessage(Text.literal("§7Stabilizing (5s)..."));
                safeSleep(5000);
                
                source.sendMessage(Text.literal("§bRunning baseline test for " + secondsPerPhase + "s..."));
                startCollecting(metricsOff);
                safeSleep(secondsPerPhase * 1000L);
                stopCollecting();
                
                // STEP 4: Ingenium Run
                source.sendMessage(Text.literal(""));
                source.sendMessage(Text.literal("§a§lStep 4/5: PHASE 2 - Testing WITH Ingenium optimizations"));
                source.sendMessage(Text.literal("§7Enabling all features..."));
                configManager.enableAllFeatures();
                IngeniumMod.GOVERNOR.forceProfile(OptimizationProfile.AGGRESSIVE);
                
                source.sendMessage(Text.literal("§7Stabilizing (5s)..."));
                safeSleep(5000);
                
                source.sendMessage(Text.literal("§bRunning optimization test for " + secondsPerPhase + "s..."));
                startCollecting(metricsOn);
                safeSleep(secondsPerPhase * 1000L);
                stopCollecting();
                
                // STEP 5: Cleanup
                source.sendMessage(Text.literal(""));
                source.sendMessage(Text.literal("§7Step 5/5: Cleanup..."));
                cleanupEntities();
                configManager.restoreOriginalState();
                IngeniumMod.GOVERNOR.releaseProfile();
                
                generateReport(source, secondsPerPhase);
                
            } catch (OutOfMemoryError e) {
                source.sendMessage(Text.literal("§4§lCRITICAL: Out of Memory! Restoring config...").formatted(Formatting.RED));
                configManager.restoreOriginalState();
                IngeniumMod.GOVERNOR.releaseProfile();
                System.gc();
            } catch (Exception e) {
                source.sendMessage(Text.literal("§4Error: " + e.getMessage()).formatted(Formatting.RED));
                e.printStackTrace();
            } finally {
                isRunning = false;
                configManager.restoreOriginalState();
                IngeniumMod.GOVERNOR.releaseProfile();
                try {
                    Runtime.getRuntime().removeShutdownHook(restoreHook);
                } catch (IllegalStateException ignored) {}
            }
        }, "Ingenium-Benchmark-Thread").start();
    }
    
    private void startCollecting(SafeMetricsCollector metrics) {
        metrics.start();
        IngeniumMod.activeBenchmarkCollector = metrics;
    }
    
    private void stopCollecting() {
        IngeniumMod.activeBenchmarkCollector = null;
    }
    
    private void spawnEntities(ServerWorld world, BlockPos center, int count) {
        MinecraftServer server = world.getServer();
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        server.execute(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    // Access world.random and EntityType.spawn on server thread
                    BlockPos pos = center.add(world.random.nextInt(20) - 10, 0, world.random.nextInt(20) - 10);
                    Entity entity = EntityType.VILLAGER.spawn(world, pos, SpawnReason.COMMAND);
                    if (entity != null) {
                        spawnedEntities.add(entity);
                    }
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        try {
            future.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void cleanupEntities() {
        if (spawnedEntities.isEmpty()) return;
        
        Entity first = spawnedEntities.get(0);
        if (first == null || first.getServer() == null) {
            spawnedEntities.clear();
            return;
        }
        
        MinecraftServer server = first.getServer();
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        server.execute(() -> {
            for (Entity entity : spawnedEntities) {
                if (entity != null) {
                    entity.discard();
                }
            }
            spawnedEntities.clear();
            future.complete(null);
        });
        
        try {
            future.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void safeSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void generateReport(ServerCommandSource source, int duration) {
        try {
            String report = buildReportText(duration, source.getServer());
            
            // Save to file
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path dir = Path.of("ingenium-benchmarks");
            Files.createDirectories(dir);
            Path file = dir.resolve("benchmark-report-" + timestamp + ".txt");
            Files.writeString(file, report);
            
            // Send summary to player
            double fpsGain = metricsOn.getAverageFps() - metricsOff.getAverageFps();
            double tickImprovement = metricsOff.getAverageTickTime() - metricsOn.getAverageTickTime();
            
            source.sendMessage(Text.literal("§6§l════════════════════════════════════"));
            source.sendMessage(Text.literal("§a§lBENCHMARK COMPLETE"));
            source.sendMessage(Text.literal("§6§l════════════════════════════════════"));
            source.sendMessage(Text.literal(""));
            source.sendMessage(Text.literal(String.format("§eFPS Change: §r%.1f (%.1f → %.1f)", 
                fpsGain, metricsOff.getAverageFps(), metricsOn.getAverageFps())));
            source.sendMessage(Text.literal(String.format("§eTPS Improvement: §r%.1f (%.1f → %.1f)", 
                metricsOn.getAverageTps() - metricsOff.getAverageTps(), metricsOff.getAverageTps(), metricsOn.getAverageTps())));
            source.sendMessage(Text.literal(String.format("§eMSPT Improvement: §r%.2fms (%.2f → %.2f)", 
                tickImprovement, metricsOff.getAverageTickTime(), metricsOn.getAverageTickTime())));
            source.sendMessage(Text.literal(String.format("§eMemory Peak: §r%dMB → %dMB", 
                metricsOff.getPeakMemory(), metricsOn.getPeakMemory())));
            source.sendMessage(Text.literal(""));
            source.sendMessage(Text.literal("§7Full report saved to:"));
            source.sendMessage(Text.literal("§o" + file.toString()));
            
        } catch (IOException e) {
            source.sendMessage(Text.literal("§cFailed to save report: " + e.getMessage()));
        }
    }
    
    private String buildReportText(int duration, MinecraftServer server) {
        StringBuilder sb = new StringBuilder();
        String separator = "=====================================================\n";
        
        // Header
        sb.append(separator);
        sb.append("           INGENIUM PERFORMANCE BENCHMARK REPORT\n");
        sb.append("                      Generated: ").append(LocalDateTime.now().toString()).append("\n");
        sb.append(separator);
        sb.append("\n");
        
        // System Information
        sb.append("SYSTEM INFORMATION\n");
        sb.append("-----------------------------------------------------\n");
        sb.append(String.format("Minecraft Version: %s\n", server.getVersion()));
        sb.append(String.format("Java Version: %s\n", System.getProperty("java.version")));
        sb.append(String.format("Operating System: %s %s\n", 
            System.getProperty("os.name"), System.getProperty("os.version")));
        sb.append(String.format("Architecture: %s\n", System.getProperty("os.arch")));
        sb.append(String.format("Available Processors: %d\n", Runtime.getRuntime().availableProcessors()));
        sb.append(String.format("Allocated Memory: %d MB\n", Runtime.getRuntime().maxMemory() / 1024 / 1024));
        sb.append("\n");
        
        // Active Performance Mods
        sb.append("ACTIVE PERFORMANCE MODS (DETECTION)\n");
        sb.append("-----------------------------------------------------\n");
        sb.append(String.format("Sodium:       %s\n", CompatibilityBridge.HAS_SODIUM ? "PRESENT" : "ABSENT"));
        sb.append(String.format("Lithium:      %s\n", CompatibilityBridge.HAS_LITHIUM ? "PRESENT" : "ABSENT"));
        sb.append(String.format("FerriteCore:  %s\n", CompatibilityBridge.HAS_FERRITECORE ? "PRESENT" : "ABSENT"));
        sb.append(String.format("Starlight:    %s\n", CompatibilityBridge.HAS_STARLIGHT ? "PRESENT" : "ABSENT"));
        sb.append(String.format("Bobby:        %s\n", CompatibilityBridge.HAS_BOBBY ? "PRESENT" : "ABSENT"));
        sb.append(String.format("Fabric Item API: %s\n", CompatibilityBridge.HAS_FABRIC_ITEM_API ? "PRESENT" : "ABSENT"));
        sb.append("\n");
        
        // Test Parameters
        sb.append("TEST PARAMETERS\n");
        sb.append("-----------------------------------------------------\n");
        sb.append(String.format("Phase Duration: %d seconds\n", duration));
        sb.append(String.format("Entities Spawned: %d\n", spawnedEntities.size()));
        sb.append(String.format("Samples Collected (OFF): %d\n", metricsOff.getSnapshots().size()));
        sb.append(String.format("Samples Collected (ON): %d\n", metricsOn.getSnapshots().size()));
        sb.append("\n");
        
        // Results Table
        sb.append("RESULTS COMPARISON\n");
        sb.append("-----------------------------------------------------\n");
        sb.append(String.format("%-20s %15s %15s %10s\n", "Metric", "Features OFF", "Features ON", "Delta"));
        sb.append("-----------------------------------------------------\n");
        
        double fpsOff = metricsOff.getAverageFps();
        double fpsOn = metricsOn.getAverageFps();
        double fpsDelta = fpsOn - fpsOff;
        sb.append(String.format("%-20s %15.1f %15.1f %+9.1f%%\n", 
            "Average FPS", fpsOff, fpsOn, (fpsOff > 0 ? (fpsDelta / fpsOff) * 100 : 0)));
        
        double tickOff = metricsOff.getAverageTickTime();
        double tickOn = metricsOn.getAverageTickTime();
        double tickDelta = tickOff - tickOn;
        sb.append(String.format("%-20s %15.2f %15.2f %+9.2fms\n", 
            "MSPT (Mean)", tickOff, tickOn, tickDelta));

        double tpsOff = metricsOff.getAverageTps();
        double tpsOn = metricsOn.getAverageTps();
        double tpsDelta = tpsOn - tpsOff;
        sb.append(String.format("%-20s %15.1f %15.1f %+9.1f\n", 
            "TPS (Average)", tpsOff, tpsOn, tpsDelta));
        
        long memOff = metricsOff.getPeakMemory();
        long memOn = metricsOn.getPeakMemory();
        sb.append(String.format("%-20s %15d %15d %9dMB\n", 
            "Peak Memory (MB)", memOff, memOn, memOn - memOff));
        
        sb.append("-----------------------------------------------------\n");
        sb.append("\n");
        
        // Review Questions
        sb.append("REVIEW QUESTIONS\n");
        sb.append("-----------------------------------------------------\n");
        sb.append("1. MSPT Comparison: Did the \"Fully Optimized\" run maintain the <20 MSPT target? \n");
        sb.append("   [ ] Yes  [ ] No  (Actual: " + String.format("%.2f", tickOn) + " ms)\n\n");
        
        sb.append("2. AI Efficiency: Was there a significant reduction in COMPUTE_POOL latency?\n");
        sb.append("   [ ] Yes  [ ] No  (MSPT Improvement: " + String.format("%.2f", tickDelta) + " ms)\n\n");
        
        sb.append("3. GC Stability: Did the \"Spikes\" count decrease due to object pooling?\n");
        sb.append("   (Check peak memory difference: " + (memOn - memOff) + " MB)\n\n");
        
        sb.append("4. Entity Density: How many entities were simulated? " + spawnedEntities.size() + "\n");
        sb.append("\n");
        
        sb.append("VISUAL PERFORMANCE QUESTIONS\n");
        sb.append("   a) Did you notice any difference in FPS? (Y/N)\n");
        sb.append("   b) Did it feel smoother with features ON? (Y/N)\n");
        sb.append("\n");
        
        // Raw Data Appendix
        sb.append("RAW DATA (First 10 samples per phase)\n");
        sb.append("-----------------------------------------------------\n");
        sb.append("Features OFF:\n");
        sb.append("Time(ms) | Memory(MB) | FPS | Tick(ms)\n");
        metricsOff.getSnapshots().stream().limit(10).forEach(s -> {
            sb.append(String.format("%8d | %10d | %3d | %8.2f\n", 
                s.timestamp % 100000, s.memoryUsedMB, s.fps, s.tickTimeMs));
        });
        
        sb.append("\nFeatures ON:\n");
        sb.append("Time(ms) | Memory(MB) | FPS | Tick(ms)\n");
        metricsOn.getSnapshots().stream().limit(10).forEach(s -> {
            sb.append(String.format("%8d | %10d | %3d | %8.2f\n", 
                s.timestamp % 100000, s.memoryUsedMB, s.fps, s.tickTimeMs));
        });
        
        sb.append("\n");
        sb.append(separator);
        sb.append("End of Report\n");
        sb.append(separator);
        
        return sb.toString();
    }
}
