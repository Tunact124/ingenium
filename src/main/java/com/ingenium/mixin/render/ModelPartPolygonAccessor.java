package com.ingenium.mixin.render;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.Polygon.class)
public interface ModelPartPolygonAccessor {
    @Accessor("normal")
    Vector3f ingenium_normal();

    @Accessor("vertices")
    ModelPart.Vertex[] ingenium_vertices();
}
