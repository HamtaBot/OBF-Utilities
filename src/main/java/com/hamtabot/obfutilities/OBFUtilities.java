package com.hamtabot.obfutilities;

import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.hud.OBFHud;
import com.hamtabot.obfutilities.waypoint.Waypoint;
import com.hamtabot.obfutilities.waypoint.WaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.hamtabot.obfutilities.debug.DebugOverlay;
import com.hamtabot.obfutilities.fullbright.FullBright;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OBFUtilities implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("obfutilities");
    public static final String MOD_ID = "obfutilities";

    public static KeyBinding keyToggleHud;
    public static KeyBinding keyOpenConfig;
    public static KeyBinding keyAddWaypoint;
    public static KeyBinding keyOpenWaypoints;

    private static int sessionBlocksPlaced = 0;
    private static int sessionBlocksMined  = 0;
    private static int sessionKills        = 0;

    private static final Map<Integer, Integer> sessionCustom = new LinkedHashMap<>();

    private static long lastBlockPlacedTime = 0;
    private static long lastBlockMinedTime  = 0;
    private static long lastKillTime        = 0;
    private static final Map<Integer, Long> lastCustomTime = new HashMap<>();

    private static float rateBlocksPlaced = 0f;
    private static float rateBlocksMined  = 0f;
    private static float rateKills        = 0f;
    private static final Map<Integer, Float> rateCustom = new HashMap<>();

    private static int recentBlocksPlaced = 0;
    private static int recentBlocksMined  = 0;
    private static int recentKills        = 0;
    private static final Map<Integer, Integer> recentCustom = new HashMap<>();
    private static long rateWindowStart   = System.currentTimeMillis();

    private static long    lastAdTime   = 0;
    private static boolean adAvailable  = true;

    private static long sessionStartTime      = System.currentTimeMillis();
    private static long connectionTime          = System.currentTimeMillis();
    private static final long NOTICE_DURATION_MS = 5 * 60 * 1000L;

    private static boolean activitySinceLastRequest = false;

    private static OBFHud hud;
    public static ModConfig config;

    @Override
    public void onInitializeClient() {
        config = ModConfig.load();

        keyToggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.obfutilities.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.obfutilities"));
        keyOpenConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.obfutilities.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.obfutilities"));
        keyAddWaypoint = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.obfutilities.add_waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.obfutilities"));
        keyOpenWaypoints = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.obfutilities.open_waypoints", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.obfutilities"));

        com.hamtabot.obfutilities.waypoint.WaypointManager.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("obf_wp_import")
                    .then(ClientCommandManager.argument("data", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String raw = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "data");
                                importWaypointData(raw);
                                return 1;
                            })
                    )
            );
        });
        hud = new OBFHud();
        HudRenderCallback.EVENT.register(hud::render);
        com.hamtabot.obfutilities.waypoint.WaypointRenderer.register();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FullBright.tick();
            DebugOverlay.tick(client);

            if (client.player != null) hud.tick(client);
        });
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
                (ctx, delta) -> DebugOverlay.render(ctx, delta));
    }

    public static void onBlockPlaced(String blockId, String itemId) {
        sessionBlocksPlaced++;
        recentBlocksPlaced++;
        lastBlockPlacedTime = System.currentTimeMillis();
        activitySinceLastRequest = true;

        for (int i = 0; i < config.customBlocks.size(); i++) {
            ModConfig.CustomBlockEntry e = config.customBlocks.get(i);
            if (!e.enabled || !e.trackPlaced) continue;
            if (itemId.equals(e.blockId) || blockId.equals(e.blockId)) {
                sessionCustom.merge(i, 1, Integer::sum);
                recentCustom.merge(i, 1, Integer::sum);
                lastCustomTime.put(i, System.currentTimeMillis());
            }
        }
    }

    public static void onBlockMined(String blockId) {
        sessionBlocksMined++;
        recentBlocksMined++;
        lastBlockMinedTime = System.currentTimeMillis();
        activitySinceLastRequest = true;

        for (int i = 0; i < config.customBlocks.size(); i++) {
            ModConfig.CustomBlockEntry e = config.customBlocks.get(i);
            if (!e.enabled || e.trackPlaced) continue;
            if (blockId.equals(e.blockId)) {
                sessionCustom.merge(i, 1, Integer::sum);
                recentCustom.merge(i, 1, Integer::sum);
                lastCustomTime.put(i, System.currentTimeMillis());
            }
        }
    }

    public static void onMobKilled(int count) {
        sessionKills    += count;
        recentKills     += count;
        lastKillTime     = System.currentTimeMillis();
        activitySinceLastRequest = true;
    }

    public static void onAdSent() {
        lastAdTime  = System.currentTimeMillis();
        adAvailable = false;
    }

    public static void updateRates() {
        long now     = System.currentTimeMillis();
        long elapsed = now - rateWindowStart;

        if (elapsed >= 3000) {
            float secs = elapsed / 1000f;
            rateBlocksPlaced = recentBlocksPlaced / secs;
            rateBlocksMined  = recentBlocksMined  / secs;
            rateKills        = recentKills        / secs;
            for (int i = 0; i < config.customBlocks.size(); i++) {
                rateCustom.put(i, recentCustom.getOrDefault(i, 0) / secs);
                recentCustom.put(i, 0);
            }
            recentBlocksPlaced = 0;
            recentBlocksMined  = 0;
            recentKills        = 0;
            rateWindowStart    = now;
        }

        if (now - lastBlockPlacedTime > 4000) rateBlocksPlaced = 0f;
        if (now - lastBlockMinedTime  > 4000) rateBlocksMined  = 0f;
        if (now - lastKillTime        > 4000) rateKills        = 0f;
        for (int i = 0; i < config.customBlocks.size(); i++) {
            if (now - lastCustomTime.getOrDefault(i, 0L) > 4000) rateCustom.put(i, 0f);
        }

        if (!adAvailable && lastAdTime > 0 && now - lastAdTime >= config.adCooldownMs) {
            adAvailable = true;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.inGameHud != null) {
                client.inGameHud.setTitle(Text.literal("§cPub disponible"));
                client.inGameHud.setSubtitle(Text.literal("§7Vous pouvez de nouveau l'envoyer"));
                client.inGameHud.setTitleTicks(10, 70, 20);
            }
        }
    }

    public static void resetSession() {
        sessionBlocksPlaced = 0;
        sessionBlocksMined  = 0;
        sessionKills        = 0;
        sessionCustom.clear();
        recentBlocksPlaced  = 0;
        recentBlocksMined   = 0;
        recentKills         = 0;
        recentCustom.clear();
        rateBlocksPlaced    = 0f;
        rateBlocksMined     = 0f;
        rateKills           = 0f;
        rateCustom.clear();
        sessionStartTime    = System.currentTimeMillis();
        rateWindowStart     = System.currentTimeMillis();
    }

    public static int   getSessionBlocksPlaced()      { return sessionBlocksPlaced; }
    public static int   getSessionBlocksMined()       { return sessionBlocksMined; }
    public static int   getSessionKills()             { return sessionKills; }
    public static int   getSessionCustom(int i)       { return sessionCustom.getOrDefault(i, 0); }
    public static float getRateBlocksPlaced()         { return rateBlocksPlaced; }
    public static float getRateBlocksMined()          { return rateBlocksMined; }
    public static float getRateKills()                { return rateKills; }
    public static float getRateCustom(int i)          { return rateCustom.getOrDefault(i, 0f); }
    public static boolean isAdAvailable()             { return adAvailable; }
    public static long  getLastAdTime()               { return lastAdTime; }
    public static long  getAdCooldownMs()             { return config.adCooldownMs; }
    public static long  getSessionStartTime()         { return sessionStartTime; }
    public static long  getConnectionTime()           { return connectionTime; }
    public static long  getNoticeDurationMs()         { return NOTICE_DURATION_MS; }
    public static void  onJoined()                    { connectionTime = System.currentTimeMillis(); onStatsRefreshed(); }

    private static long lastStatsRefreshTime = 0;
    private static final long STATS_REFRESH_COOLDOWN_MS = 15 * 60 * 1000L;
    public static boolean canRefreshStats() { return System.currentTimeMillis() - lastStatsRefreshTime >= STATS_REFRESH_COOLDOWN_MS; }
    public static long getStatsRefreshCooldownRemaining() { return Math.max(0, STATS_REFRESH_COOLDOWN_MS - (System.currentTimeMillis() - lastStatsRefreshTime)); }
    public static void onStatsRefreshed() { lastStatsRefreshTime = System.currentTimeMillis(); }
    public static boolean shouldRequestStats()        { return activitySinceLastRequest; }
    public static void  clearActivityFlag()           { activitySinceLastRequest = false; }
    public static OBFHud getHud()                     { return hud; }

    private static void importWaypointData(String raw) {
        if (raw.startsWith("OBF-WP:")) {
            Waypoint wp = Waypoint.fromShareString(raw.substring(7), WaypointManager.getCurrentDimension());
            if (wp != null) { WaypointManager.add(wp); LOGGER.info("[OBF] Waypoint importé: " + wp.name); }
        } else if (raw.startsWith("OBF-WPS:")) {
            String[] parts = raw.substring(8).split(";");
            for (String part : parts) {
                Waypoint wp = Waypoint.fromShareString(part, WaypointManager.getCurrentDimension());
                if (wp != null) { WaypointManager.add(wp); LOGGER.info("[OBF] Waypoint importé: " + wp.name); }
            }
        }
    }
}