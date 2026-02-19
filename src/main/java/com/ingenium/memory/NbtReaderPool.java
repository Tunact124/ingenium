package com.ingenium.memory;

import net.minecraft.nbt.NbtCompound;

public final class NbtReaderPool {
 
    private static final ObjectPool<NbtCompound> POOL = new ObjectPool<>(
        NbtCompound::new,
        nbt -> nbt.getKeys().forEach(nbt::remove), // clear all keys
        32 // NBT compounds are large — keep pool small
    );
 
    /**
     * Acquire a blank NbtCompound for use as a read target.
     *
     * WARNING: Only use for SHORT-LIVED reads. The compound must be released
     * before any async handoff — do not store pooled NBT in entity fields.
     */
    public static NbtCompound acquire() { return POOL.acquire(); }
    public static void release(NbtCompound c) { POOL.release(c); }
}
