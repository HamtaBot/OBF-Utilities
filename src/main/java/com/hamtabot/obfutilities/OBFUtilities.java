package com.hamtabot.obfutilities;

import com.hamtabot.obfutilities.blockshade.BlockShade;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.debug.DebugOverlay;
import com.hamtabot.obfutilities.fullbright.FullBright;
import com.hamtabot.obfutilities.grade.GradeHud;
import com.hamtabot.obfutilities.grade.GradeTracker;
import com.hamtabot.obfutilities.hud.InventoryHud;
import com.hamtabot.obfutilities.hud.OBFHud;
import com.hamtabot.obfutilities.input.KeyRemap;
import com.hamtabot.obfutilities.kit.KitHud;
import com.hamtabot.obfutilities.kit.KitTracker;
import com.hamtabot.obfutilities.input.Keybinds;
import com.hamtabot.obfutilities.stats.AdTimer;
import com.hamtabot.obfutilities.stats.AfkTimer;
import com.hamtabot.obfutilities.stats.ServerStats;
import com.hamtabot.obfutilities.stats.SessionStats;
import com.hamtabot.obfutilities.stats.SessionStore;
import com.hamtabot.obfutilities.stats.StatsReader;
import com.hamtabot.obfutilities.storage.StorageUnit;
import com.hamtabot.obfutilities.timer.Timers;
import com.hamtabot.obfutilities.ui.HudEditScreen;
import com.hamtabot.obfutilities.warp.WarpMacros;
import com.hamtabot.obfutilities.waypoint.WaypointCommand;
import com.hamtabot.obfutilities.waypoint.WaypointManager;
import com.hamtabot.obfutilities.waypoint.WaypointRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OBFUtilities implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("obfutilities");
    public static final String MOD_ID = "obfutilities";

    public static ModConfig config;
    private static OBFHud hud;

    @Override
    public void onInitializeClient() {
        config = ModConfig.load();
        hud = new OBFHud();
        SessionStore.load();
        AfkTimer.load();
        KitTracker.load();
        GradeTracker.load();

        Keybinds.register();
        StorageUnit.register();
        WaypointManager.load();
        WaypointCommand.register();
        WaypointRenderer.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(KeyRemap::applySaved);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> { SessionStore.save(); AfkTimer.save(); });

        HudRenderCallback.EVENT.register((ctx, delta) -> {
            if (MinecraftClient.getInstance().currentScreen instanceof HudEditScreen) return;
            InventoryHud.render(ctx, false);
            hud.render(ctx, false);
            DebugOverlay.render(ctx, false);
            Timers.render(ctx, false);
            KitHud.render(ctx, false);
            GradeHud.render(ctx, false);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FullBright.tick();
            BlockShade.tick(client);
            if (client.player == null) return;
            SessionStats.tick();
            SessionStore.tick();
            AdTimer.tick();
            AfkTimer.tick(client);
            Timers.tick(client);
            StatsReader.tick(client);
            ServerStats.tick(client);
            Keybinds.tick(client);
            WarpMacros.tick(client);
            StorageUnit.tick(client);
            KitTracker.tick(client);
            GradeTracker.tick(client);
        });
    }

    public static OBFHud getHud() { return hud; }
}
