package com.ingenium.be;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.IngeniumGovernor;
import com.ingenium.core.IngeniumGovernor.SubsystemType;

/**
 * Decides whether a block entity should tick on a given world tick.
 *
 * <p>Design:
 * - Always tick near players (critical radius).
 * - Otherwise tick every N ticks (divisor) based on OptimizationProfile.
 * - Enforce maxSkip as a safety net (prevents indefinite starvation).
 * - Optional governor budget integration (fast path: skip budget checks if disabled).
 */
public final class BlockEntityThrottlePolicy {

    private BlockEntityThrottlePolicy() {}

    /** Minimal data needed for a decision. Keeps policy testable. */
    public record Inputs(
            long worldTime,
            int squaredDistanceToNearestPlayer,
            int criticalRadiusSq,
            int divisor,
            int maxSkip,
            int currentSkipCount
    ) {}

    public enum DecisionType { TICK, SKIP }

    public record Decision(DecisionType type, String reason) {
        public static Decision tick(String reason) { return new Decision(DecisionType.TICK, reason); }
        public static Decision skip(String reason) { return new Decision(DecisionType.SKIP, reason); }
    }

    /**
     * Computes tick/skip decision.
     */
    public static Decision shouldTick(Inputs in) {
        // High-level feature flag belongs to the service; keep this pure.
        if (in.squaredDistanceToNearestPlayer() <= in.criticalRadiusSq()) {
            return Decision.tick("near_player");
        }

        // Safety: prevent starvation
        if (in.currentSkipCount() >= in.maxSkip()) {
            return Decision.tick("max_skip_reached");
        }

        int div = Math.max(1, in.divisor());
        if ((in.worldTime() % div) == 0) {
            return Decision.tick("divisor_hit");
        }
        return Decision.skip("divisor_miss");
    }

    /**
     * Optional: governor budget consumption. Keep outside core decision so it can be toggled independently.
     *
     * @return true if allowed to tick under budget constraints.
     */
    public static boolean allowUnderBudget(long estimatedCostNs) {
        // If governor budgets are disabled in config, treat as allowed.
        IngeniumConfig cfg = IngeniumConfig.get();
        if (cfg == null) return true;
        // No dedicated switch yet; rely on subsystem enablement defaults.
        return IngeniumGovernor.get().consumeBudget(SubsystemType.BLOCK_ENTITIES, estimatedCostNs);
    }
}
