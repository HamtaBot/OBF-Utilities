package com.hamtabot.obfutilities.hud;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class InventoryHud {

    private static final int PAD = 5, CELL = 18, PITCH = 19, COLS = 9, HOTBAR_GAP = 5;
    public static final int W = PAD * 2 + COLS * PITCH - 1;
    private static final int H_MAIN = PAD * 2 + 3 * PITCH - 1;
    private static final int H_FULL = H_MAIN + PITCH + HOTBAR_GAP;
    public static final int DEFAULT_X = 10, DEFAULT_Y = 60;
    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2f;

    public static void render(DrawContext ctx, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        ModConfig cfg = OBFUtilities.config;
        if (client.player == null || !cfg.invHudEnabled) return;
        PlayerInventory inv = client.player.getInventory();
        float op = cfg.invHudOpacity;
        int[] b = bounds();
        int h = height(), rows = cfg.invHudShowHotbar ? 4 : 3;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(b[0], b[1], 0);
        ctx.getMatrices().scale(cfg.invHudScale, cfg.invHudScale, 1f);

        // fill/drawText flush the buffer on every call unless wrapped in ctx.draw(Runnable),
        // so backgrounds, items and overlays are each drawn in their own batch
        ctx.draw(() -> {
            if (op > 0f) {
                Draw.card(ctx, 0, 0, W, h, alpha(Theme.WINDOW, op), alpha(Theme.WINDOW_BORDER, op));
                if (cfg.invHudShowHotbar) {
                    int lineY = PAD + 3 * PITCH - 1 + HOTBAR_GAP / 2;
                    ctx.fill(PAD, lineY, W - PAD, lineY + 1, alpha(Theme.DIVIDER, op));
                }
                int surface = alpha(Theme.SURFACE, op), border = alpha(Theme.BORDER, op), accent = alpha(Theme.ACCENT, op);
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < COLS; col++) {
                        boolean selected = row == 3 && col == inv.selectedSlot;
                        Draw.card(ctx, slotX(col), slotY(row), CELL, CELL, surface, selected ? accent : border);
                    }
                }
            }
            if (editing) ctx.drawBorder(0, 0, W, h, Theme.ACCENT);
        });

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < COLS; col++) {
                ItemStack stack = inv.getStack(slotIndex(row, col));
                if (!stack.isEmpty()) ctx.drawItem(stack, slotX(col) + 1, slotY(row) + 1);
            }
        }

        ctx.draw(() -> {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < COLS; col++) {
                    ItemStack stack = inv.getStack(slotIndex(row, col));
                    if (!stack.isEmpty()) ctx.drawItemInSlot(client.textRenderer, stack, slotX(col) + 1, slotY(row) + 1);
                }
            }
        });
        ctx.getMatrices().pop();

        if (editing) Draw.resizeGrip(ctx, b[0] + b[2], b[1] + b[3]);
    }

    private static int slotIndex(int row, int col) { return row < 3 ? 9 + row * COLS + col : col; }
    private static int slotX(int col) { return PAD + col * PITCH; }
    private static int slotY(int row) { return PAD + row * PITCH + (row == 3 ? HOTBAR_GAP : 0); }

    private static int height() { return OBFUtilities.config.invHudShowHotbar ? H_FULL : H_MAIN; }

    private static int alpha(int color, float factor) {
        int a = Math.round((color >>> 24) * factor);
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int[] bounds() {
        MinecraftClient client = MinecraftClient.getInstance();
        ModConfig cfg = OBFUtilities.config;
        int w = Math.round(W * cfg.invHudScale), h = Math.round(height() * cfg.invHudScale);
        int[] p = cfg.invHudPos.resolve(w, h, DEFAULT_X, DEFAULT_Y);
        return new int[]{ p[0], p[1], w, h };
    }

    public static boolean isMouseOver(double mx, double my) {
        if (!OBFUtilities.config.invHudEnabled) return false;
        int[] b = bounds();
        return Draw.inside(mx, my, b[0], b[1], b[2], b[3]);
    }

    public static boolean isInResizeGrip(double mx, double my) {
        if (!OBFUtilities.config.invHudEnabled) return false;
        int[] b = bounds();
        return Draw.inResizeGrip(mx, my, b[0] + b[2], b[1] + b[3]);
    }

    public static void setPos(int x, int y) {
        int[] b = bounds();
        OBFUtilities.config.invHudPos.set(x, y, b[2], b[3]);
    }

    public static void setScale(float s) {
        OBFUtilities.config.invHudScale = clampScale(s);
    }

    public static float clampScale(float s) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
    }
}
