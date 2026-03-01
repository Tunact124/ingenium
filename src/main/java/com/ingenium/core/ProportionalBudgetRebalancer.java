package com.ingenium.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proportional Budget Rebalancer (Phase 6C)
 *
 * <p>
 * A simple proportional feedback controller that shifts budget from
 * underutilized subsystems to overutilized ones. This is the "first milestone"
 * of adaptive budget allocation — no ML, just measured utilization.
 *
 * <p>
 * Key insight: if Block Entities consistently finish in 40% of their budget
 * while World Ticks overflow, we're wasting 60% of the BE allocation.
 *
 * <p>
 * Safety bounds prevent any subsystem from going below 10% or above 50%
 * of the total tick budget, preventing pathological states.
 */
public final class ProportionalBudgetRebalancer {

    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/Budget");

    /** Rebalance every 200 ticks (10 seconds). */
    private static final int REBALANCE_INTERVAL = 200;

    /** Maximum fraction shift per cycle — prevents oscillation. */
    private static final float MAX_SHIFT_PER_CYCLE = 0.03f;

    /** Hard bounds: no subsystem goes below 10% or above 50%. */
    private static final float MIN_FRACTION = 0.10f;
    private static final float MAX_FRACTION = 0.50f;

    /** Utilization above this triggers "needs more" pressure. */
    private static final float HIGH_UTILIZATION = 0.90f;

    /** Utilization below this triggers "has surplus" pressure. */
    private static final float LOW_UTILIZATION = 0.50f;

    private final int subsystemCount;
    private final float[] fractions;

    // Accumulated usage tracking per rebalance cycle
    private final long[] totalUsedNs;
    private final long[] totalAllocatedNs;
    private int ticksSinceRebalance;

    public ProportionalBudgetRebalancer(int subsystemCount, float[] defaultFractions) {
        if (subsystemCount != defaultFractions.length) {
            throw new IllegalArgumentException(
                    "Subsystem count (" + subsystemCount + ") != fractions length (" + defaultFractions.length + ")");
        }
        this.subsystemCount = subsystemCount;
        this.fractions = defaultFractions.clone();
        this.totalUsedNs = new long[subsystemCount];
        this.totalAllocatedNs = new long[subsystemCount];
    }

    /** Record how much budget was allocated and used for a subsystem this tick. */
    public void recordUsage(int index, long allocatedNs, long usedNs) {
        totalAllocatedNs[index] += allocatedNs;
        totalUsedNs[index] += Math.min(usedNs, allocatedNs);
    }

    /** Called once per tick. Triggers rebalance every REBALANCE_INTERVAL ticks. */
    public void tick() {
        if (++ticksSinceRebalance < REBALANCE_INTERVAL)
            return;
        ticksSinceRebalance = 0;
        rebalance();
        java.util.Arrays.fill(totalUsedNs, 0);
        java.util.Arrays.fill(totalAllocatedNs, 0);
    }

    /** Get the current budget fraction for a subsystem. */
    public float getFraction(int index) {
        return fractions[index];
    }

    // --- Rebalancing logic ---

    private void rebalance() {
        float[] utilization = new float[subsystemCount];
        float[] pressure = new float[subsystemCount];
        float totalSurplus = 0;
        float totalDemand = 0;

        for (int i = 0; i < subsystemCount; i++) {
            utilization[i] = totalAllocatedNs[i] > 0
                    ? (float) totalUsedNs[i] / totalAllocatedNs[i]
                    : 0f;

            if (utilization[i] > HIGH_UTILIZATION) {
                pressure[i] = (utilization[i] - HIGH_UTILIZATION) * 10f;
                totalDemand += pressure[i];
            } else if (utilization[i] < LOW_UTILIZATION) {
                pressure[i] = -(LOW_UTILIZATION - utilization[i]) * 2f;
                totalSurplus += -pressure[i];
            }
        }

        if (totalSurplus < 0.01f || totalDemand < 0.01f)
            return;

        float transferable = Math.min(Math.min(totalSurplus, totalDemand), MAX_SHIFT_PER_CYCLE);
        float[] updated = fractions.clone();

        for (int i = 0; i < subsystemCount; i++) {
            if (pressure[i] < 0 && totalSurplus > 0) {
                updated[i] -= transferable * (-pressure[i] / totalSurplus);
            } else if (pressure[i] > 0 && totalDemand > 0) {
                updated[i] += transferable * (pressure[i] / totalDemand);
            }
            updated[i] = Math.max(MIN_FRACTION, Math.min(MAX_FRACTION, updated[i]));
        }

        // Normalize to sum to 1.0
        float sum = 0;
        for (float f : updated)
            sum += f;
        if (sum > 0) {
            for (int i = 0; i < subsystemCount; i++) {
                updated[i] /= sum;
            }
        }

        // Apply and log if changed
        boolean changed = false;
        for (int i = 0; i < subsystemCount; i++) {
            if (Math.abs(updated[i] - fractions[i]) > 0.001f)
                changed = true;
            fractions[i] = updated[i];
        }

        if (changed) {
            var sb = new StringBuilder("Rebalanced: ");
            for (int i = 0; i < subsystemCount; i++) {
                sb.append(String.format("[%d]=%.1f%%(util:%.0f%%) ", i, fractions[i] * 100, utilization[i] * 100));
            }
            LOG.info(sb.toString().trim());
        }
    }
}
