package com.ingenium.render;

import com.ingenium.core.IngeniumGovernor;
import com.ingenium.core.IngeniumGovernor.OptimizationProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamically adjusts effective render distance based on server performance.
 */
public final class AdaptiveRenderDistance {

    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/RenderDist");

    private static final double REDUCE_THRESHOLD_MS = 45.0;
    private static final double RECOVER_THRESHOLD_MS = 35.0;
    private static final double EMERGENCY_THRESHOLD_MS = 48.0;

    private static final int REDUCE_WINDOW_TICKS = 100;
    private static final int RECOVER_WINDOW_TICKS = 200;
    private static final int STEP_COOLDOWN_TICKS = 40;

    private static final int MIN_RENDER_DISTANCE = 6;
    private static final int MAX_REDUCTION_CHUNKS = 6;

    private enum State {
        STABLE, REDUCING, REDUCED, RECOVERING
    }

    private State state = State.STABLE;
    private int currentReduction = 0;
    private int configuredDistance;

    private int ticksAboveThreshold = 0;
    private int ticksBelowThreshold = 0;
    private int ticksSinceLastStep = 0;

    private final double[] msptHistory = new double[20];
    private int msptHistoryIndex = 0;
    private boolean msptHistoryFull = false;

    private boolean enabled = true;

    public AdaptiveRenderDistance(int configuredDistance) {
        this.configuredDistance = configuredDistance;
    }

    public int tick(double currentMspt) {
        if (!enabled) return configuredDistance;

        msptHistory[msptHistoryIndex] = currentMspt;
        msptHistoryIndex = (msptHistoryIndex + 1) % msptHistory.length;
        if (msptHistoryIndex == 0) msptHistoryFull = true;

        double avgMspt = getAverageMspt();
        ticksSinceLastStep++;

        switch (state) {
            case STABLE -> tickStable(avgMspt);
            case REDUCING -> tickReducing(avgMspt);
            case REDUCED -> tickReduced(avgMspt);
            case RECOVERING -> tickRecovering(avgMspt);
        }

        return getEffectiveDistance();
    }

    private void tickStable(double avgMspt) {
        if (avgMspt >= EMERGENCY_THRESHOLD_MS) {
            ticksAboveThreshold += 3;
        } else if (avgMspt >= REDUCE_THRESHOLD_MS) {
            ticksAboveThreshold++;
        } else {
            ticksAboveThreshold = Math.max(0, ticksAboveThreshold - 1);
        }

        if (ticksAboveThreshold >= REDUCE_WINDOW_TICKS) {
            state = State.REDUCING;
            ticksAboveThreshold = 0;
            ticksSinceLastStep = 0;
            LOG.info("[Ingenium] Render distance: STABLE → REDUCING (avg MSPT: {}ms)", avgMspt);
        }
    }

    private void tickReducing(double avgMspt) {
        if (ticksSinceLastStep >= STEP_COOLDOWN_TICKS) {
            if (currentReduction < MAX_REDUCTION_CHUNKS 
                    && getEffectiveDistance() > MIN_RENDER_DISTANCE) {
                currentReduction++;
                ticksSinceLastStep = 0;
                LOG.info("[Ingenium] Render distance: {} → {} (reduction step, MSPT: {}ms)",
                         configuredDistance - currentReduction + 1,
                         getEffectiveDistance(), avgMspt);
            }
        }

        if (avgMspt < RECOVER_THRESHOLD_MS) {
            state = State.REDUCED;
            ticksBelowThreshold = 0;
            LOG.info("[Ingenium] Render distance: REDUCING → REDUCED at {} chunks", getEffectiveDistance());
        }

        if (currentReduction >= MAX_REDUCTION_CHUNKS 
                || getEffectiveDistance() <= MIN_RENDER_DISTANCE) {
            state = State.REDUCED;
            ticksBelowThreshold = 0;
        }
    }

    private void tickReduced(double avgMspt) {
        if (avgMspt >= REDUCE_THRESHOLD_MS) {
            ticksBelowThreshold = 0;
            if (currentReduction < MAX_REDUCTION_CHUNKS) {
                state = State.REDUCING;
            }
            return;
        }

        if (avgMspt < RECOVER_THRESHOLD_MS) {
            ticksBelowThreshold++;
        } else {
            ticksBelowThreshold = Math.max(0, ticksBelowThreshold - 1);
        }

        if (ticksBelowThreshold >= RECOVER_WINDOW_TICKS) {
            state = State.RECOVERING;
            ticksBelowThreshold = 0;
            ticksSinceLastStep = 0;
            LOG.info("[Ingenium] Render distance: REDUCED → RECOVERING");
        }
    }

    private void tickRecovering(double avgMspt) {
        if (avgMspt >= REDUCE_THRESHOLD_MS) {
            state = State.REDUCED;
            ticksBelowThreshold = 0;
            LOG.info("[Ingenium] Render distance: RECOVERING → REDUCED (load spike)");
            return;
        }

        if (ticksSinceLastStep >= STEP_COOLDOWN_TICKS && currentReduction > 0) {
            currentReduction--;
            ticksSinceLastStep = 0;
            LOG.info("[Ingenium] Render distance: {} → {} (recovery step, MSPT: {}ms)",
                     getEffectiveDistance() - 1, getEffectiveDistance(), avgMspt);
        }

        if (currentReduction == 0) {
            state = State.STABLE;
            LOG.info("[Ingenium] Render distance: RECOVERING → STABLE (fully restored)");
        }
    }

    public int getEffectiveDistance() {
        return Math.max(MIN_RENDER_DISTANCE, configuredDistance - currentReduction);
    }

    private double getAverageMspt() {
        int count = msptHistoryFull ? msptHistory.length : msptHistoryIndex;
        if (count == 0) return 0;
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += msptHistory[i];
        }
        return sum / count;
    }

    public void setConfiguredDistance(int distance) {
        this.configuredDistance = distance;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
