package com.hamtabot.obfutilities.autotool;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class AutoTool {

    // 10 me parait bien si y'a un lag vue que la dura est geré coté serv
    private static final int LOW_DURABILITY_THRESHOLD = 10;

    public static void onBlockLook(BlockPos pos) {
        if (!OBFUtilities.config.autoToolEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.currentScreen != null) return;

        BlockState state = client.world.getBlockState(pos);
        ClientPlayerEntity player = client.player;

        switchTo(player, findBestMiningSlot(player, state));
    }

    public static void onEntityAttack(Entity target) {
        if (!OBFUtilities.config.autoToolEnabled) return;
        if (!(target instanceof LivingEntity)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.currentScreen != null) return;

        ClientPlayerEntity player = client.player;

        switchTo(player, findBestCombatSlot(player));
    }

    private static void switchTo(ClientPlayerEntity player, int slot) {
        if (slot != -1 && slot != player.getInventory().selectedSlot) {
            player.getInventory().selectedSlot = slot;
        }
    }

    private static int findBestMiningSlot(ClientPlayerEntity player, BlockState state) {
        ModConfig cfg = OBFUtilities.config;
        float bestSpeed  = 1.0f; // vitesse de la main nue
        int bestSlot     = -1;
        int selectedSlot = player.getInventory().selectedSlot;
        boolean selectedIsWorn = false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (!isToolAllowed(stack.getItem(), cfg)) continue;

            if (isTooWorn(stack, cfg)) {
                if (i == selectedSlot) selectedIsWorn = true;
                continue;
            }

            float speed = getEffectiveSpeed(stack, state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot  = i;
            }
        }

        if (bestSlot == -1 && selectedIsWorn) {
            return fallbackAwayFromWornTool(player, item -> isToolAllowed(item, cfg));
        }

        return bestSlot;
    }

    private static int findBestCombatSlot(ClientPlayerEntity player) {
        ModConfig cfg = OBFUtilities.config;
        if (!cfg.autoToolUseEpee) return -1;

        float bestDamage = -1f;
        int bestSlot      = -1;
        int selectedSlot  = player.getInventory().selectedSlot;
        boolean selectedIsWorn = false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (!(stack.getItem() instanceof SwordItem)) continue;

            if (isTooWorn(stack, cfg)) {
                if (i == selectedSlot) selectedIsWorn = true;
                continue;
            }

            float damage = getAttackDamage(stack);
            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot   = i;
            }
        }

        if (bestSlot == -1 && selectedIsWorn) {
            return fallbackAwayFromWornTool(player, item -> item instanceof SwordItem);
        }

        return bestSlot;
    }

    private static boolean isTooWorn(ItemStack stack, ModConfig cfg) {
        if (!cfg.autoToolSkipLowDurability || !stack.isDamageable()) return false;
        int remaining = stack.getMaxDamage() - stack.getDamage();
        return remaining <= LOW_DURABILITY_THRESHOLD;
    }

    private static int fallbackAwayFromWornTool(ClientPlayerEntity player, Predicate<Item> managedByAutoTool) {
        int fallbackSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) return i;

            if (fallbackSlot == -1 && !managedByAutoTool.test(stack.getItem())) {
                fallbackSlot = i;
            }
        }
        return fallbackSlot;
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
        if (item instanceof SwordItem) {
            float speed = stack.getMiningSpeedMultiplier(state);
            return Math.max(speed, 1.5f);
        }

        return stack.getMiningSpeedMultiplier(state);
    }

    private static float getAttackDamage(ItemStack stack) {
        float damage = 0f;
        for (EntityAttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
            damage += modifier.getValue();
        }
        return damage;
    }
}
