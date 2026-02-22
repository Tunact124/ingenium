package com.ingenium.mixin.chunk;

import com.ingenium.chunk.ChunkPriorityEngine;
import com.ingenium.core.IngeniumGovernor;
import com.ingenium.server.ServerLevelExtension;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Implements ServerLevelExtension to provide access to ChunkPriorityEngine.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelExtensionMixin implements ServerLevelExtension {
    
    @Unique
    private ChunkPriorityEngine ingenium_chunkPriorityEngine;

    @Inject(
        method = "<init>*",
        at = @At("RETURN")
    )
    private void ingenium_initChunkPriorityEngine(CallbackInfo ci) {
        this.ingenium_chunkPriorityEngine = new ChunkPriorityEngine(IngeniumGovernor.getInstance());
    }

    @Override
    public ChunkPriorityEngine ingenium_getChunkPriorityEngine() {
        return this.ingenium_chunkPriorityEngine;
    }
}
