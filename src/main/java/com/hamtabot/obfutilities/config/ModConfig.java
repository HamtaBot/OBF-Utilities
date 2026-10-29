package com.hamtabot.obfutilities.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hamtabot.obfutilities.timer.TimerEntry;
import com.hamtabot.obfutilities.ui.HudElement;
import com.hamtabot.obfutilities.ui.HudPos;
import com.hamtabot.obfutilities.util.JsonFiles;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModConfig {

    private static final Path CONFIG_FILE = Paths.get("config/obfutilities.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean showBlocksPlaced = true;
    public boolean showBlocksMined  = true;
    public boolean showMobsKilled   = true;
    public boolean showAdTimer      = true;
    public boolean showAfkTimer     = true;

    public boolean keepSession  = false;
    public boolean keepAfkTimer = false;

    public String afkDimension = "minecraft:overworld";
    public int[]  afkCorner1   = null;
    public int[]  afkCorner2   = null;

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

    public boolean hideOtherArmor        = false;
    public boolean disableParticles      = false;
    public boolean disableChestAnimation = false;
    public boolean hideItemFrames        = false;
    public boolean disableSpawnerAnimation = false;
    public float   heldItemScale         = 1.0f;

    public List<WarpBind> warps = new ArrayList<>();

    public List<String> pageOrder = new ArrayList<>();

    public boolean storageDepositEnabled  = true;
    public boolean storageWithdrawEnabled = true;

    public boolean blockShadeEnabled = true;
    public List<BlockShadeEntry> blockShades = new ArrayList<>();

    public boolean attackRemapEnabled = false;
    public int     attackRemapKey     = org.lwjgl.glfw.GLFW.GLFW_KEY_R;

    public boolean useRemapEnabled = false;
    public int     useRemapKey     = org.lwjgl.glfw.GLFW.GLFW_KEY_F;

    public boolean waypointsEnabled = true;

    public float scaleHud = 1.0f;

    public boolean invHudEnabled = false;
    public HudPos  invHudPos     = new HudPos();
    public float   invHudScale   = 1.0f;
    public float   invHudOpacity = 0.8f;
    public boolean invHudShowHotbar = true;

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
    public HudPos debugFpsPos      = new HudPos();
    public HudPos debugCoordsPos   = new HudPos();
    public HudPos debugRamPos      = new HudPos();

    public long adCooldownMs = 15 * 60 * 1000L;
    public String  adMessage      = "";
    public boolean adTitleEnabled = true;

    public List<TimerEntry> timers = new ArrayList<>();

    public boolean kitsEnabled     = false;
    public boolean kitTitleEnabled = true;
    public boolean kitHudEnabled   = false;
    public HudPos  kitHudPos       = new HudPos();
    public float   kitHudScale     = 1.0f;
    public List<String> kitHudHidden = new ArrayList<>();
    public String  kitServer       = "ONEBLOCK";

    public boolean gradesEnabled     = false;
    public boolean gradeTitleEnabled = true;
    public boolean gradeHudEnabled   = false;
    public HudPos  gradeHudPos       = new HudPos();
    public float   gradeHudScale     = 1.0f;

    public boolean jobBarsEnabled  = false;
    public HudPos  jobBarsPos      = new HudPos();
    public float   jobBarsScale    = 1.0f;
    public float   jobBarsOpacity  = 1.0f;
    public boolean skillBarsEnabled = false;
    public HudPos  skillBarsPos     = new HudPos();
    public float   skillBarsScale   = 1.0f;
    public float   skillBarsOpacity = 1.0f;
    public boolean questBarsEnabled = false;
    public HudPos  questBarsPos     = new HudPos();
    public float   questBarsScale   = 1.0f;
    public float   questBarsOpacity = 1.0f;
    public boolean infoBarsEnabled = false;
    public HudPos  infoBarsPos     = new HudPos();
    public float   infoBarsScale   = 1.0f;
    public float   infoBarsOpacity = 1.0f;
    public boolean infoBarsSeparators = true;
    public List<String> infoBarsHidden = new ArrayList<>();

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

    public static class WarpBind {
        public String name = "";
        public int    key  = -1;

        public WarpBind() {}
        public WarpBind(String name) { this.name = name; }
    }

    public static class BlockShadeEntry {
        public String  blockId = "minecraft:stone";
        public boolean enabled = true;

        public BlockShadeEntry() {}
        public BlockShadeEntry(String blockId) { this.blockId = blockId; }
    }

    public static ModConfig load() {
        ModConfig cfg = JsonFiles.read(CONFIG_FILE, ModConfig.class, GSON);
        if (cfg == null) cfg = new ModConfig();
        cfg.sanitize();
        cfg.save();
        return cfg;
    }

    private void sanitize() {
        if (customBlocks == null) customBlocks = new ArrayList<>();
        if (blockShades == null) blockShades = new ArrayList<>();
        if (timers == null) timers = new ArrayList<>();
        if (warps == null) warps = new ArrayList<>();
        if (pageOrder == null) pageOrder = new ArrayList<>();
        pageOrder.removeIf(Objects::isNull);
        customBlocks.removeIf(e -> e == null || e.blockId == null);
        blockShades.removeIf(e -> e == null || e.blockId == null);
        timers.removeIf(Objects::isNull);
        for (TimerEntry t : timers) if (t.pos == null) t.pos = new HudPos();
        if (invHudPos == null) invHudPos = new HudPos();
        if (debugFpsPos == null) debugFpsPos = new HudPos();
        if (debugCoordsPos == null) debugCoordsPos = new HudPos();
        if (debugRamPos == null) debugRamPos = new HudPos();
        if (kitHudPos == null) kitHudPos = new HudPos();
        if (kitHudHidden == null) kitHudHidden = new ArrayList<>();
        kitHudHidden.removeIf(Objects::isNull);
        if (kitServer == null) kitServer = "ONEBLOCK";
        if (gradeHudPos == null) gradeHudPos = new HudPos();
        if (jobBarsPos == null) jobBarsPos = new HudPos();
        if (skillBarsPos == null) skillBarsPos = new HudPos();
        if (questBarsPos == null) questBarsPos = new HudPos();
        if (infoBarsPos == null) infoBarsPos = new HudPos();
        if (infoBarsHidden == null) infoBarsHidden = new ArrayList<>();
        infoBarsHidden.removeIf(Objects::isNull);
        warps.removeIf(w -> w == null || w.name == null || w.name.isBlank());
        if (adMessage == null) adMessage = "";
        if (afkDimension == null) afkDimension = "minecraft:overworld";
        scaleHud = clampScale(scaleHud);
        heldItemScale = clamp01(heldItemScale);
        invHudScale = HudElement.clampScale(invHudScale);
        kitHudScale = HudElement.clampScale(kitHudScale);
        gradeHudScale = HudElement.clampScale(gradeHudScale);
        jobBarsScale = HudElement.clampScale(jobBarsScale);
        skillBarsScale = HudElement.clampScale(skillBarsScale);
        questBarsScale = HudElement.clampScale(questBarsScale);
        infoBarsScale = HudElement.clampScale(infoBarsScale);
        invHudOpacity = clamp01(invHudOpacity);
        jobBarsOpacity = clamp01(jobBarsOpacity);
        skillBarsOpacity = clamp01(skillBarsOpacity);
        questBarsOpacity = clamp01(questBarsOpacity);
        infoBarsOpacity = clamp01(infoBarsOpacity);
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    public static float clampScale(float v) {
        return Math.max(0.6f, Math.min(2.0f, v == 0f ? 1.0f : v));
    }

    public void save() {
        JsonFiles.write(CONFIG_FILE, this, GSON);
    }
}