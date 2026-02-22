package com.ingenium.simd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Thread-local scratch buffers for SIMD operations.
 */
public final class SIMDScratchBuffers {

    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/SIMD");

    private static final int NOISE_BUFFER_FLOATS = 4096;
    private static final int PALETTE_BUFFER_INTS = 4096;

    private static final ThreadLocal<ByteBuffer> NOISE_BUFFER = ThreadLocal.withInitial(() -> {
        ByteBuffer buf = ByteBuffer.allocateDirect(NOISE_BUFFER_FLOATS * Float.BYTES);
        buf.order(ByteOrder.nativeOrder());
        return buf;
    });

    private static final ThreadLocal<ByteBuffer> PALETTE_BUFFER = ThreadLocal.withInitial(() -> {
        ByteBuffer buf = ByteBuffer.allocateDirect(PALETTE_BUFFER_INTS * Integer.BYTES);
        buf.order(ByteOrder.nativeOrder());
        return buf;
    });

    public static ByteBuffer getNoiseBuffer() {
        ByteBuffer buf = NOISE_BUFFER.get();
        buf.clear();
        return buf;
    }

    public static ByteBuffer getPaletteBuffer() {
        ByteBuffer buf = PALETTE_BUFFER.get();
        buf.clear();
        return buf;
    }

    public static void cleanup() {
        NOISE_BUFFER.remove();
        PALETTE_BUFFER.remove();
    }

    private SIMDScratchBuffers() {}
}
