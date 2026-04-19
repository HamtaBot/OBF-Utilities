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

    // AutoTool
    public boolean autoToolEnabled           = false;
    public boolean autoToolSkipLowDurability = true;
    public boolean autoToolUsePioche  = true;
    public boolean autoToolUsePelle   = true;
    public boolean autoToolUseHache   = true;
    public boolean autoToolUseHoue    = true;
    public boolean autoToolUseCisaille= true;
    public boolean autoToolUseEpee    = true;

    // FullBright
    public boolean fullBrightEnabled   = false;
    public float   fullBrightLevel     = 1.0f; // 0.0 = 0%, 15.0 = 1500%

    public int cfgLeftX = -1, cfgLeftY = 20;
    public int cfgRightX = -1, cfgRightY = 20;
    public int cfgToolX = -1, cfgToolY = 20;
    public int cfgBrightX = -1, cfgBrightY = 20;
    public int cfgDebugX = -1, cfgDebugY = 20;

    // Stats serveur mis à jour à la connexion via /utilitiesstats
    // C'est ultra dégueu mais pas envie de faire une API qui va être abusée
    // Par les cassos qui décompilent le mod (sert a rien de spam la commande y'a un cache)
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

    // Pub comme ca plus d'excuses
    // TODO: bloquer la pub si timer pas fini
    public long adCooldownMs = 15 * 60 * 1000L;

    public static class CustomBlockEntry {
        public String  blockId      = "minecraft:stone";
        public boolean trackPlaced  = true; // true=posé, false=miné
        public boolean enabled      = true;

        // Pas utilisé mais cest normal touche pas a ça
        public CustomBlockEntry() {}
        // Pas utilisé mais cest normal touche pas a ça
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
                return cfg;
            } catch (Exception e) {
                OBFUtilities.LOGGER.error("[OBF] Erreur chargement config: " + e.getMessage());
            }
        }
        ModConfig cfg = new ModConfig();
        cfg.save();
        return cfg;
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