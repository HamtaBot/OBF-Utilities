package com.hamtabot.obfutilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

public class StatsReader {

    public static void requestStatsUpdate() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.getNetworkHandler() == null) return;
            client.getNetworkHandler().sendPacket(
                    new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS)
            );
            OBFUtilities.clearActivityFlag();
        } catch (Exception e) {
            OBFUtilities.LOGGER.error("[OBF] Erreur requête stats: " + e.getMessage());
        }
    }

    public static int getTotalBlocksPlaced() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return -1;
            int total = 0;
            // Itérer sur les blocs, récupérer leur item correspondant via BlockItem
            for (net.minecraft.block.Block block : Registries.BLOCK) {
                Item item = Registries.ITEM.get(Registries.BLOCK.getId(block));
                if (item instanceof net.minecraft.item.BlockItem) {
                    total += client.player.getStatHandler().getStat(Stats.USED.getOrCreateStat(item));
                }
            }
            return total;
        } catch (Exception e) { return -1; }
    }

    public static int getTotalBlocksMined() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return -1;
            int total = 0;
            for (net.minecraft.block.Block block : Registries.BLOCK) {
                total += client.player.getStatHandler().getStat(Stats.MINED.getOrCreateStat(block));
            }
            return total;
        } catch (Exception e) { return -1; }
    }

    public static int getTotalMobsKilled() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return -1;
            int total = 0;
            for (EntityType<?> type : Registries.ENTITY_TYPE) {
                total += client.player.getStatHandler().getStat(Stats.KILLED.getOrCreateStat(type));
            }
            return total;
        } catch (Exception e) { return -1; }
    }

    public static int getTotalCustomBlock(String blockId, boolean placed) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return -1;
            Identifier id = new Identifier(blockId);
            if (placed) {
                Item item = Registries.ITEM.get(id);
                return client.player.getStatHandler().getStat(Stats.USED.getOrCreateStat(item));
            } else {
                net.minecraft.block.Block block = Registries.BLOCK.get(id);
                return client.player.getStatHandler().getStat(Stats.MINED.getOrCreateStat(block));
            }
        } catch (Exception e) { return -1; }
    }
}