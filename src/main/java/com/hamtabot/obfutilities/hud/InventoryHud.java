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
    public static final int H = PAD * 2 + 4 * PITCH - 1 + HOTBAR_GAP;
    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2f;

    public static void render(DrawContext ctx, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        ModConfig cfg = OBFUtilities.config;
        if (client.player == null || !cfg.invHudEnabled) return;
        PlayerInventory inv = client.player.getInventory();
        float op = cfg.invHudOpacity;
        int[] b = bounds();

        ctx.getMatrices().push();
        ctx.getMatrices().translate(b[0], b[1], 0);
        ctx.getMatrices().scale(cfg.invHudScale, cfg.invHudScale, 1f);

        if (op > 0f) {
            Draw.card(ctx, 0, 0, W, H, alpha(Theme.WINDOW, op), alpha(Theme.WINDOW_BORDER, op));
            int lineY = PAD + 3 * PITCH - 1 + HOTBAR_GAP / 2;
            ctx.fill(PAD, lineY, W - PAD, lineY + 1, alpha(Theme.DIVIDER, op));
        }
        if (editing) ctx.drawBorder(0, 0, W, H, Theme.ACCENT);

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = row < 3 ? 9 + row * COLS + col : col;
                int sx = PAD + col * PITCH;
                int sy = PAD + row * PITCH + (row == 3 ? HOTBAR_GAP : 0);
                if (op > 0f) {
                    boolean selected = row == 3 && col == inv.selectedSlot;
                    Draw.card(ctx, sx, sy, CELL, CELL, alpha(Theme.SURFACE, op),
                            alpha(selected ? Theme.ACCENT : Theme.BORDER, op));
                }
                ItemStack stack = inv.getStack(slot);
                if (stack.isEmpty()) continue;
                ctx.drawItem(stack, sx + 1, sy + 1);
                ctx.drawItemInSlot(client.textRenderer, stack, sx + 1, sy + 1);
            }
        }
        ctx.getMatrices().pop();

        if (editing) Draw.resizeGrip(ctx, b[0] + b[2], b[1] + b[3]);
    }

    private static int alpha(int color, float factor) {
        int a = Math.round((color >>> 24) * factor);
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int[] bounds() {
        MinecraftClient client = MinecraftClient.getInstance();
        ModConfig cfg = OBFUtilities.config;
        int w = Math.round(W * cfg.invHudScale), h = Math.round(H * cfg.invHudScale);
        int x = Math.max(0, Math.min(cfg.invHudX, client.getWindow().getScaledWidth()  - w));
        int y = Math.max(0, Math.min(cfg.invHudY, client.getWindow().getScaledHeight() - h));
        return new int[]{ x, y, w, h };
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
        OBFUtilities.config.invHudX = x;
        OBFUtilities.config.invHudY = y;
    }

    public static void setScale(float s) {
        OBFUtilities.config.invHudScale = clampScale(s);
    }

    public static float clampScale(float s) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
    }
}
