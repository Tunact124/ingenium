$content = @'
package com.ingenium.adaptive;

import com.ingenium.config.IngeniumConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;

/**
 * Tick Governor - Correct Implementation
 */
public class TickGovernor {
    private final IngeniumConfig config;
    private static float rollingMspt = 0;
    private static int aiRate = 1;

    public TickGovernor(IngeniumConfig config) {
        this.config = config;
    }

    public static float getRollingMspt(MinecraftServer server) {
        long[] ticks = server.lastTickLengths;
        if (ticks == null || ticks.length == 0) return 0;
        long sum = 0;
        int count = Math.min(20, ticks.length);
        for (int i = ticks.length - count; i < ticks.length; i++)
            sum += ticks[i];
        return (sum / (float)count) / 1_000_000f;
    }

    public void onServerTick(MinecraftServer server) {
        if (!IngeniumConfig.ENABLE_TICK_GOVERNOR) {
            aiRate = 1;
            return;
        }

        rollingMspt = getRollingMspt(server);

        if (rollingMspt < 40.0f) {
            aiRate = 1; // NORMAL
        } else if (rollingMspt < 48.0f) {
            aiRate = 2; // STRESSED
        } else {
            aiRate = 3; // CRITICAL
        }
    }

    public static int getAIRate() {
        return aiRate;
    }

    public static boolean shouldTickAI(Entity e, long worldTick) {
        if (!IngeniumConfig.ENABLE_TICK_GOVERNOR) return true;
        
        if (e instanceof PlayerEntity) return true;
        if (e instanceof MobEntity m && m.getTarget() != null) return true; // Use getTarget() as isAttacking() might be different
        
        int rate = getAIRate();
        if (rate <= 1) return true;
        
        return (e.getId() % rate) == (worldTick % rate);
    }
    
    public void enable() {}
    public void disable() { aiRate = 1; }
    public boolean isEnabled() { return IngeniumConfig.ENABLE_TICK_GOVERNOR; }
    public String getStatistics() {
        return String.format("MSPT: %.1fms, Rate: %d:1", rollingMspt, aiRate);
    }
}
'@
Set-Content -Path src\main\java\com\ingenium\adaptive\TickGovernor.java -Value $content -NoNewline
