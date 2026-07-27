package com.hamtabot.obfutilities.autotool;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.BlockPos;

public class AutoTool {

    public static void onBlockLook(BlockPos pos) {
        if (!OBFUtilities.config.autoToolEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.currentScreen != null) return;

        BlockState state = client.world.getBlockState(pos);
        ClientPlayerEntity player = client.player;

        int bestSlot = findBestSlot(player, state);
        if (bestSlot != -1 && bestSlot != player.getInventory().selectedSlot) {
            player.getInventory().selectedSlot = bestSlot;
        }
    }

    private static int findBestSlot(ClientPlayerEntity player, BlockState state) {
        ModConfig cfg = OBFUtilities.config;
        float bestSpeed = 1.0f; // vitesse de la main nue
        int bestSlot    = -1;
        int selectedSlot = player.getInventory().selectedSlot;
        boolean selectedIsLow = false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (!isToolAllowed(stack.getItem(), cfg)) continue;

            // 10 me parait bien si y'a un lag vue que la dura est geré coté serv
            // TODO: mettre la valeur configurable
            if (cfg.autoToolSkipLowDurability && stack.isDamageable()) {
                int remaining = stack.getMaxDamage() - stack.getDamage();
                if (remaining <= 10) {
                    if (i == selectedSlot) selectedIsLow = true;
                    continue;
                }
            }

            float speed = getEffectiveSpeed(stack, state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot  = i;
            }
        }

        if (bestSlot == -1 && selectedIsLow) {
            int fallbackSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isEmpty()) return i; // main nue

                if (fallbackSlot == -1 && !isToolAllowed(stack.getItem(), cfg)) {
                    fallbackSlot = i;
                }
            }
            if (fallbackSlot != -1) return fallbackSlot;
        }

        return bestSlot;
    }

    private static boolean isToolAllowed(Item item, ModConfig cfg) {
        if (item instanceof PickaxeItem)  return cfg.autoToolUsePioche;
        if (item instanceof ShovelItem)   return cfg.autoToolUsePelle;
        if (item instanceof AxeItem)      return cfg.autoToolUseHache;
        if (item instanceof HoeItem)      return cfg.autoToolUseHoue;
        if (item instanceof ShearsItem)   return cfg.autoToolUseCisaille;
        if (item instanceof SwordItem)    return cfg.autoToolUseEpee;
        return false;
    }

    private static float getEffectiveSpeed(ItemStack stack, BlockState state) {
        Item item = stack.getItem();

        // boost multi épée sinon ca marche pas
        if (item instanceof SwordItem) {
            float speed = stack.getMiningSpeedMultiplier(state);
            return Math.max(speed, 1.5f);
        }

        return stack.getMiningSpeedMultiplier(state);
    }
}