package com.ingenium.governor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ActivityClassifier {
 
    // Weights: combat persists longer than building (more state-critical)
    private static final int COMBAT_DECAY   = 30; // 1.5 seconds
    private static final int BUILDING_DECAY =  8; // 0.4 seconds
 
    private int combatCooldown   = 0;
    private int buildingCooldown = 0;
 
    // Registered via ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY
    public void onCombat(ServerWorld w, Entity k, LivingEntity killed) {
        combatCooldown = COMBAT_DECAY;
    }
 
    // Registered via PlayerBlockBreakEvents.AFTER and UseBlockCallback
    public void onBlockInteract(World w, PlayerEntity p, BlockPos pos) {
        buildingCooldown = BUILDING_DECAY;
    }
 
    /** Decay once per tick on the server thread. */
    public void tick() {
        if (combatCooldown   > 0) combatCooldown--;
        if (buildingCooldown > 0) buildingCooldown--;
    }
 
    public ActivityState classify(double mspt, int playerCount) {
        if (playerCount == 0)      return ActivityState.IDLE;
        if (combatCooldown   > 0)  return ActivityState.COMBAT;
        if (buildingCooldown > 0)  return ActivityState.BUILDING;
        return ActivityState.LOADED;
    }
}
