package com.ingenium.mixin.render;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.Vertex.class)
public interface ModelPartVertexAccessor {
    @Accessor("pos")
    Vector3f ingenium_pos();

    @Accessor("u")
    float ingenium_u();

    @Accessor("v")
    float ingenium_v();
}
