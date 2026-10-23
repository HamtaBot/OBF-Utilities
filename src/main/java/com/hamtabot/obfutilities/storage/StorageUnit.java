package com.hamtabot.obfutilities.storage;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public final class StorageUnit {

    private static final int UNIT_SLOTS = 5;
    private static final int GUI_W = 176, GUI_H = 133;
    private static final int BTN_W = 80, BTN_H = 20;
    private static final int MAX_WITHDRAW_CLICKS = 200;
    private static final int MAX_DEPOSIT_ATTEMPTS = 3;
    private static final int IDLE_LIMIT_TICKS = 10;

    private static final Text DEPOSIT_LABEL  = Text.literal("Tout déposer");
    private static final Text WITHDRAW_LABEL = Text.literal("Tout prendre");
    private static final Text STOP_LABEL     = Text.literal("Arrêter");

    private enum Mode { DEPOSIT, WITHDRAW }

    private static Mode mode;
    private static int syncId, unitSlot, clicks, inFlight, lastCount, idleTicks, missingTicks;
    private static ItemStack unitItem = ItemStack.EMPTY;
    private static final Map<Integer, Integer> attempts = new HashMap<>();

    private StorageUnit() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof HopperScreen hopper)) return;
            ModConfig cfg = OBFUtilities.config;
            if (!cfg.storageDepositEnabled && !cfg.storageWithdrawEnabled) return;

            int gx = (width - GUI_W) / 2, gy = (height - GUI_H) / 2;
            int bx = gx + GUI_W + 4;
            if (bx + BTN_W > width) bx = gx - 4 - BTN_W;
            int by = gy;
            if (bx < 0) { bx = gx; by = gy - 2 * (BTN_H + 2); }

            ButtonWidget deposit = ButtonWidget.builder(DEPOSIT_LABEL, b -> toggle(hopper, Mode.DEPOSIT))
                    .dimensions(bx, by, BTN_W, BTN_H).build();
            ButtonWidget withdraw = ButtonWidget.builder(WITHDRAW_LABEL, b -> toggle(hopper, Mode.WITHDRAW))
                    .dimensions(bx, by + BTN_H + 2, BTN_W, BTN_H).build();
            if (cfg.storageDepositEnabled) Screens.getButtons(screen).add(deposit);
            if (cfg.storageWithdrawEnabled) Screens.getButtons(screen).add(withdraw);

            ScreenEvents.afterTick(screen).register(s -> {
                HopperScreenHandler handler = hopper.getScreenHandler();
                int slot = findUnitSlot(handler);
                boolean isUnit = slot >= 0;
                boolean hasItem = isUnit && !storedItem(handler, slot).isEmpty();
                deposit.visible  = isUnit || mode == Mode.DEPOSIT;
                withdraw.visible = isUnit || mode == Mode.WITHDRAW;
                deposit.setMessage(mode == Mode.DEPOSIT ? STOP_LABEL : DEPOSIT_LABEL);
                withdraw.setMessage(mode == Mode.WITHDRAW ? STOP_LABEL : WITHDRAW_LABEL);
                deposit.active  = mode == Mode.DEPOSIT  || (mode == null && hasItem);
                withdraw.active = mode == Mode.WITHDRAW || (mode == null && hasItem);
            });
        });
    }

    private static int findUnitSlot(HopperScreenHandler handler) {
        for (int u = 0; u < UNIT_SLOTS; u++) {
            if (isFrame(handler, u)) return u;
        }
        return -1;
    }

    private static boolean isFrame(HopperScreenHandler handler, int unit) {
        ItemStack filler = ItemStack.EMPTY;
        for (int i = 0; i < UNIT_SLOTS; i++) {
            if (i == unit) continue;
            ItemStack s = handler.getSlot(i).getStack();
            if (s.isEmpty()) return false;
            if (filler.isEmpty()) filler = s;
            else if (!ItemStack.canCombine(s, filler)) return false;
        }
        return !ItemStack.canCombine(handler.getSlot(unit).getStack(), filler);
    }

    private static ItemStack storedItem(HopperScreenHandler handler, int unit) {
        ItemStack frame = handler.getSlot(unit == 0 ? 1 : 0).getStack();
        return frame.isOf(Items.BLACK_STAINED_GLASS_PANE) ? ItemStack.EMPTY : frame;
    }

    private static void toggle(HopperScreen screen, Mode wanted) {
        if (mode != null) { stop(); return; }
        HopperScreenHandler handler = screen.getScreenHandler();
        int slot = findUnitSlot(handler);
        if (slot < 0) return;
        ItemStack stored = storedItem(handler, slot);
        if (stored.isEmpty()) return;
        mode = wanted;
        syncId = handler.syncId;
        unitSlot = slot;
        unitItem = stored.copy();
        clicks = 0;
        inFlight = 0;
        lastCount = -1;
        idleTicks = 0;
        missingTicks = 0;
        attempts.clear();
    }

    private static void stop() {
        mode = null;
        attempts.clear();
    }

    private static boolean matches(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(unitItem.getItem())) return false;
        return customModelData(stack) == customModelData(unitItem);
    }

    private static int customModelData(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains("CustomModelData", NbtElement.NUMBER_TYPE) ? nbt.getInt("CustomModelData") : 0;
    }

    public static void tick(MinecraftClient client) {
        if (mode == null) return;
        if (client.interactionManager == null) {
            stop();
            return;
        }
        HopperScreenHandler handler = client.currentScreen instanceof HopperScreen screen ? screen.getScreenHandler() : null;
        int slot = handler != null ? findUnitSlot(handler) : -1;
        if (slot < 0) {
            if (++missingTicks > IDLE_LIMIT_TICKS) stop();
            return;
        }
        missingTicks = 0;
        if (handler.syncId != syncId) {
            ItemStack stored = storedItem(handler, slot);
            if (!stored.isEmpty() && !stored.isOf(unitItem.getItem())) {
                stop();
                return;
            }
            syncId = handler.syncId;
            unitSlot = slot;
            inFlight = 0;
            idleTicks = 0;
        }

        ItemStack cursor = handler.getCursorStack();
        if (!cursor.isEmpty()) {
            int target = freeSlotFor(handler, cursor);
            if (target < 0) stop();
            else client.interactionManager.clickSlot(syncId, target, 0, SlotActionType.PICKUP, client.player);
            return;
        }

        if (mode == Mode.DEPOSIT) {
            for (int i = UNIT_SLOTS; i < handler.slots.size(); i++) {
                int tries = attempts.getOrDefault(i, 0);
                if (tries >= MAX_DEPOSIT_ATTEMPTS || !matches(handler.getSlot(i).getStack())) continue;
                attempts.put(i, tries + 1);
                idleTicks = 0;
                client.interactionManager.clickSlot(syncId, i, 0, SlotActionType.QUICK_MOVE, client.player);
                return;
            }
            if (++idleTicks > IDLE_LIMIT_TICKS) stop();
            return;
        }

        if (storedItem(handler, unitSlot).isEmpty()) {
            stop();
            return;
        }
        PlayerInventory inv = client.player.getInventory();
        int count = countMatching(inv);
        if (lastCount >= 0 && count > lastCount) {
            int stack = unitItem.getMaxCount();
            inFlight = Math.max(0, inFlight - (count - lastCount + stack - 1) / stack);
            idleTicks = 0;
        } else if (lastCount >= 0) {
            idleTicks++;
        }
        lastCount = count;

        int room = freeStacks(inv);
        if (idleTicks > IDLE_LIMIT_TICKS || clicks >= MAX_WITHDRAW_CLICKS || (room == 0 && inFlight == 0)) {
            stop();
            return;
        }
        if (inFlight >= room) return;
        clicks++;
        inFlight++;
        client.interactionManager.clickSlot(syncId, unitSlot, 0, SlotActionType.QUICK_MOVE, client.player);
    }

    private static int freeSlotFor(HopperScreenHandler handler, ItemStack stack) {
        int empty = -1;
        for (int i = UNIT_SLOTS; i < handler.slots.size(); i++) {
            ItemStack s = handler.getSlot(i).getStack();
            if (s.isEmpty()) { if (empty < 0) empty = i; continue; }
            if (ItemStack.canCombine(s, stack) && s.getCount() < s.getMaxCount()) return i;
        }
        return empty;
    }

    private static int freeStacks(PlayerInventory inv) {
        int empty = 0, partial = 0;
        for (int i = 0; i < inv.main.size(); i++) {
            ItemStack s = inv.main.get(i);
            if (s.isEmpty()) empty++;
            else if (matches(s) && s.getCount() < s.getMaxCount()) partial += s.getMaxCount() - s.getCount();
        }
        return empty + partial / unitItem.getMaxCount();
    }

    private static int countMatching(PlayerInventory inv) {
        int count = 0;
        for (ItemStack s : inv.main) if (matches(s)) count += s.getCount();
        return count;
    }
}
