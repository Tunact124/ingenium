package com.ingenium.client;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.config.IngeniumConfigScreen;
import com.ingenium.keybind.IngeniumKeybinds;
import com.ingenium.render.FrustumCache;
import com.ingenium.render.instanced.InstancedRenderManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class IngeniumClientMod implements ClientModInitializer {
 
    @Override
    public void onInitializeClient() {
        IngeniumKeybinds.register();
 
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Open settings screen
            while (IngeniumKeybinds.KEY_OPEN_SETTINGS.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(IngeniumConfigScreen.create(client.currentScreen));
                }
            }
            // Toggle governor (sends packet to server or flips client flag)
            while (IngeniumKeybinds.KEY_TOGGLE_GOVERNOR.wasPressed()) {
                IngeniumConfig.get().enableGovernor ^= true;
                IngeniumConfig.HANDLER.save();
                if (client.player != null) {
                    client.player.sendMessage(
                        Text.of("Governor: " + (IngeniumConfig.get().enableGovernor ? "ON" : "OFF")),
                        true // action bar
                    );
                }
            }
            // Toggle instanced rendering
            while (IngeniumKeybinds.KEY_TOGGLE_INSTANCED_RENDER.wasPressed()) {
                IngeniumConfig.get().enableInstancedRendering ^= true;
                IngeniumConfig.HANDLER.save();
            }
        });

        // 3. Update frustum cache each frame
        WorldRenderEvents.START.register(ctx ->
            FrustumCache.update(ctx.frustum()));
 
        // 4. Flush instanced render buffers at end of world render
        WorldRenderEvents.LAST.register(ctx ->
            InstancedRenderManager.flushAll());
    }
}
