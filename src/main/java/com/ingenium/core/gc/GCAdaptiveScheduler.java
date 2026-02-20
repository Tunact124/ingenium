package com.ingenium.core.gc;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.IngeniumGovernor;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

public final class GCAdaptiveScheduler {
    private static final GCAdaptiveScheduler INSTANCE = new GCAdaptiveScheduler();

    public static GCAdaptiveScheduler get() { return INSTANCE; }

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    private long lastYoungGcCount = -1;
    private long lastYoungGcTimeMs = -1;

    private GCAdaptiveScheduler() {}

    public void onTickStart(IngeniumGovernor governor) {
        if (!IngeniumConfig.get().enableGcCoordination) return;

        detectYoungGcEvents();

        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        double occ = (double) heap.getUsed() / (double) heap.getMax();

        // Conservative: no forced GC. We let Governor's GC pre-emption do the work.
        // If you later allow optional explicit GC hints, gate behind config + JVM flags.
        if (occ > 0.90 && governor.profile() == IngeniumGovernor.OptimizationProfile.EMERGENCY) {
            // Intentionally no-op by default.
        }
    }

    private void detectYoungGcEvents() {
        for (GarbageCollectorMXBean gc : gcBeans) {
            String n = gc.getName().toLowerCase();
            if (!n.contains("young") && !n.contains("minor")) continue;

            long c = gc.getCollectionCount();
            long t = gc.getCollectionTime();
            if (lastYoungGcCount < 0) {
                lastYoungGcCount = c;
                lastYoungGcTimeMs = t;
            } else if (c > lastYoungGcCount) {
                lastYoungGcCount = c;
                lastYoungGcTimeMs = t;
            }
            return;
        }
    }
}
