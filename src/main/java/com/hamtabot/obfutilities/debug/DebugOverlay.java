package com.hamtabot.obfutilities.debug;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

public class DebugOverlay {

    private static boolean draggingFps    = false;
    private static boolean draggingCoords = false;
    private static boolean draggingRam    = false;
    private static int dragFpsOffX, dragFpsOffY;
    private static int dragCoordsOffX, dragCoordsOffY;
    private static int dragRamOffX, dragRamOffY;

    private static boolean wasMouseDown = false;

    public static boolean cfgDraggingFps    = false;
    public static boolean cfgDraggingCoords = false;
    public static boolean cfgDraggingRam    = false;
    public static int cfgDragFpsOffX, cfgDragFpsOffY;
    public static int cfgDragCoordsOffX, cfgDragCoordsOffY;
    public static int cfgDragRamOffX, cfgDragRamOffY;

    private static final int PAD_X = 7;
    private static final int PAD_Y = 4;
    private static final int H = 16;

    private static final int BG_BASE       = 0xCC0A0A12;
    private static final int ACCENT_FPS    = 0xFF4FC3F7;
    private static final int ACCENT_COORDS = 0xFF80CBC4;
    private static final int ACCENT_RAM    = 0xFFCE93D8;

    public static boolean inConfigScreen = false;

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        TextRenderer tr = client.textRenderer;
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        if (OBFUtilities.config.debugShowFps) {
            int fps = client.getCurrentFps();
            int col = fps >= 60 ? 0xFF66BB6A : fps >= 30 ? 0xFFFFD54F : 0xFFFF5252;
            String text = fps + " FPS";
            int w = tr.getWidth(text) + PAD_X * 2 + 6;
            int x = clamp(OBFUtilities.config.debugFpsX, 0, sw - w);
            int y = clamp(OBFUtilities.config.debugFpsY, 0, sh - H);
            OBFUtilities.config.debugFpsX = x;
            OBFUtilities.config.debugFpsY = y;
            drawPill(context, x, y, w, H, ACCENT_FPS);
            context.drawText(tr, text, x + PAD_X + 6, y + PAD_Y, col, false);
        }

        if (OBFUtilities.config.debugShowCoords) {
            PlayerEntity player = client.player;
            int px = (int) player.getX(), py = (int) player.getY(), pz = (int) player.getZ();
            float yaw = ((player.getYaw() % 360) + 360) % 360;
            String dir = yaw < 45 || yaw >= 315 ? "S" : yaw < 135 ? "O" : yaw < 225 ? "N" : "E";
            String text = "X " + px + "  Y " + py + "  Z " + pz + "  §8" + dir;
            String textPlain = "X " + px + "  Y " + py + "  Z " + pz + "  " + dir;
            int w = tr.getWidth(textPlain) + PAD_X * 2 + 6;
            int x = clamp(OBFUtilities.config.debugCoordsX, 0, sw - w);
            int y = clamp(OBFUtilities.config.debugCoordsY, 0, sh - H);
            OBFUtilities.config.debugCoordsX = x;
            OBFUtilities.config.debugCoordsY = y;
            drawPill(context, x, y, w, H, ACCENT_COORDS);
            context.drawText(tr, text, x + PAD_X + 6, y + PAD_Y, 0xFF80CBC4, false);
        }

        if (OBFUtilities.config.debugShowRam) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long max  = rt.maxMemory() / 1024 / 1024;
            int pct   = (int) (used * 100 / max);
            int col   = pct < 60 ? 0xFF66BB6A : pct < 80 ? 0xFFFFD54F : 0xFFFF5252;
            String text = used + " / " + max + " Mo";
            int w = tr.getWidth(text) + PAD_X * 2 + 6 + 40;
            int x = clamp(OBFUtilities.config.debugRamX, 0, sw - w);
            int y = clamp(OBFUtilities.config.debugRamY, 0, sh - H);
            OBFUtilities.config.debugRamX = x;
            OBFUtilities.config.debugRamY = y;
            drawPill(context, x, y, w, H, ACCENT_RAM);
            context.drawText(tr, text, x + PAD_X + 6, y + PAD_Y, 0xFFCE93D8, false);
            int barX = x + PAD_X + 6 + tr.getWidth(text) + 4;
            int barW = 32;
            int barY = y + PAD_Y + 1;
            int barH = 6;
            context.fill(barX, barY, barX + barW, barY + barH, 0x44FFFFFF);
            int filled = (int) (barW * pct / 100f);
            context.fill(barX, barY, barX + filled, barY + barH, col & 0xAAFFFFFF | (col & 0x00FFFFFF));
            context.fill(barX, barY, barX + barW, barY + 1, 0x33FFFFFF);
            context.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0x33FFFFFF);
        }
    }

    private static void drawPill(DrawContext ctx, int x, int y, int w, int h, int accent) {
        ctx.fill(x, y, x + w, y + h, BG_BASE);
        ctx.fill(x, y, x + 3, y + h, accent);
        int borderSubtle = inConfigScreen ? (accent & 0x00FFFFFF | 0x88000000) : 0x22FFFFFF;
        ctx.fill(x + 3, y,         x + w, y + 1,     borderSubtle);
        ctx.fill(x + 3, y + h - 1, x + w, y + h,     borderSubtle);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, borderSubtle);
        if (inConfigScreen) {
            ctx.fill(x, y,         x + w, y + 1,         0x55FFD54F);
            ctx.fill(x, y + h - 1, x + w, y + h,         0x55FFD54F);
            ctx.fill(x + 3, y + 1, x + 4, y + h - 1,     0x22FFD54F);
            ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, 0x55FFD54F);
        }
    }

    public static void tick(MinecraftClient client) {
        if (client.currentScreen != null) return;
        if (client.player == null) return;
        if (!inConfigScreen) {
            draggingFps = false;
            draggingCoords = false;
            draggingRam = false;
            return;
        }
        if (client.player == null) return;

        long window = client.getWindow().getHandle();
        double scale = client.getWindow().getScaleFactor();
        double[] mx = new double[1], my = new double[1];
        GLFW.glfwGetCursorPos(window, mx, my);
        int mouseX = (int) (mx[0] / scale), mouseY = (int) (my[0] / scale);
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        boolean justPressed = down && !wasMouseDown;
        wasMouseDown = down;

        TextRenderer tr = client.textRenderer;

        if (OBFUtilities.config.debugShowFps) {
            int fps = client.getCurrentFps();
            String text = fps + " FPS";
            int w = tr.getWidth(text) + PAD_X * 2 + 6;
            int x = OBFUtilities.config.debugFpsX, y = OBFUtilities.config.debugFpsY;
            if (justPressed && !draggingFps && !draggingCoords && !draggingRam
                    && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + H) {
                draggingFps = true; dragFpsOffX = mouseX - x; dragFpsOffY = mouseY - y;
            }
            if (draggingFps) {
                if (down) { OBFUtilities.config.debugFpsX = mouseX - dragFpsOffX; OBFUtilities.config.debugFpsY = mouseY - dragFpsOffY; }
                else { draggingFps = false; OBFUtilities.config.save(); }
            }
        }

        if (OBFUtilities.config.debugShowCoords) {
            if (client.player != null) {
                int px = (int) client.player.getX(), py = (int) client.player.getY(), pz = (int) client.player.getZ();
                String textPlain = "X " + px + "  Y " + py + "  Z " + pz + "  S";
                int w = tr.getWidth(textPlain) + PAD_X * 2 + 6;
                int x = OBFUtilities.config.debugCoordsX, y = OBFUtilities.config.debugCoordsY;
                if (justPressed && !draggingFps && !draggingCoords && !draggingRam
                        && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + H) {
                    draggingCoords = true; dragCoordsOffX = mouseX - x; dragCoordsOffY = mouseY - y;
                }
                if (draggingCoords) {
                    if (down) { OBFUtilities.config.debugCoordsX = mouseX - dragCoordsOffX; OBFUtilities.config.debugCoordsY = mouseY - dragCoordsOffY; }
                    else { draggingCoords = false; OBFUtilities.config.save(); }
                }
            }
        }

        if (OBFUtilities.config.debugShowRam) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long max  = rt.maxMemory() / 1024 / 1024;
            String text = used + " / " + max + " Mo";
            int w = tr.getWidth(text) + PAD_X * 2 + 6 + 40;
            int x = OBFUtilities.config.debugRamX, y = OBFUtilities.config.debugRamY;
            if (justPressed && !draggingFps && !draggingCoords && !draggingRam
                    && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + H) {
                draggingRam = true; dragRamOffX = mouseX - x; dragRamOffY = mouseY - y;
            }
            if (draggingRam) {
                if (down) { OBFUtilities.config.debugRamX = mouseX - dragRamOffX; OBFUtilities.config.debugRamY = mouseY - dragRamOffY; }
                else { draggingRam = false; OBFUtilities.config.save(); }
            }
        }
    }

    public static int[] getFpsBounds(TextRenderer tr, MinecraftClient client) {
        int fps = client.getCurrentFps();
        String text = fps + " FPS";
        int w = tr.getWidth(text) + PAD_X * 2 + 6;
        return new int[]{ OBFUtilities.config.debugFpsX, OBFUtilities.config.debugFpsY, w, H };
    }

    public static int[] getCoordsBounds(TextRenderer tr, MinecraftClient client) {
        if (client.player == null) return new int[]{0, 0, 0, 0};
        int px = (int) client.player.getX(), py = (int) client.player.getY(), pz = (int) client.player.getZ();
        String textPlain = "X " + px + "  Y " + py + "  Z " + pz + "  S";
        int w = tr.getWidth(textPlain) + PAD_X * 2 + 6;
        return new int[]{ OBFUtilities.config.debugCoordsX, OBFUtilities.config.debugCoordsY, w, H };
    }

    public static int[] getRamBounds(TextRenderer tr) {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long max  = rt.maxMemory() / 1024 / 1024;
        String text = used + " / " + max + " Mo";
        int w = tr.getWidth(text) + PAD_X * 2 + 6 + 40;
        return new int[]{ OBFUtilities.config.debugRamX, OBFUtilities.config.debugRamY, w, H };
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(v, max)); }
}