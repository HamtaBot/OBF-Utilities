package com.hamtabot.obfutilities.ui;

import net.minecraft.client.MinecraftClient;

public class HudPos {

    public float x = -1f, y = -1f;

    public boolean isSet() { return x >= 0f && y >= 0f; }

    public int[] resolve(int w, int h, int legacyX, int legacyY) {
        if (!isSet()) set(legacyX, legacyY, w, h);
        return new int[]{ Math.round(x * Math.max(0, screenW() - w)), Math.round(y * Math.max(0, screenH() - h)) };
    }

    public void set(int px, int py, int w, int h) {
        x = fraction(px, screenW() - w);
        y = fraction(py, screenH() - h);
    }

    private static float fraction(int p, int free) {
        return free <= 0 ? 0f : Math.max(0f, Math.min(1f, p / (float) free));
    }

    private static int screenW() { return MinecraftClient.getInstance().getWindow().getScaledWidth(); }

    private static int screenH() { return MinecraftClient.getInstance().getWindow().getScaledHeight(); }
}
