package com.ingenium.render.instanced;

import com.ingenium.config.IngeniumConfig;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

public final class InstanceBuffer {
 
    // ── Struct-of-Arrays layout ─────────────────────────────────────
    // Each channel is a separate float[] — contiguous per attribute.
    private float[] posX, posY, posZ;     // world position
    private float[] rotY;                  // y-rotation in degrees
    private float[] packedLight;           // pre-packed combined block+sky light int
    private float[] openness;              // chest open amount [0..1]
 
    private int count    = 0;
    private int capacity;
    private int vboId    = -1;
 
    // ── Dirty region tracking ───────────────────────────────────────
    private long lastFrameHash = 0L;
    private boolean dirty      = true;
 
    public InstanceBuffer(int initialCapacity) {
        this.capacity = initialCapacity;
        allocate(initialCapacity);
    }
 
    private void allocate(int cap) {
        posX       = new float[cap];
        posY       = new float[cap];
        posZ       = new float[cap];
        rotY       = new float[cap];
        packedLight= new float[cap];
        openness   = new float[cap];
    }
 
    /** Add an instance. Called during the collection pass. */
    public void add(float x, float y, float z, float ry, float light, float open) {
        if (count == capacity) grow();
        int i = count++;
        posX[i] = x; posY[i] = y; posZ[i] = z;
        rotY[i] = ry; packedLight[i] = light; openness[i] = open;
    }
 
    /**
     * Upload to GPU only if data changed since last frame.
     * Returns true if an upload occurred.
     */
    public boolean uploadIfDirty() {
        if (!IngeniumConfig.get().enableDirtyTracking) {
            upload(); return true;
        }
        long hash = computeHash();
        if (hash == lastFrameHash) return false; // static — skip upload
        lastFrameHash = hash;
        upload();
        return true;
    }
 
    private void upload() {
        // Interleave all SoA channels into one contiguous DirectBuffer for the GPU.
        // 6 floats per instance: x, y, z, rotY, light, openness
        FloatBuffer buf = BufferUtils.createFloatBuffer(count * 6);
        for (int i = 0; i < count; i++) {
            buf.put(posX[i]).put(posY[i]).put(posZ[i])
               .put(rotY[i]).put(packedLight[i]).put(openness[i]);
        }
        buf.flip();
        if (vboId == -1) vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
    }
 
    private long computeHash() {
        // FNV-1a over raw float bits
        long h = 0xCBF29CE484222325L;
        for (int i = 0; i < count; i++) {
            h ^= Float.floatToRawIntBits(posX[i]);
            h *= 0x100000001B3L;
            h ^= Float.floatToRawIntBits(posY[i]);
            h *= 0x100000001B3L;
            h ^= Float.floatToRawIntBits(posZ[i]);
            h *= 0x100000001B3L;
        }
        return h;
    }
 
    public void reset() { count = 0; dirty = true; }
    public int  count() { return count; }
    public int  vboId() { return vboId; }
 
    private void grow() {
        int newCap = capacity + (capacity >> 1); // 1.5x growth
        float[] nx=new float[newCap], ny=new float[newCap], nz=new float[newCap];
        float[] nr=new float[newCap], nl=new float[newCap], no=new float[newCap];
        System.arraycopy(posX,       0, nx, 0, count);
        System.arraycopy(posY,       0, ny, 0, count);
        System.arraycopy(posZ,       0, nz, 0, count);
        System.arraycopy(rotY,       0, nr, 0, count);
        System.arraycopy(packedLight,0, nl, 0, count);
        System.arraycopy(openness,   0, no, 0, count);
        posX=nx; posY=ny; posZ=nz; rotY=nr; packedLight=nl; openness=no;
        capacity = newCap;
    }
}
