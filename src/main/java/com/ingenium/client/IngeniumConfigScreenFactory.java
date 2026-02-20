package com.ingenium.client;

import com.ingenium.benchmark.IngeniumDiagnostics;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Stub factory for the config screen.
 */
public final class IngeniumConfigScreenFactory {
    private IngeniumConfigScreenFactory() {}

    public static Screen create(Screen parent) {
        // Replace with YACL/Cloth screen if desired.
        return parent;
    }

    /** Tooltip that includes estimated impact + latest observed diagnostic hints. */
    public static Text tooltipWithImpact(String base, String impactClass) {
        IngeniumDiagnostics d = IngeniumDiagnostics.get();

        String observed =
                "\n\nObserved (last window):" +
                "\n- server alloc bytes(window): " + d.allocBytesDeltaWindow() +
                "\n- GC time delta(window ms): " + d.gcTimeDeltaWindowMs() +
                "\n- chunk latency avg(ms): " + String.format("%.2f", d.chunkLatencyAvgMs()) +
                "\n- wheel: " + d.wheelSummary().trim();

        String text = base +
                "\n\n" + impactClass +
                observed;

        return Text.literal(text);
    }

    // Tooltip strings (UI uses these verbatim)
    public static String tooltipScheduledWheel() {
        return "Scheduled Tick Bucketing: Replaces vanilla's O(log n) tree queue with O(1) hierarchical timing wheels. " +
                "Reduces redstone/farm lag but may alter precise tick ordering in extreme contraptions. [Default: ON]";
    }

    public static String tooltipOffHeap() {
        return "Off-Heap Block Entity Cache: Moves tile entity data to native memory (outside JVM heap). " +
                "Reduces GC stutter but increases native RAM usage. Requires Java 19+ for MemorySegment (preferred) " +
                "or uses DirectByteBuffer fallback. [Default: OFF]";
    }

    public static String tooltipEmergencyThreshold() {
        return "Governor Emergency Threshold: MSPT value at which non-critical block entities begin skipping ticks. " +
                "[Default: 45ms, Range: 20-100]";
    }
}
