package com.hamtabot.obfutilities.stats;

import com.google.gson.Gson;
import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.util.JsonFiles;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class SessionStore {

    private static final Path FILE = Paths.get("config/obfutilities_session.json");
    private static final Gson GSON = new Gson();
    private static final long SAVE_EVERY_MS = 30_000;

    private static long lastSave = System.currentTimeMillis();

    private SessionStore() {}

    static class Data {
        long elapsedMs;
        int placed, mined, kills;
        Map<String, Integer> custom = new HashMap<>();
    }

    public static void load() {
        if (!OBFUtilities.config.keepSession) return;
        Data d = JsonFiles.read(FILE, Data.class, GSON);
        if (d != null) SessionStats.restore(d);
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastSave < SAVE_EVERY_MS) return;
        lastSave = now;
        save();
        AfkTimer.save();
    }

    public static void save() {
        if (OBFUtilities.config.keepSession) JsonFiles.write(FILE, SessionStats.snapshot(), GSON);
    }
}
