package com.hamtabot.obfutilities.input;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.ui.ConfigScreen;
import com.hamtabot.obfutilities.waypoint.WaypointCreateScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Keybinds {

    private static final String CATEGORY = "category.obfutilities";

    public static KeyBinding toggleHud;
    public static KeyBinding openConfig;
    public static KeyBinding addWaypoint;
    public static KeyBinding openWaypoints;

    public static void register() {
        toggleHud     = register("toggle_hud",     GLFW.GLFW_KEY_K);
        openConfig    = register("open_config",    GLFW.GLFW_KEY_O);
        addWaypoint   = register("add_waypoint",   GLFW.GLFW_KEY_N);
        openWaypoints = register("open_waypoints", GLFW.GLFW_KEY_G);
    }

    private static KeyBinding register(String name, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + OBFUtilities.MOD_ID + "." + name, InputUtil.Type.KEYSYM, defaultKey, CATEGORY));
    }

    public static void tick(MinecraftClient client) {
        while (toggleHud.wasPressed()) OBFUtilities.getHud().toggleVisible();
        while (openConfig.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new ConfigScreen(null));
        }
        while (addWaypoint.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new WaypointCreateScreen(null));
        }
        while (openWaypoints.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new ConfigScreen(null, ConfigScreen.Page.WAYPOINTS));
        }
    }
}
