package com.hamtabot.obfutilities.input;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.EnumMap;
import java.util.Map;

public class KeyRemap {

    public enum Target { ATTACK, USE }

    private static final Map<Target, InputUtil.Key> originalKeys = new EnumMap<>(Target.class);

    public static void applySaved(MinecraftClient client) {
        for (Target t : Target.values()) {
            if (isEnabled(t)) bind(t, InputUtil.fromKeyCode(getKey(t), 0));
        }
    }

    public static void setEnabled(Target t, boolean enabled) {
        if (enabled) {
            originalKeys.putIfAbsent(t, KeyBindingHelper.getBoundKeyOf(binding(t)));
            bind(t, InputUtil.fromKeyCode(getKey(t), 0));
        } else {
            InputUtil.Key original = originalKeys.remove(t);
            bind(t, original != null ? original : binding(t).getDefaultKey());
        }
        ModConfig cfg = OBFUtilities.config;
        if (t == Target.ATTACK) cfg.attackRemapEnabled = enabled; else cfg.useRemapEnabled = enabled;
        cfg.save();
    }

    public static void setKey(Target t, int glfwKey) {
        ModConfig cfg = OBFUtilities.config;
        if (t == Target.ATTACK) cfg.attackRemapKey = glfwKey; else cfg.useRemapKey = glfwKey;
        cfg.save();
        if (isEnabled(t)) bind(t, InputUtil.fromKeyCode(glfwKey, 0));
    }

    public static boolean isEnabled(Target t) {
        return t == Target.ATTACK ? OBFUtilities.config.attackRemapEnabled : OBFUtilities.config.useRemapEnabled;
    }

    public static int getKey(Target t) {
        return t == Target.ATTACK ? OBFUtilities.config.attackRemapKey : OBFUtilities.config.useRemapKey;
    }

    private static KeyBinding binding(Target t) {
        MinecraftClient client = MinecraftClient.getInstance();
        return t == Target.ATTACK ? client.options.attackKey : client.options.useKey;
    }

    private static void bind(Target t, InputUtil.Key key) {
        binding(t).setBoundKey(key);
        KeyBinding.updateKeysByCode();
    }
}
