package com.hamtabot.obfutilities.stats;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class SessionStats {

    private static final long RATE_WINDOW_MS = 3000;

    public static final StatCounter PLACED = new StatCounter();
    public static final StatCounter MINED  = new StatCounter();
    public static final StatCounter KILLS  = new StatCounter();
    private static final List<StatCounter> BUILTIN = List.of(PLACED, MINED, KILLS);

    private static final Map<ModConfig.CustomBlockEntry, StatCounter> custom = new IdentityHashMap<>();

    private static long rateWindowStart  = System.currentTimeMillis();
    private static long sessionStartTime = System.currentTimeMillis();
    private static boolean activitySinceLastCheck = false;

    public static void onBlockPlaced(String blockId, String itemId) {
        PLACED.add(1);
        activitySinceLastCheck = true;
        for (ModConfig.CustomBlockEntry e : OBFUtilities.config.customBlocks) {
            if (e.enabled && e.trackPlaced && (itemId.equals(e.blockId) || blockId.equals(e.blockId))) custom(e).add(1);
        }
    }

    public static void onBlockMined(String blockId) {
        MINED.add(1);
        activitySinceLastCheck = true;
        for (ModConfig.CustomBlockEntry e : OBFUtilities.config.customBlocks) {
            if (e.enabled && !e.trackPlaced && blockId.equals(e.blockId)) custom(e).add(1);
        }
    }

    public static void onMobKilled(int count) {
        KILLS.add(count);
        activitySinceLastCheck = true;
    }

    public static StatCounter custom(ModConfig.CustomBlockEntry entry) {
        return custom.computeIfAbsent(entry, e -> new StatCounter());
    }

    public static void resetCustom(ModConfig.CustomBlockEntry entry) {
        custom.remove(entry);
    }

    public static void tick() {
        long now     = System.currentTimeMillis();
        long elapsed = now - rateWindowStart;

        if (elapsed >= RATE_WINDOW_MS) {
            float secs = elapsed / 1000f;
            BUILTIN.forEach(c -> c.closeRateWindow(secs));
            custom.values().forEach(c -> c.closeRateWindow(secs));
            rateWindowStart = now;
        }

        BUILTIN.forEach(c -> c.decayIfIdle(now));
        custom.values().forEach(c -> c.decayIfIdle(now));
    }

    private static String key(ModConfig.CustomBlockEntry e) { return e.blockId + (e.trackPlaced ? "|posé" : "|miné"); }

    static SessionStore.Data snapshot() {
        SessionStore.Data d = new SessionStore.Data();
        d.elapsedMs = System.currentTimeMillis() - sessionStartTime;
        d.placed = PLACED.getSession();
        d.mined  = MINED.getSession();
        d.kills  = KILLS.getSession();
        custom.forEach((e, c) -> d.custom.put(key(e), c.getSession()));
        return d;
    }

    static void restore(SessionStore.Data d) {
        sessionStartTime = System.currentTimeMillis() - Math.max(0, d.elapsedMs);
        PLACED.setSession(d.placed);
        MINED.setSession(d.mined);
        KILLS.setSession(d.kills);
        if (d.custom == null) return;
        for (ModConfig.CustomBlockEntry e : OBFUtilities.config.customBlocks) {
            Integer n = d.custom.get(key(e));
            if (n != null) custom(e).setSession(n);
        }
    }

    public static void reset() {
        BUILTIN.forEach(StatCounter::reset);
        custom.clear();
        sessionStartTime = rateWindowStart = System.currentTimeMillis();
        SessionStore.save();
    }

    public static long getSessionStartTime() { return sessionStartTime; }

    public static boolean consumeActivity() {
        boolean had = activitySinceLastCheck;
        activitySinceLastCheck = false;
        return had;
    }
}
