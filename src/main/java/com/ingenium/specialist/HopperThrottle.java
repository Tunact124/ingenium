package com.ingenium.specialist;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class HopperThrottle {
    private static final int FULL_CONTAINER_COOLDOWN_TICKS = 24;

    private HopperThrottle() {}

    public static int cooldownWhenDestinationFull() {
        return FULL_CONTAINER_COOLDOWN_TICKS;
    }

    public static boolean isContainerFull(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot);
            if (stack.isEmpty()) return false;
            if (stack.getCount() < stack.getMaxStackSize()) return false;
        }
        return true;
    }

    public static boolean canAcceptAny(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) return true;
            if (stack.getCount() < stack.getMaxStackSize()) return true;
        }
        return false;
    }
}
