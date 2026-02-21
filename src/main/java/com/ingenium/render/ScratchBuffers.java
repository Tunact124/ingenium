package com.ingenium.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ScratchBuffers {
    private ScratchBuffers() {}

    // Default capacity sized for "small bulk pushes" (quads, particles, etc.)
    // You can raise this later; keep it power-of-two-ish to reduce realloc churn.
    private static final int DEFAULT_BYTES = 64 * 1024;

    private static final ThreadLocal<ByteBuffer> TL = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(DEFAULT_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    );

    public static ByteBuffer get(int minCapacityBytes) {
        ByteBuffer buf = TL.get();
        if (buf.capacity() < minCapacityBytes) {
            // Rare path: grow. Still thread-local, so no cross-thread contention.
            int newCap = nextPow2(minCapacityBytes);
            buf = ByteBuffer.allocateDirect(newCap).order(ByteOrder.LITTLE_ENDIAN);
            TL.set(buf);
        }
        buf.clear();
        return buf;
    }

    private static int nextPow2(int x) {
        int v = x - 1;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        return v + 1;
    }
}
