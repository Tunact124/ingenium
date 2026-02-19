package com.ingenium.memory;

import net.minecraft.util.math.Vec3d;

// Vec3d is immutable in vanilla — we pool a mutable carrier instead.
public final class Vec3dPool {
 
    // MutableVec3d is an Ingenium-internal class that holds x,y,z as doubles.
    // Convert to vanilla Vec3d only when the vanilla API strictly requires it.
    private static final ObjectPool<MutableVec3d> POOL = new ObjectPool<>(
        MutableVec3d::new,
        v -> v.set(0, 0, 0),
        128
    );
 
    public static MutableVec3d acquire(double x, double y, double z) {
        return POOL.acquire().set(x, y, z);
    }
 
    public static void release(MutableVec3d v) { POOL.release(v); }
 
    /** Internal mutable carrier — never expose beyond your method scope. */
    public static final class MutableVec3d {
        public double x, y, z;
        public MutableVec3d set(double x, double y, double z) {
            this.x=x; this.y=y; this.z=z; return this;
        }
        public Vec3d toVanilla() { return new Vec3d(x, y, z); }
        public double lengthSq() { return x*x + y*y + z*z; }
    }
}
