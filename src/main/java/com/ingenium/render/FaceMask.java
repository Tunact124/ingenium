package com.ingenium.render;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class FaceMask {
    private FaceMask() {}

    // Returns a 6-bit mask of visible faces for an axis-aligned block at (bx, by, bz)
    // Bit index = Direction.ordinal() (DOWN, UP, NORTH, SOUTH, WEST, EAST)
    public static int visibleBlockFaces(Vec3 camPos, int bx, int by, int bz, boolean isGuiLike) {
        if (isGuiLike) {
            return 0b00_111111; // don’t prune for GUI-like paths
        }

        final double cx = camPos.x - (bx + 0.5);
        final double cy = camPos.y - (by + 0.5);
        final double cz = camPos.z - (bz + 0.5);

        int mask = 0;
        for (Direction d : Direction.values()) {
            final int nx = d.getStepX();
            final int ny = d.getStepY();
            final int nz = d.getStepZ();

            // dot(normal, viewVector)
            final double dot = nx * cx + ny * cy + nz * cz;
            if (dot > 0.0) {
                mask |= (1 << d.ordinal());
            }
        }

        return mask;
    }
}
