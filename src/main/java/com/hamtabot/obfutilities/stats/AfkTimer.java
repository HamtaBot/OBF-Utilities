package com.hamtabot.obfutilities.stats;

import com.google.gson.Gson;
import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.util.JsonFiles;
import com.hamtabot.obfutilities.waypoint.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class AfkTimer {

    private static final Path FILE = Paths.get("config/obfutilities_afk.json");
    private static final Gson GSON = new Gson();
    private static final long MAX_TICK_GAP_MS = 1_000;

    private static long    elapsedMs = 0;
    private static boolean inZone    = false;
    private static long    lastTick  = 0;

    private AfkTimer() {}

    static class Data {
        long elapsedMs;
    }

    public static void tick(MinecraftClient client) {
        long now = System.currentTimeMillis();
        boolean nowInZone = isInZone(client);
        if (inZone && nowInZone) elapsedMs += Math.min(now - lastTick, MAX_TICK_GAP_MS);
        inZone   = nowInZone;
        lastTick = now;
    }

    private static boolean isInZone(MinecraftClient client) {
        if (client.player == null || client.world == null || !hasZone()) return false;
        ModConfig cfg = OBFUtilities.config;
        if (!WaypointManager.getCurrentDimension().equals(cfg.afkDimension)) return false;
        int[] a = cfg.afkCorner1, b = cfg.afkCorner2;
        return inRange(client.player.getX(), a[0], b[0])
            && inRange(client.player.getY(), a[1], b[1])
            && inRange(client.player.getZ(), a[2], b[2]);
    }

    private static boolean inRange(double v, int a, int b) {
        return v >= Math.min(a, b) && v < Math.max(a, b) + 1;
    }

    public static boolean isCorner(int[] c) { return c != null && c.length == 3; }

    public static boolean hasZone() {
        ModConfig cfg = OBFUtilities.config;
        return isCorner(cfg.afkCorner1) && isCorner(cfg.afkCorner2);
    }

    public static void setCorner(MinecraftClient client, boolean first) {
        if (client.player == null || client.world == null) return;
        ModConfig cfg = OBFUtilities.config;
        BlockPos p = client.player.getBlockPos();
        int[] corner = { p.getX(), p.getY(), p.getZ() };
        if (first) cfg.afkCorner1 = corner;
        else       cfg.afkCorner2 = corner;
        cfg.afkDimension = WaypointManager.getCurrentDimension();
        cfg.save();
    }

    public static void clearZone() {
        ModConfig cfg = OBFUtilities.config;
        cfg.afkCorner1 = null;
        cfg.afkCorner2 = null;
        cfg.save();
    }

    public static boolean isActive() { return inZone; }

    public static long getElapsedMs() { return elapsedMs; }

    public static void reset() {
        elapsedMs = 0;
        save();
    }

    public static void load() {
        if (!OBFUtilities.config.keepAfkTimer) return;
        Data d = JsonFiles.read(FILE, Data.class, GSON);
        if (d != null) elapsedMs = Math.max(0, d.elapsedMs);
    }

    public static void save() {
        if (!OBFUtilities.config.keepAfkTimer) return;
        Data d = new Data();
        d.elapsedMs = elapsedMs;
        JsonFiles.write(FILE, d, GSON);
    }
}
