package com.ingenium.chunk.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Off-Heap Buffer Pool for Chunk I/O (Phase 6B)
 *
 * <p>
 * Vanilla allocates a new {@code byte[]} for every chunk
 * compression/decompression.
 * These arrays are typically 4KB–64KB and become garbage immediately after use.
 * During fast travel, hundreds are allocated per second, driving GC pauses.
 *
 * <p>
 * This pool maintains reusable {@link ByteBuffer#allocateDirect
 * DirectByteBuffers}
 * (off-heap) that avoid: allocation cost, GC scanning, and copy overhead when
 * used with NIO channels.
 *
 * <p>
 * Thread-safe: chunk I/O happens on multiple threads (especially with C2ME).
 */
public final class ChunkIOBufferPool {

    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/ChunkIO");

    private static final int SMALL_SIZE = 4 * 1024;
    private static final int MEDIUM_SIZE = 16 * 1024;
    private static final int LARGE_SIZE = 64 * 1024;
    private static final int XLARGE_SIZE = 256 * 1024;

    /** Maximum buffers to keep per size tier. */
    private static final int MAX_POOLED_PER_TIER = 32;

    private final ConcurrentLinkedDeque<ByteBuffer> smallPool = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<ByteBuffer> mediumPool = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<ByteBuffer> largePool = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<ByteBuffer> xlargePool = new ConcurrentLinkedDeque<>();

    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicInteger misses = new AtomicInteger();
    private final AtomicInteger totalAllocatedBytes = new AtomicInteger();

    /**
     * Acquire a buffer of at least {@code minSize} bytes.
     * Returns a cleared DirectByteBuffer with position=0 and limit=capacity.
     */
    public ByteBuffer acquire(int minSize) {
        ConcurrentLinkedDeque<ByteBuffer> pool = poolForSize(minSize);

        if (pool != null) {
            ByteBuffer buffer = pool.pollFirst();
            if (buffer != null) {
                buffer.clear();
                hits.incrementAndGet();
                return buffer;
            }
        }

        // Pool miss — allocate new
        misses.incrementAndGet();
        int allocSize = roundUpToTier(minSize);
        totalAllocatedBytes.addAndGet(allocSize);

        LOG.debug("Allocated new {}KB buffer (total: {}KB)",
                allocSize / 1024, totalAllocatedBytes.get() / 1024);

        return ByteBuffer.allocateDirect(allocSize);
    }

    /**
     * Return a buffer to the pool for reuse.
     * Non-standard sizes or full pools are abandoned to GC via Cleaner.
     */
    public void release(ByteBuffer buffer) {
        if (buffer == null || !buffer.isDirect())
            return;

        ConcurrentLinkedDeque<ByteBuffer> pool = poolForCapacity(buffer.capacity());
        if (pool != null && pool.size() < MAX_POOLED_PER_TIER) {
            buffer.clear();
            pool.offerFirst(buffer);
        }
    }

    /** Release all pooled buffers — called on shutdown. */
    public void shutdown() {
        int count = pooledBufferCount();
        smallPool.clear();
        mediumPool.clear();
        largePool.clear();
        xlargePool.clear();

        LOG.info("Buffer pool shut down ({} pooled released). Stats: {} hits, {} misses, {}KB allocated",
                count, hits.get(), misses.get(), totalAllocatedBytes.get() / 1024);
    }

    public float hitRate() {
        int total = hits.get() + misses.get();
        return total > 0 ? (float) hits.get() / total : 0f;
    }

    public int pooledBufferCount() {
        return smallPool.size() + mediumPool.size() + largePool.size() + xlargePool.size();
    }

    // --- Tier selection ---

    private ConcurrentLinkedDeque<ByteBuffer> poolForSize(int minSize) {
        if (minSize <= SMALL_SIZE)
            return smallPool;
        if (minSize <= MEDIUM_SIZE)
            return mediumPool;
        if (minSize <= LARGE_SIZE)
            return largePool;
        if (minSize <= XLARGE_SIZE)
            return xlargePool;
        return null;
    }

    private ConcurrentLinkedDeque<ByteBuffer> poolForCapacity(int capacity) {
        if (capacity == SMALL_SIZE)
            return smallPool;
        if (capacity == MEDIUM_SIZE)
            return mediumPool;
        if (capacity == LARGE_SIZE)
            return largePool;
        if (capacity == XLARGE_SIZE)
            return xlargePool;
        return null;
    }

    private static int roundUpToTier(int size) {
        if (size <= SMALL_SIZE)
            return SMALL_SIZE;
        if (size <= MEDIUM_SIZE)
            return MEDIUM_SIZE;
        if (size <= LARGE_SIZE)
            return LARGE_SIZE;
        if (size <= XLARGE_SIZE)
            return XLARGE_SIZE;
        return size; // Exact allocation for oversized requests
    }
}
