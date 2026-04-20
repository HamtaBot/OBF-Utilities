package com.hamtabot.obfutilities.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hamtabot.obfutilities.OBFUtilities;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    private static final Path CONFIG_FILE = Paths.get("config/obfutilities.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean showBlocksPlaced = true;
    public boolean showBlocksMined  = true;
    public boolean showMobsKilled   = true;
    public boolean showAdTimer      = true;

    public List<CustomBlockEntry> customBlocks = new ArrayList<>();

    public boolean autoToolEnabled           = false;
    public boolean autoToolSkipLowDurability = true;

    public boolean autoToolUsePioche  = true;
    public boolean autoToolUsePelle   = true;
    public boolean autoToolUseHache   = true;
    public boolean autoToolUseHoue    = true;
    public boolean autoToolUseCisaille= true;
    public boolean autoToolUseEpee    = true;

    public boolean fullBrightEnabled   = false;
    public float   fullBrightLevel     = 1.0f;

    public boolean attackRemapEnabled = false;
    public int     attackRemapKey     = org.lwjgl.glfw.GLFW.GLFW_KEY_R;
    public int     cfgAttackRemapX    = -1;
    public int     cfgAttackRemapY    = 20;

    public boolean waypointsEnabled = true;
    public int cfgWpX = -1, cfgWpY = 20;

    public int cfgLeftX = -1, cfgLeftY = 20;
    public int cfgRightX = -1, cfgRightY = 20;
    public int cfgToolX = -1, cfgToolY = 20;
    public int cfgBrightX = -1, cfgBrightY = 20;
    public int cfgDebugX = -1, cfgDebugY = 20;

    public float scaleLeft   = 1.0f;
    public float scaleRight  = 1.0f;
    public float scaleTool   = 1.0f;
    public float scaleBright = 1.0f;
    public float scaleDebug  = 1.0f;
    public float scaleWp     = 1.0f;
    public float scaleAr     = 1.0f;
    public float scaleHud    = 1.0f;

    public int serverTotalPlaced = -1;
    public int serverTotalMined  = -1;
    public int serverTotalKills  = -1;

    public boolean debugShowFps    = false;
    public boolean debugShowCoords = false;
    public boolean debugShowRam    = false;
    public int debugFpsX           = 10;
    public int debugFpsY           = 100;
    public int debugCoordsX        = 10;
    public int debugCoordsY        = 120;
    public int debugRamX           = 10;
    public int debugRamY           = 140;

    public long adCooldownMs = 15 * 60 * 1000L;

    public static class CustomBlockEntry {
        public String  blockId      = "minecraft:stone";
        public boolean trackPlaced  = true;
        public boolean enabled      = true;

        public CustomBlockEntry() {}
        public CustomBlockEntry(String blockId, boolean trackPlaced) {
            this.blockId     = blockId;
            this.trackPlaced = trackPlaced;
        }
    }

    public static ModConfig load() {
        if (Files.exists(CONFIG_FILE)) {
            try (Reader r = Files.newBufferedReader(CONFIG_FILE)) {
                ModConfig cfg = GSON.fromJson(r, ModConfig.class);
                if (cfg.customBlocks == null) cfg.customBlocks = new ArrayList<>();
                cfg.scaleLeft   = clampScale(cfg.scaleLeft);
                cfg.scaleRight  = clampScale(cfg.scaleRight);
                cfg.scaleTool   = clampScale(cfg.scaleTool);
                cfg.scaleBright = clampScale(cfg.scaleBright);
                cfg.scaleDebug  = clampScale(cfg.scaleDebug);
                cfg.scaleWp     = clampScale(cfg.scaleWp);
                cfg.scaleAr     = clampScale(cfg.scaleAr);
                cfg.scaleHud    = clampScale(cfg.scaleHud);
                return cfg;
            } catch (Exception e) {
                OBFUtilities.LOGGER.error("[OBF] Erreur chargement config: " + e.getMessage());
            }
        }
        ModConfig cfg = new ModConfig();
        cfg.save();
        return cfg;
    }

    public static float clampScale(float v) {
        return Math.max(0.6f, Math.min(2.0f, v == 0f ? 1.0f : v));
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(this, w);
            }
        } catch (Exception e) {
            OBFUtilities.LOGGER.error("[OBF] Erreur sauvegarde config: " + e.getMessage());
        }
    }
}