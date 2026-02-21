package com.ingenium.core;

import com.ingenium.metrics.IngeniumMetrics;
import com.ingenium.core.hw.HardwareProfile;
import com.ingenium.tick.HopperThrottler;
import com.ingenium.world.PoiQueryCache;
import net.fabricmc.api.EnvType;

/**
 * Single runtime container so everything has one place to access shared services.
 * Keep it side-neutral: do not reference client-only MC classes here.
 */
public final class IngeniumRuntime {
    private final EnvType env;
    private final IngeniumMetrics metrics;
    private final HardwareProfile hardware;
    private final IngeniumGovernor governor;
    private final HopperThrottler hopperThrottler;
    private final PoiQueryCache poiQueryCache;

    public IngeniumRuntime(EnvType env, IngeniumMetrics metrics, HardwareProfile hardware, IngeniumGovernor governor) {
        this.env = env;
        this.metrics = metrics;
        this.hardware = hardware;
        this.governor = governor;
        this.hopperThrottler = new HopperThrottler();
        this.poiQueryCache = new PoiQueryCache();
    }

    public EnvType env() {
        return env;
    }

    public IngeniumMetrics metrics() {
        return metrics;
    }

    public HardwareProfile hardware() {
        return hardware;
    }

    public IngeniumGovernor governor() {
        return governor;
    }

    public HopperThrottler hopperThrottler() {
        return hopperThrottler;
    }

    public PoiQueryCache poiQueryCache() {
        return poiQueryCache;
    }
}
