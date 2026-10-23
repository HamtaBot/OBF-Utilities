package com.hamtabot.obfutilities.stats;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

import java.util.IdentityHashMap;
import java.util.Map;

public class StatsReader {

    private static final int  POLL_TICKS       = 40;
    private static final long AFK_THRESHOLD_MS = 30_000;

    private static final Map<ModConfig.CustomBlockEntry, Integer> cachedCustomTotals = new IdentityHashMap<>();

    private static int tickCounter = 0;
    private static double lastX = 0, lastY = 0, lastZ = 0;
    private static long lastActivityTime = System.currentTimeMillis();

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;

        double cx = client.player.getX(), cy = client.player.getY(), cz = client.player.getZ();
        if (Math.abs(cx-lastX) > 0.01 || Math.abs(cy-lastY) > 0.01 || Math.abs(cz-lastZ) > 0.01) {
            lastActivityTime = System.currentTimeMillis();
            lastX = cx; lastY = cy; lastZ = cz;
        }
        if (SessionStats.consumeActivity()) lastActivityTime = System.currentTimeMillis();

        if (++tickCounter < POLL_TICKS) return;
        tickCounter = 0;

        boolean isAfk = System.currentTimeMillis() - lastActivityTime > AFK_THRESHOLD_MS;
        if (!isAfk) requestStatsUpdate(client);

        cachedCustomTotals.clear();
        for (ModConfig.CustomBlockEntry e : OBFUtilities.config.customBlocks) {
            if (e.enabled) cachedCustomTotals.put(e, readCustomTotal(client, e));
        }
    }

    public static int getCustomTotal(ModConfig.CustomBlockEntry entry) {
        return cachedCustomTotals.getOrDefault(entry, -1);
    }

    private static void requestStatsUpdate(MinecraftClient client) {
        try {
            if (client.getNetworkHandler() == null) return;
            client.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));
        } catch (Exception e) {
            OBFUtilities.LOGGER.error("[OBF] Erreur requête stats: " + e.getMessage());
        }
    }

    private static int readCustomTotal(MinecraftClient client, ModConfig.CustomBlockEntry entry) {
        try {
            Identifier id = new Identifier(entry.blockId);
            if (entry.trackPlaced) {
                Item item = Registries.ITEM.get(id);
                return client.player.getStatHandler().getStat(Stats.USED.getOrCreateStat(item));
            }
            Block block = Registries.BLOCK.get(id);
            return client.player.getStatHandler().getStat(Stats.MINED.getOrCreateStat(block));
        } catch (Exception e) { return -1; }
    }
}
