package com.ingenium.render;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.core.Direction;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;

public final class ItemFacesMask {
    private static final int ALL_FACES = 0b11_1111;
    private static final float FAR_WORLD_DEPTH_THRESHOLD = -10.0F;

    private ItemFacesMask() {
    }

    public static int decide(ItemTransforms transforms, ItemDisplayContext context, PoseStack.Pose pose) {
        if (context == ItemDisplayContext.GUI) {
            return decideGui(transforms, pose);
        }
        return decideWorld(transforms, pose);
    }

    private static int decideGui(ItemTransforms transforms, PoseStack.Pose pose) {
        var gui = transforms.gui;

        if (gui == ItemTransform.NO_TRANSFORM) {
            if (pose.pose().m20() == 0.0F && pose.pose().m21() == 0.0F) {
                return 1 << Direction.SOUTH.ordinal();
            }
            return ALL_FACES;
        }

        var rotation = gui.rotation;
        if (rotation != null) {
            if (rotation.equals(30.0F, 225.0F, 0.0F)) {
                return (1 << Direction.UP.ordinal())
                        | (1 << Direction.NORTH.ordinal())
                        | (1 << Direction.EAST.ordinal());
            }

            if (rotation.equals(30.0F, 135.0F, 0.0F)) {
                return (1 << Direction.UP.ordinal())
                        | (1 << Direction.NORTH.ordinal())
                        | (1 << Direction.WEST.ordinal());
            }
        }
        return ALL_FACES;
    }

    private static int decideWorld(ItemTransforms transforms, PoseStack.Pose pose) {
        // Items in world are rendered from all angles, so we shouldn't cull anything
        return ALL_FACES;
    }
}
