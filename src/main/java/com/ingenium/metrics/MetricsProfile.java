package com.ingenium.metrics;

import net.fabricmc.api.EnvType;

public final class MetricsProfile {
    private MetricsProfile() {}

    public static void configureForEnv(EnvType env, PhaseTimings timings) {
        // Always enable unified MSPT_TOTAL
        timings.setEnabled(Phase.MSPT_TOTAL, true);

        if (env == EnvType.SERVER) {
            timings.setEnabled(Phase.WORLD_TICK, true);
            timings.setEnabled(Phase.ENTITY_TICK, true);
            timings.setEnabled(Phase.BLOCK_ENTITY_TICK, true);
            timings.setEnabled(Phase.CHUNK_GEN, true);
            timings.setEnabled(Phase.SCHEDULED_TICKS_DRAIN, true);

            // Client phases off
            timings.setEnabled(Phase.RENDER_BUILD, false);
            timings.setEnabled(Phase.RENDER_SUBMIT, false);
            timings.setEnabled(Phase.CULL_PASS, false);
        } else {
            // EnvType.CLIENT
            timings.setEnabled(Phase.RENDER_BUILD, true);
            timings.setEnabled(Phase.RENDER_SUBMIT, true);
            timings.setEnabled(Phase.CULL_PASS, true);

            // Server phases can be off on pure client, but you can keep WORLD_TICK on if desired.
            timings.setEnabled(Phase.WORLD_TICK, true); // useful even on integrated server
            timings.setEnabled(Phase.ENTITY_TICK, false);
            timings.setEnabled(Phase.BLOCK_ENTITY_TICK, false);
            timings.setEnabled(Phase.CHUNK_GEN, false);
            timings.setEnabled(Phase.SCHEDULED_TICKS_DRAIN, false);
        }
    }
}
