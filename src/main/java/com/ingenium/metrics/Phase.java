package com.ingenium.metrics;

/**
 * Phases we may measure. Some are client-specific, some server-specific.
 * We keep one unified enum so storage and reporting stay stable across sides.
 */
public enum Phase {
    // Server-side phases
    WORLD_TICK,
    ENTITY_TICK,
    BLOCK_ENTITY_TICK,
    CHUNK_GEN,
    SCHEDULED_TICKS_DRAIN,

    // Client-side phases
    RENDER_BUILD,
    RENDER_SUBMIT,
    CULL_PASS,

    // Unified/common
    MSPT_TOTAL
}
