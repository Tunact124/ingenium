package com.ingenium.core;

/**
 * GovernorBudgetProfile defines token limits per budget epoch.
 * Used by the Governor to gate visual optimizations.
 */
public record GovernorBudgetProfile(
        int entityBackfaceCullPerFrame,
        int itemFastPathPerFrame,
        int entityBackfaceCullPerFrameEmergency,
        int itemFastPathPerFrameEmergency
) {
    public static GovernorBudgetProfile defaults() {
        return new GovernorBudgetProfile(
                8192,
                4096,
                2048,
                1024
        );
    }
}
