package com.ingenium.render.culling;

import net.minecraft.core.Direction;

public final class FaceMask {
    private FaceMask() {}

    public static final int ALL = (1 << 6) - 1;

    public static int of(Direction dir) {
        return 1 << dir.ordinal();
    }

    public static int of(Direction a, Direction b) {
        return of(a) | of(b);
    }

    public static boolean contains(int mask, Direction dir) {
        return (mask & of(dir)) != 0;
    }
}
