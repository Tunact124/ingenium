package com.ingenium.mixin.chunk;

import com.ingenium.chunk.ChunkPriorityEngine;
import com.ingenium.server.ServerLevelExtension;
import net.minecraft.server.level.ServerLevel;

public final class ChunkPriorityAccess {
    public static ChunkPriorityEngine getEngine(ServerLevel level) {
        if (level instanceof ServerLevelExtension ext) {
            return ext.ingenium_getChunkPriorityEngine();
        }
        return null;
    }
}
