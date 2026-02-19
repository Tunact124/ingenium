package com.ingenium.memory;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.governor.OptimizationProfile;
import com.ingenium.governor.ProfileListener;
import com.ingenium.threading.IngeniumExecutors;
import com.ingenium.util.IngeniumLogger;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class GcHintScheduler implements ProfileListener {
 
    private static final List<GarbageCollectorMXBean> GC_BEANS =
        ManagementFactory.getGarbageCollectorMXBeans();
 
    private long            lastKnownGcCount  = 0;
    private ScheduledFuture<?> pending        = null;
 
    @Override
    public void onProfileChange(OptimizationProfile profile) {
        if (!IngeniumConfig.get().enableGcHints) return;
 
        if (profile == OptimizationProfile.AGGRESSIVE) {
            // Schedule GC hint 2 seconds after going idle.
            if (pending != null) pending.cancel(false);
            pending = IngeniumExecutors.SCHEDULER.schedule(this::maybeHint,
                2, TimeUnit.SECONDS);
        } else {
            // Cancel pending hint if server became active again.
            if (pending != null) { pending.cancel(false); pending = null; }
        }
    }
 
    private void maybeHint() {
        long currentGcCount = GC_BEANS.stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
 
        if (currentGcCount > lastKnownGcCount + 2) {
            // JVM already GC'd twice since last check. Skip redundant hint.
            lastKnownGcCount = currentGcCount;
            IngeniumLogger.debug("GC hint skipped — JVM already collected.");
            return;
        }
 
        System.gc(); // hint — JVM may ignore, but usually honors during idle
        lastKnownGcCount = currentGcCount;
        IngeniumLogger.debug("Proactive GC hint issued.");
    }
}
