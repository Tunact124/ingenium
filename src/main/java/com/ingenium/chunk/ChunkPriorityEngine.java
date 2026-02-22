package com.ingenium.chunk;

import com.ingenium.core.IngeniumGovernor;
import com.ingenium.core.IngeniumGovernor.SubsystemType;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Adaptive Chunk Prioritization Engine
 *
 * Assigns priority scores to pending chunk operations based on:
 * 1. Direction: Is the chunk in the player's look direction?
 * 2. Distance: How far is it from the player?
 * 3. Velocity: Is the player moving toward it?
 * 4. Staleness: How long has it been waiting?
 *
 * The Governor controls how many chunks get processed per tick
 * based on available MSPT budget.
 */
public class ChunkPriorityEngine {

    // Tuning constants — derived from profiling
    private static final float DIRECTION_WEIGHT = 3.0f;
    private static final float DISTANCE_WEIGHT = 2.0f;
    private static final float VELOCITY_WEIGHT = 1.5f;
    private static final float STALENESS_WEIGHT = 0.5f;
    private static final float STALENESS_ESCALATION_TICKS = 200;

    private final IngeniumGovernor governor;
    private final PriorityQueue<ScoredChunk> priorityQueue;

    // Player state cache — updated once per tick, not per chunk
    private Vec3 playerPos = Vec3.ZERO;
    private Vec3 playerLook = Vec3.ZERO;
    private Vec3 playerVelocity = Vec3.ZERO;
    private long currentTick = 0;

    public ChunkPriorityEngine(IngeniumGovernor governor) {
        this.governor = governor;
        this.priorityQueue = new PriorityQueue<>(
            256,
            Comparator.comparingDouble(ScoredChunk::score).reversed()
        );
    }

    /**
     * Called once at the start of each server tick.
     * Caches player state so we don't recompute it per chunk.
     */
    public void updatePlayerState(Vec3 pos, Vec3 look, Vec3 velocity, long tick) {
        this.playerPos = pos;
        this.playerLook = look.normalize();
        this.playerVelocity = velocity;
        this.currentTick = tick;
    }

    /**
     * Submit a chunk section that needs work (rebuild, light update, etc).
     * The engine scores it and inserts it into the priority queue.
     */
    public void submit(SectionPos section, ChunkWorkType workType, long submittedTick) {
        float score = scoreChunk(section, workType, submittedTick);
        priorityQueue.offer(new ScoredChunk(section, workType, score, submittedTick));
    }

    /**
     * Drain the highest-priority chunks that fit within the Governor's budget.
     * Returns the number of chunks released for processing.
     */
    public int drainToBudget(ChunkWorkConsumer consumer) {
        int processed = 0;

        // Ask Governor how much budget we have for chunk work this tick
        long budgetNs = governor.getBudgetForSubsystem(SubsystemType.CHUNK_SCHEDULING);
        long startNs = System.nanoTime();

        while (!priorityQueue.isEmpty()) {
            long elapsed = System.nanoTime() - startNs;
            if (elapsed >= budgetNs) break;

            ScoredChunk top = priorityQueue.poll();
            if (top == null) break;

            // Re-score stale entries — if it's been sitting for a while,
            // its priority may have changed dramatically
            if (currentTick - top.submittedTick() > 20) {
                float newScore = scoreChunk(
                    top.section(), top.workType(), top.submittedTick()
                );
                // If re-scored priority dropped significantly, re-queue
                if (newScore < top.score() * 0.5f && !priorityQueue.isEmpty()) {
                    priorityQueue.offer(new ScoredChunk(
                        top.section(), top.workType(), newScore, top.submittedTick()
                    ));
                    continue;
                }
            }

            consumer.accept(top.section(), top.workType());
            processed++;
        }

        return processed;
    }

    /**
     * Core scoring function.
     *
     * Score = (direction × W_d) + (proximity × W_p) +
     *         (velocity_toward × W_v) + (staleness × W_s)
     *
     * All components are normalized to [0, 1] before weighting.
     */
    private float scoreChunk(SectionPos section, ChunkWorkType workType, long submittedTick) {
        // Center of the chunk section in world coordinates
        double cx = SectionPos.sectionToBlockCoord(section.x()) + 8.0;
        double cy = SectionPos.sectionToBlockCoord(section.y()) + 8.0;
        double cz = SectionPos.sectionToBlockCoord(section.z()) + 8.0;

        // Vector from player to chunk center
        double dx = cx - playerPos.x;
        double dy = cy - playerPos.y;
        double dz = cz - playerPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        double dist = Math.sqrt(distSq);

        if (dist < 0.01) return Float.MAX_VALUE; // Player is inside this chunk

        // 1. DIRECTION SCORE: dot product of look vector and chunk direction
        //    Range: [-1, 1] → normalized to [0, 1]
        double invDist = 1.0 / dist;
        double dirX = dx * invDist;
        double dirY = dy * invDist;
        double dirZ = dz * invDist;
        double dot = dirX * playerLook.x + dirY * playerLook.y + dirZ * playerLook.z;
        float directionScore = (float)((dot + 1.0) * 0.5); // [0, 1]

        // 2. PROXIMITY SCORE: closer chunks score higher
        //    Use inverse square falloff, capped at render distance
        float maxDist = 16.0f * 16.0f; // 16 chunks × 16 blocks
        float proximityScore = 1.0f - Math.min((float)(distSq / (maxDist * maxDist * 256.0)), 1.0f);

        // 3. VELOCITY SCORE: is the player moving toward this chunk?
        //    dot(velocity, direction_to_chunk)
        double velDot = playerVelocity.x * dirX +
                        playerVelocity.y * dirY +
                        playerVelocity.z * dirZ;
        float velocityScore = (float) Math.max(0.0, Math.min(velDot / 0.5, 1.0));

        // 4. STALENESS SCORE: chunks waiting too long get escalated
        //    Prevents starvation — even low-priority chunks eventually process
        long waitTicks = currentTick - submittedTick;
        float stalenessScore = Math.min(
            (float) waitTicks / STALENESS_ESCALATION_TICKS, 1.0f
        );

        // 5. WORK TYPE MULTIPLIER: some operations are more urgent
        float typeMultiplier = workType.urgencyMultiplier();

        // Weighted sum
        float score = (directionScore * DIRECTION_WEIGHT) +
                      (proximityScore * DISTANCE_WEIGHT) +
                      (velocityScore * VELOCITY_WEIGHT) +
                      (stalenessScore * STALENESS_WEIGHT);

        return score * typeMultiplier;
    }

    /**
     * Emergency drain — Governor is in EMERGENCY profile,
     * only process the absolute highest priority chunk.
     */
    public void emergencyDrain(ChunkWorkConsumer consumer) {
        if (!priorityQueue.isEmpty()) {
            ScoredChunk top = priorityQueue.poll();
            if (top != null) {
                consumer.accept(top.section(), top.workType());
            }
        }
    }

    /**
     * Clear all pending work — called on dimension change or disconnect
     */
    public void clear() {
        priorityQueue.clear();
    }

    public int pendingCount() {
        return priorityQueue.size();
    }

    // ================================================================
    // Inner types
    // ================================================================

    public record ScoredChunk(
        SectionPos section,
        ChunkWorkType workType,
        float score,
        long submittedTick
    ) {}

    public enum ChunkWorkType {
        BLOCK_CHANGE(2.0f),       // Player-triggered, highest urgency
        LIGHT_UPDATE(1.5f),       // Visual artifact if delayed
        MESH_REBUILD(1.0f),       // Standard rebuild
        ENTITY_CHANGE(0.8f),      // Entity moved in/out of section
        SCHEDULED_TICK(0.5f);     // Background maintenance

        private final float urgencyMultiplier;

        ChunkWorkType(float urgencyMultiplier) {
            this.urgencyMultiplier = urgencyMultiplier;
        }

        public float urgencyMultiplier() {
            return urgencyMultiplier;
        }
    }

    @FunctionalInterface
    public interface ChunkWorkConsumer {
        void accept(SectionPos section, ChunkWorkType workType);
    }
}
