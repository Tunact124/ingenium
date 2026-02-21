package com.ingenium.net;

import com.ingenium.compat.ModDetect;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

public final class FriendlyByteBufPool {
    private static final int DEFAULT_CAPACITY_BYTES = 8 * 1024;

    private static final ThreadLocal<Pooled> LOCAL = ThreadLocal.withInitial(() -> {
        var byteBuf = Unpooled.directBuffer(DEFAULT_CAPACITY_BYTES, DEFAULT_CAPACITY_BYTES);
        return new Pooled(byteBuf, new FriendlyByteBuf(byteBuf));
    });

    private FriendlyByteBufPool() {
    }

    public static FriendlyByteBuf acquire() {
        if (ModDetect.isKryptonLoaded()) {
            return new FriendlyByteBuf(Unpooled.directBuffer(DEFAULT_CAPACITY_BYTES, DEFAULT_CAPACITY_BYTES));
        }

        var pooled = LOCAL.get();
        pooled.byteBuf.clear();
        return pooled.friendly;
    }

    private record Pooled(ByteBuf byteBuf, FriendlyByteBuf friendly) {}
}
