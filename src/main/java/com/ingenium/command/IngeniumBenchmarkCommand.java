package com.ingenium.command;

import com.ingenium.benchmark.BenchmarkReportWriter;
import com.ingenium.benchmark.ChunkLatencyMonitor;
import com.ingenium.benchmark.IngeniumBenchmarkService;
import com.ingenium.benchmark.IngeniumDiagnostics;
import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.IngeniumExecutors;
import com.ingenium.core.IngeniumGovernor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * /ingenium benchmark [ticks] [--stress]
 *
 * Produces a TXT report under config/ingenium/benchmark.
 */
public final class IngeniumBenchmarkCommand {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("ingenium")
            .then(CommandManager.literal("benchmark")
                .executes(ctx -> run(ctx.getSource(), 200, false))
                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(40, 6000))
                    .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "ticks"), false))
                    .then(CommandManager.literal("stress")
                        .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "ticks"), true))
                    )
                )
                .then(CommandManager.literal("stress")
                    .executes(ctx -> run(ctx.getSource(), 400, true))
                    .then(CommandManager.argument("ticks", IntegerArgumentType.integer(40, 6000))
                        .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "ticks"), true))
                    )
                )
            )
        );
    }

    private static int run(ServerCommandSource src, int ticks, boolean stress) {
        MinecraftServer server = src.getServer();

        IngeniumDiagnostics.get().onServerStartThreadCaptured();

        // Snapshot A: Ingenium OFF
        boolean prevMaster = IngeniumConfig.getInstance().masterEnabled;
        IngeniumConfig.getInstance().masterEnabled = false;

        if (stress) StressWorkload.install(server, ticks);

        Snapshot off = collectWindow(server, ticks, "OFF", stress);

        // Snapshot B: Ingenium ON
        IngeniumConfig.getInstance().masterEnabled = true;

        collectWindow(server, Math.min(60, ticks / 4), "WARMUP_ON", false);

        if (stress) StressWorkload.install(server, ticks);

        Snapshot on = collectWindow(server, ticks, "ON", stress);

        // Restore
        IngeniumConfig.getInstance().masterEnabled = prevMaster;

        // Build report
        String report = buildReport(server, ticks, stress, off, on);

        String name = "ingenium-benchmark-" + LocalDateTime.now().format(TS) + ".txt";
        try {
            Path out = BenchmarkReportWriter.writeReport(name, report);
            src.sendFeedback(() -> Text.literal("[Ingenium] Benchmark written: " + out.toAbsolutePath()), false);
        } catch (Exception e) {
            src.sendError(Text.literal("[Ingenium] Failed writing report: " + e.getMessage()));
        }

        src.sendFeedback(() -> Text.literal(shortDelta(off, on)), false);

        return 1;
    }

    private static Snapshot collectWindow(MinecraftServer server, int ticks, String label, boolean stress) {
        IngeniumDiagnostics diag = IngeniumDiagnostics.get();
        // Instantaneous capture for now as commands block.
        return Snapshot.capture(label, stress, diag);
    }

    private static String buildReport(MinecraftServer server, int ticks, boolean stress, Snapshot off, Snapshot on) {
        IngeniumDiagnostics diag = IngeniumDiagnostics.get();

        StringBuilder sb = new StringBuilder(4096);
        sb.append("=== Ingenium Benchmark Report ===\n");
        sb.append("time=").append(LocalDateTime.now()).append("\n");
        sb.append("ticksRequested=").append(ticks).append("\n");
        sb.append("stress=").append(stress).append("\n");
        sb.append("mcTicksNow=").append(server.getTicks()).append("\n");
        sb.append("\n");

        sb.append("--- Snapshot OFF ---\n").append(off.format()).append("\n");
        sb.append("--- Snapshot ON ---\n").append(on.format()).append("\n");

        sb.append("--- Delta (ON - OFF) ---\n");
        sb.append(deltaSection(off, on)).append("\n");

        sb.append("--- Live System ---\n");
        sb.append(diag.governorSummary()).append("\n");
        sb.append(diag.asyncQueueSummary()).append("\n");
        sb.append(diag.wheelSummary()).append("\n");

        appendChunkLatencySection(sb, server);

        return sb.toString();
    }

    private static String deltaSection(Snapshot off, Snapshot on) {
        StringBuilder sb = new StringBuilder(1024);

        sb.append("lastTickMs: ").append(String.format("%.3f", on.lastTickMs - off.lastTickMs)).append("\n");
        sb.append("allocBytes(window): ").append(on.allocBytesWindow - off.allocBytesWindow).append("\n");
        sb.append("gcTimeDeltaMs(window): ").append(on.gcTimeMsWindow - off.gcTimeMsWindow).append("\n");
        sb.append("gcCountDelta(window): ").append(on.gcCountWindow - off.gcCountWindow).append("\n");
        sb.append("chunkLatencyAvgMs: ").append(String.format("%.3f", on.chunkLatencyAvgMs - off.chunkLatencyAvgMs)).append("\n");
        sb.append("chunkLatencyMaxMs: ").append(String.format("%.3f", on.chunkLatencyMaxMs - off.chunkLatencyMaxMs)).append("\n");
        sb.append("chunkMainWaitAvgMs: ").append(String.format("%.3f", on.chunkMainWaitAvgMs - off.chunkMainWaitAvgMs)).append("\n");
        sb.append("chunkMainWaitMaxMs: ").append(String.format("%.3f", on.chunkMainWaitMaxMs - off.chunkMainWaitMaxMs)).append("\n");

        return sb.toString();
    }

    private static String shortDelta(Snapshot off, Snapshot on) {
        double allocMb = (on.allocBytesWindow - off.allocBytesWindow) / (1024.0 * 1024.0);
        double gcMs = (on.gcTimeMsWindow - off.gcTimeMsWindow);
        return "[Ingenium] Δalloc=" + String.format("%.2f", allocMb) + "MB, Δgc=" + String.format("%.1f", gcMs) +
               "ms, ΔchunkAvg=" + String.format("%.2f", (on.chunkLatencyAvgMs - off.chunkLatencyAvgMs)) + "ms";
    }

    static void appendChunkLatencySection(StringBuilder out, MinecraftServer server) {
        ChunkLatencyMonitor.Snapshot snap =
                IngeniumBenchmarkService.get().getChunkLatency().snapshotAndReset();

        out.append("\n== Chunk Latency (request -> ready proxy) ==\n");
        if (snap.statsByDim.isEmpty()) {
            out.append("No samples captured in this window.\n");
            return;
        }

        for (Long2ObjectMap.Entry<ChunkLatencyMonitor.LatencyStats> e : snap.statsByDim.long2ObjectEntrySet()) {
            long dimId = e.getLongKey();
            ChunkLatencyMonitor.LatencyStats s = e.getValue();
            out.append("dimId=").append(dimId)
               .append(" samples=").append(s.samples)
               .append(" avgMs=").append(String.format("%.3f", s.avgMs()))
               .append(" maxMs=").append(String.format("%.3f", s.maxMs()))
               .append("\n");
        }
    }

    private record Snapshot(
            String label,
            boolean stress,
            double lastTickMs,
            long allocBytesWindow,
            long gcTimeMsWindow,
            long gcCountWindow,
            long chunkReq,
            long chunkReady,
            double chunkLatencyAvgMs,
            double chunkLatencyMaxMs,
            double chunkMainWaitAvgMs,
            double chunkMainWaitMaxMs,
            int commitQueueDepth
    ) {
        static Snapshot capture(String label, boolean stress, IngeniumDiagnostics d) {
            return new Snapshot(
                    label,
                    stress,
                    d.lastTickMs(),
                    d.allocBytesDeltaWindow(),
                    d.gcTimeDeltaWindowMs(),
                    d.gcCountDeltaWindow(),
                    d.chunkRequestCount(),
                    d.chunkReadyCount(),
                    d.chunkLatencyAvgMs(),
                    d.chunkLatencyMaxMs(),
                    d.chunkMainThreadWaitAvgMs(),
                    d.chunkMainThreadWaitMaxMs(),
                    IngeniumExecutors.commitQueueSize()
            );
        }

        String format() {
            StringBuilder sb = new StringBuilder(1024);
            sb.append("label=").append(label).append("\n");
            sb.append("stress=").append(stress).append("\n");
            sb.append("lastTickMs=").append(String.format("%.3f", lastTickMs)).append("\n");
            sb.append("allocBytesWindow=").append(allocBytesWindow).append("\n");
            sb.append("gcTimeMsWindow=").append(gcTimeMsWindow).append("\n");
            sb.append("gcCountWindow=").append(gcCountWindow).append("\n");
            sb.append("chunkReq=").append(chunkReq).append(" chunkReady=").append(chunkReady).append("\n");
            sb.append("chunkLatencyAvgMs=").append(String.format("%.3f", chunkLatencyAvgMs)).append("\n");
            sb.append("chunkLatencyMaxMs=").append(String.format("%.3f", chunkLatencyMaxMs)).append("\n");
            sb.append("chunkMainWaitAvgMs=").append(String.format("%.3f", chunkMainWaitAvgMs)).append("\n");
            sb.append("chunkMainWaitMaxMs=").append(String.format("%.3f", chunkMainWaitMaxMs)).append("\n");
            sb.append("commitQueueDepth=").append(commitQueueDepth).append("\n");
            return sb.toString();
        }
    }
}
