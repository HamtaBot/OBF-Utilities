package com.hamtabot.obfutilities.util;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class Format {

    public static String number(int n) {
        return String.format("%,d", n).replace(',', '.');
    }

    public static String number(long n) {
        return String.format("%,d", n).replace(',', '.');
    }

    public static String duration(long ms) {
        long s = (ms / 1000) % 60, m = (ms / 60000) % 60, h = ms / 3600000;
        return h > 0 ? String.format("%dh%02dm%02ds", h, m, s) : String.format("%dm%02ds", m, s);
    }

    public static String clock(long ms) {
        long s = (ms / 1000) % 60, m = (ms / 60000) % 60, h = ms / 3600000;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    public static String countdown(long ms) {
        return String.format("%02dm %02ds", (ms / 60000) % 60, (ms / 1000) % 60);
    }

    public static String blockName(String id) {
        if (id == null || id.isEmpty()) return "Unknown";
        String n = id.contains(":") ? id.split(":", 2)[1] : id;
        StringBuilder sb = new StringBuilder();
        for (String p : n.split("_")) if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        return sb.toString().trim();
    }

    public static ItemStack icon(String id, boolean isItem) {
        Identifier ident = Identifier.tryParse(id);
        if (ident == null) return ItemStack.EMPTY;
        return new ItemStack(isItem ? Registries.ITEM.get(ident) : Registries.BLOCK.get(ident).asItem());
    }

    public static String keyName(int glfwKey) {
        return InputUtil.fromKeyCode(glfwKey, 0).getLocalizedText().getString().toUpperCase();
    }

    public static String keyName(KeyBinding kb) {
        return kb == null ? "?" : kb.getBoundKeyLocalizedText().getString();
    }
}
