package com.ingenium.offheap;

import com.ingenium.core.VectorGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;

/**
 * NioGpuStreamManager
 *
 * Streams chunk data from native memory slabs directly to GPU-bound buffers
 * using Java NIO DirectByteBuffers. Completely bypasses the JVM heap.
 *
 * <p>
 * Architecture: Manages a pool of reusable off-heap slabs. Each slab is a
 * DirectByteBuffer allocated once and recycled via
 * {@link #releaseSlab(ByteBuffer)}.
 * When Java 22+ FFM API becomes available at the build target, this class will
 * be upgraded to use {@code java.lang.foreign.Arena} and {@code MemorySegment}
 * for tighter lifecycle control. Until then, DirectByteBuffer + Cleaner is the
 * correct Java 17 approach.
 *
 * <p>
 * Thread safety: Not thread-safe. Intended for server-thread-only use,
 * mirroring {@link OffHeapBlockEntityStore}.
 */
public final class NioGpuStreamManager {
    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/NioStream");

    /** Default slab size: 256KB — large enough for a chunk column's block data. */
    private static final int DEFAULT_SLAB_BYTES = 256 * 1024;

    /** Maximum slabs to keep pooled. Beyond this, slabs are abandoned to GC. */
    private static final int MAX_POOLED_SLABS = 16;

    /** Alignment for slab allocations (cache-line aligned for GPU transfer). */
    private static final int SLAB_ALIGNMENT = 64;

    private final ArrayDeque<ByteBuffer> slabPool = new ArrayDeque<>(MAX_POOLED_SLABS);
    private final int slabSizeBytes;

    private long totalAllocated;
    private long totalRecycled;
    private boolean initialized;

    public NioGpuStreamManager() {
        this(DEFAULT_SLAB_BYTES);
    }

    public NioGpuStreamManager(int slabSizeBytes) {
        if (slabSizeBytes <= 0) {
            throw new IllegalArgumentException("Slab size must be positive, got: " + slabSizeBytes);
        }
        this.slabSizeBytes = slabSizeBytes;
    }

    /**
     * Initialize the streaming pipeline. Safe to call multiple times.
     */
    public void initialize() {
        if (initialized)
            return;

        LOG.info("[Ingenium/NioStream] Initialized off-heap streaming pipeline " +
                "(slabSize={}KB, maxPooled={})", slabSizeBytes / 1024, MAX_POOLED_SLABS);
        initialized = true;
    }

    /**
     * Acquire an off-heap slab for chunk data. Reuses pooled slabs when available.
     *
     * @return a cleared DirectByteBuffer with native byte order, or null if not
     *         initialized
     */
    public ByteBuffer acquireSlab() {
        if (!initialized)
            return null;

        ByteBuffer recycled = slabPool.pollFirst();
        if (recycled != null) {
            recycled.clear();
            totalRecycled++;
            return recycled;
        }

        // Allocate fresh off-heap slab
        ByteBuffer slab = ByteBuffer.allocateDirect(slabSizeBytes).order(ByteOrder.nativeOrder());
        totalAllocated++;

        LOG.debug("[Ingenium/NioStream] Allocated new {}KB slab (total allocated: {})",
                slabSizeBytes / 1024, totalAllocated);
        return slab;
    }

    /**
     * Acquire an off-heap slab of a specific size. Does NOT use the pool
     * (pool only stores standard-sized slabs).
     */
    public ByteBuffer acquireSlab(int sizeBytes) {
        if (!initialized)
            return null;
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("Size must be positive, got: " + sizeBytes);
        }

        // If requested size matches default, use the pool path
        if (sizeBytes == slabSizeBytes)
            return acquireSlab();

        totalAllocated++;
        return ByteBuffer.allocateDirect(sizeBytes).order(ByteOrder.nativeOrder());
    }

    /**
     * Return a slab to the pool for reuse. Only standard-sized slabs are pooled.
     */
    public void releaseSlab(ByteBuffer slab) {
        if (slab == null || !slab.isDirect())
            return;

        if (slab.capacity() == slabSizeBytes && slabPool.size() < MAX_POOLED_SLABS) {
            slab.clear();
            slabPool.offerFirst(slab);
        }
        // Non-standard sizes or full pool: GC handles it via Cleaner
    }

    /**
     * Transfer slab contents to a GPU-bound buffer.
     * Placeholder: actual Vulkan buffer binding will be implemented when the
     * Vulkan pipeline (VulkanFeatureManager) is production-ready.
     *
     * @param chunkData the source slab containing raw chunk data
     * @param gpuTarget the destination buffer (e.g., Vulkan staging buffer)
     */
    public void streamToGpuBuffer(ByteBuffer chunkData, ByteBuffer gpuTarget) {
        if (chunkData == null || gpuTarget == null)
            return;
        if (!chunkData.isDirect() || !gpuTarget.isDirect()) {
            throw new IllegalArgumentException("Both buffers must be direct for zero-copy transfer");
        }

        chunkData.flip();
        gpuTarget.put(chunkData);
    }

    /**
     * Release all pooled slabs. Called on shutdown or dimension unload.
     */
    public void shutdown() {
        int pooledCount = slabPool.size();
        slabPool.clear();
        initialized = false;

        LOG.info("[Ingenium/NioStream] Shutdown. Released {} pooled slabs. " +
                "Lifetime stats: {} allocated, {} recycled",
                pooledCount, totalAllocated, totalRecycled);
    }

    // --- Telemetry ---

    public long totalAllocated() {
        return totalAllocated;
    }

    public long totalRecycled() {
        return totalRecycled;
    }

    public int pooledCount() {
        return slabPool.size();
    }

    public boolean isInitialized() {
        return initialized;
    }
}
