package com.hamtabot.obfutilities.stats;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import net.minecraft.client.MinecraftClient;

import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerStats {

    private static final String COMMAND = "utilitiesstats";

    private static final long REFRESH_COOLDOWN_MS = 15 * 60 * 1000L;
    private static final long NOTICE_DURATION_MS  = 5 * 60 * 1000L;
    private static final long JOIN_DELAY_MS       = 3000;

    private static final Pattern MINED_PATTERN  = Pattern.compile("blocs min[ée]s\\s*:\\s*([\\d,\\.]{1,20})", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACED_PATTERN = Pattern.compile("blocs pos[ée]s\\s*:\\s*([\\d,\\.]{1,20})", Pattern.CASE_INSENSITIVE);
    private static final Pattern KILLS_PATTERN  = Pattern.compile("mobs tu[ée]s\\s*:\\s*([\\d,\\.]{1,20})", Pattern.CASE_INSENSITIVE);

    private static long lastRefreshTime = 0;
    private static long connectionTime  = System.currentTimeMillis();
    private static long pendingSendAt   = 0;

    public static void onJoined() {
        connectionTime  = System.currentTimeMillis();
        lastRefreshTime = connectionTime;
        pendingSendAt   = connectionTime + JOIN_DELAY_MS;
    }

    public static void tick(MinecraftClient client) {
        if (pendingSendAt == 0 || System.currentTimeMillis() < pendingSendAt) return;
        pendingSendAt = 0;
        if (client.player != null) sendCommand();
    }

    public static void refresh() {
        if (!canRefresh()) return;
        sendCommand();
        lastRefreshTime = System.currentTimeMillis();
    }

    private static void sendCommand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) client.getNetworkHandler().sendChatCommand(COMMAND);
    }

    public static boolean handleServerMessage(String text) {
        ModConfig cfg = OBFUtilities.config;
        boolean matched = false;
        matched |= parse(MINED_PATTERN,  text, v -> cfg.serverTotalMined  = v);
        matched |= parse(PLACED_PATTERN, text, v -> cfg.serverTotalPlaced = v);
        matched |= parse(KILLS_PATTERN,  text, v -> cfg.serverTotalKills  = v);
        return matched;
    }

    private static boolean parse(Pattern pattern, String text, IntConsumer setter) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) return false;
        try {
            setter.accept(Integer.parseInt(m.group(1).replace(",", "").replace(".", "")));
            return true;
        } catch (NumberFormatException e) { return false; }
    }

    public static boolean canRefresh() { return getRefreshCooldownRemaining() == 0; }

    public static long getRefreshCooldownRemaining() {
        return Math.max(0, REFRESH_COOLDOWN_MS - (System.currentTimeMillis() - lastRefreshTime));
    }

    public static long getNoticeRemainingMs() {
        return Math.max(0, NOTICE_DURATION_MS - (System.currentTimeMillis() - connectionTime));
    }
}
