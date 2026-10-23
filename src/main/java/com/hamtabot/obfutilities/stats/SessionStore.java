package com.hamtabot.obfutilities.stats;

import com.google.gson.Gson;
import com.hamtabot.obfutilities.OBFUtilities;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
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
        if (!OBFUtilities.config.keepSession || !Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d != null) SessionStats.restore(d);
        } catch (Exception e) {
            OBFUtilities.LOGGER.error("[OBF] Erreur chargement session: " + e.getMessage());
        }
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastSave < SAVE_EVERY_MS) return;
        lastSave = now;
        save();
    }

    public static void save() {
        if (!OBFUtilities.config.keepSession) return;
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) { GSON.toJson(SessionStats.snapshot(), w); }
        } catch (Exception e) {
            OBFUtilities.LOGGER.error("[OBF] Erreur sauvegarde session: " + e.getMessage());
        }
    }
}
