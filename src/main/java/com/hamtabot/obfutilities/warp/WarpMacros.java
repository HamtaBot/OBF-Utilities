package com.hamtabot.obfutilities.warp;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.stats.ServerStats;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class WarpMacros {

    private static final int  REQUEST_ID      = 0x0BF0BF;
    private static final long RESPONSE_WAIT_MS = 5000;
    private static final Pattern VALID_NAME   = Pattern.compile("[\\p{L}\\p{N}_\\-.:]{1,64}");

    private static long pendingRequestAt = 0;
    private static long awaitingUntil    = 0;
    private static final Set<String>  found    = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private static int version = 0;

    private WarpMacros() {}

    public static void onJoined() {
        pendingRequestAt = System.currentTimeMillis() + ServerStats.JOIN_DELAY_MS;
    }

    public static void refresh() {
        pendingRequestAt = System.currentTimeMillis();
    }

    public static boolean isSyncing() { return pendingRequestAt != 0 || awaitingUntil != 0; }

    public static int version() { return version; }

    public static void tick(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (pendingRequestAt != 0 && now >= pendingRequestAt) {
            pendingRequestAt = 0;
            request(client);
        }
        if (awaitingUntil != 0 && now >= awaitingUntil) finish();
    }

    private static void request(MinecraftClient client) {
        ClientPlayNetworkHandler net = client.getNetworkHandler();
        if (net == null) return;
        found.clear();
        CommandNode<CommandSource> warp = net.getCommandDispatcher().getRoot().getChild("warp");
        if (warp != null) {
            for (CommandNode<CommandSource> child : warp.getChildren()) {
                if (child instanceof LiteralCommandNode) addName(child.getName());
            }
        }
        net.sendPacket(new RequestCommandCompletionsC2SPacket(REQUEST_ID, "/warp "));
        awaitingUntil = System.currentTimeMillis() + RESPONSE_WAIT_MS;
    }

    public static boolean onSuggestions(CommandSuggestionsS2CPacket packet) {
        if (packet.getCompletionId() != REQUEST_ID) return false;
        for (Suggestion s : packet.getSuggestions().getList()) addName(s.getText());
        finish();
        return true;
    }

    private static void addName(String name) {
        if (name != null && VALID_NAME.matcher(name).matches()) found.add(name);
    }

    private static void finish() {
        awaitingUntil = 0;
        if (found.isEmpty()) return;

        ModConfig cfg = OBFUtilities.config;
        if (cfg.warps.stream().map(w -> w.name).toList().equals(new ArrayList<>(found))) return;

        Map<String, Integer> oldKeys = new HashMap<>();
        for (ModConfig.WarpBind w : cfg.warps) oldKeys.put(w.name.toLowerCase(), w.key);

        List<ModConfig.WarpBind> updated = new ArrayList<>();
        for (String name : found) {
            ModConfig.WarpBind w = new ModConfig.WarpBind(name);
            w.key = oldKeys.getOrDefault(name.toLowerCase(), -1);
            updated.add(w);
        }
        cfg.warps = updated;
        cfg.save();
        version++;
    }

    public static void setKey(ModConfig.WarpBind warp, int key) {
        if (key >= 0) {
            for (ModConfig.WarpBind w : OBFUtilities.config.warps) {
                if (w != warp && w.key == key) w.key = -1;
            }
        }
        warp.key = key;
        OBFUtilities.config.save();
    }

    public static void onKeyPressed(MinecraftClient client, int key) {
        if (client.player == null) return;
        for (ModConfig.WarpBind w : OBFUtilities.config.warps) {
            if (w.key == key) {
                client.player.networkHandler.sendChatCommand("warp " + w.name);
                return;
            }
        }
    }
}
