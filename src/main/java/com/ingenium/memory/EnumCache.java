package com.ingenium.memory;

import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;

public final class EnumCache {
    public static final Direction[]   DIRECTIONS  = Direction.values();
    public static final Direction[]   HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    public static final DyeColor[]    DYE_COLORS  = DyeColor.values();
    public static final Hand[]        HANDS       = Hand.values();
    public static final ActionResult[] ACTIONS    = ActionResult.values();
}
