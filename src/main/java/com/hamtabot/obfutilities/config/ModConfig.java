package com.hamtabot.obfutilities.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.timer.TimerEntry;

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

    public boolean keepSession = false;

    public boolean doubleDropEnabled = false;

    public List<CustomBlockEntry> customBlocks = new ArrayList<>();

    public boolean autoToolEnabled           = false;
    public boolean autoToolSkipLowDurability = true;

    public boolean autoToolUsePioche  = true;
    public boolean autoToolUsePelle   = true;
    public boolean autoToolUseHache   = true;
    public boolean autoToolUseHoue    = true;
    public boolean autoToolUseCisaille= true;
    public boolean autoToolUseEpee    = true;

    public boolean fullBrightEnabled  = false;

    public boolean blockShadeEnabled = true;
    public List<BlockShadeEntry> blockShades = new ArrayList<>();

    public boolean attackRemapEnabled = false;
    public int     attackRemapKey     = org.lwjgl.glfw.GLFW.GLFW_KEY_R;

    public boolean useRemapEnabled = false;
    public int     useRemapKey     = org.lwjgl.glfw.GLFW.GLFW_KEY_F;

    public boolean waypointsEnabled = true;

    public float scaleHud = 1.0f;

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
    public String  adMessage      = "";
    public boolean adTitleEnabled = true;

    public List<TimerEntry> timers = new ArrayList<>();

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

    public static class BlockShadeEntry {
        public String  blockId = "minecraft:stone";
        public boolean enabled = true;

        public BlockShadeEntry() {}
        public BlockShadeEntry(String blockId) { this.blockId = blockId; }
    }

    public static ModConfig load() {
        if (Files.exists(CONFIG_FILE)) {
            try (Reader r = Files.newBufferedReader(CONFIG_FILE)) {
                ModConfig cfg = GSON.fromJson(r, ModConfig.class);
                if (cfg.customBlocks == null) cfg.customBlocks = new ArrayList<>();
                if (cfg.blockShades == null) cfg.blockShades = new ArrayList<>();
                if (cfg.adMessage == null) cfg.adMessage = "";
                if (cfg.timers == null) cfg.timers = new ArrayList<>();
                cfg.scaleHud = clampScale(cfg.scaleHud);
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