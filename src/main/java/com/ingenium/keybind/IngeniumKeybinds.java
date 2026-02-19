package com.ingenium.keybind;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class IngeniumKeybinds {
 
    //  ⚠ THIS is the single shared category string.
    //  All KeyBinding registrations MUST use this constant.
    //  It maps to an i18n key in your lang file.
    public static final String KEY_CATEGORY = "key.categories.ingenium";
 
    // ── All mod keybinds declared here ─────────────────────────────
    public static final KeyBinding KEY_TOGGLE_GOVERNOR =
        new KeyBinding(
            "key.ingenium.toggle_governor",      // translation key
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,               // unbound by default
            KEY_CATEGORY                         // ← shared constant
        );
 
    public static final KeyBinding KEY_TOGGLE_INSTANCED_RENDER =
        new KeyBinding(
            "key.ingenium.toggle_instanced_render",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KEY_CATEGORY                         // ← same constant
        );
 
    public static final KeyBinding KEY_OPEN_SETTINGS =
        new KeyBinding(
            "key.ingenium.open_settings",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KEY_CATEGORY                         // ← same constant
        );
 
    public static final KeyBinding KEY_TOGGLE_ASYNC_PATH =
        new KeyBinding(
            "key.ingenium.toggle_async_pathfinding",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KEY_CATEGORY                         // ← same constant
        );
 
    /** Must be called from IngeniumClientMod.onInitializeClient() */
    public static void register() {
        KeyBindingHelper.registerKeyBinding(KEY_TOGGLE_GOVERNOR);
        KeyBindingHelper.registerKeyBinding(KEY_TOGGLE_INSTANCED_RENDER);
        KeyBindingHelper.registerKeyBinding(KEY_OPEN_SETTINGS);
        KeyBindingHelper.registerKeyBinding(KEY_TOGGLE_ASYNC_PATH);
    }
}
