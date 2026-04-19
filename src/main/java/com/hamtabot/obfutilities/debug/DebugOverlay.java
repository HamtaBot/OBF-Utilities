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

    public static boolean cfgDraggingFps    = false;
    public static boolean cfgDraggingCoords = false;
    public static boolean cfgDraggingRam    = false;
    public static int cfgDragFpsOffX, cfgDragFpsOffY;
    public static int cfgDragCoordsOffX, cfgDragCoordsOffY;
    public static int cfgDragRamOffX, cfgDragRamOffY;

    private static final int BG     = 0xDD111111;
    private static final int BORDER = 0xFF4FC3F7;
    private static final int PAD    = 5;


    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        TextRenderer tr = client.textRenderer;
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        if (OBFUtilities.config.debugShowFps) {
            int fps = client.getCurrentFps();
            String text = "FPS: " + fps;
            int w = tr.getWidth(text) + PAD*2, h = 14;
            int x = clamp(OBFUtilities.config.debugFpsX, 0, sw-w);
            int y = clamp(OBFUtilities.config.debugFpsY, 0, sh-h);
            OBFUtilities.config.debugFpsX = x; OBFUtilities.config.debugFpsY = y;
            drawBox(context, x, y, w, h);
            int col = fps >= 60 ? 0xFF66BB6A : fps >= 30 ? 0xFFFFD54F : 0xFFFF5252;
            context.drawText(tr, text, x+PAD, y+3, col, false);
        }

        if (OBFUtilities.config.debugShowCoords) {
            PlayerEntity player = client.player;
            int px=(int)player.getX(), py=(int)player.getY(), pz=(int)player.getZ();
            float yaw = ((player.getYaw() % 360) + 360) % 360;
            String dir = yaw<45||yaw>=315?"S": yaw<135?"O": yaw<225?"N":"E";
            String text = "X:"+px+" Y:"+py+" Z:"+pz+" ("+dir+")";
            int w = tr.getWidth(text) + PAD*2, h = 14;
            int x = clamp(OBFUtilities.config.debugCoordsX, 0, sw-w);
            int y = clamp(OBFUtilities.config.debugCoordsY, 0, sh-h);
            OBFUtilities.config.debugCoordsX = x; OBFUtilities.config.debugCoordsY = y;
            drawBox(context, x, y, w, h);
            context.drawText(tr, text, x+PAD, y+3, 0xFF80CBC4, false);
        }

        if (OBFUtilities.config.debugShowRam) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long max  = rt.maxMemory() / 1024 / 1024;
            int pct   = (int)(used * 100 / max);
            String text = "RAM: " + used + "Mo / " + max + "Mo (" + pct + "%)";
            int w = tr.getWidth(text) + PAD*2, h = 14;
            int x = clamp(OBFUtilities.config.debugRamX, 0, sw-w);
            int y = clamp(OBFUtilities.config.debugRamY, 0, sh-h);
            OBFUtilities.config.debugRamX = x; OBFUtilities.config.debugRamY = y;
            drawBox(context, x, y, w, h);
            int col = pct < 60 ? 0xFF66BB6A : pct < 80 ? 0xFFFFD54F : 0xFFFF5252;
            context.drawText(tr, text, x+PAD, y+3, col, false);
        }
    }

    private static void drawBox(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x+w, y+h, BG);
        int border = inConfigScreen ? 0xFFFFD54F : BORDER;
        ctx.fill(x, y, x+w, y+1, border);
        ctx.fill(x, y+h-1, x+w, y+h, border);
        ctx.fill(x, y, x+1, y+h, border);
        ctx.fill(x+w-1, y, x+w, y+h, border);
        if (inConfigScreen) {
            // Fond légèrement plus visible
            ctx.fill(x+1, y+1, x+w-1, y+h-1, 0xEE1A1A1A);
        }
    }


    public static void tick(MinecraftClient client) {
        if (client.currentScreen != null) return;
        if (client.player == null) return;

        long window = client.getWindow().getHandle();
        double scale = client.getWindow().getScaleFactor();
        double[] mx = new double[1], my = new double[1];
        GLFW.glfwGetCursorPos(window, mx, my);
        int mouseX = (int)(mx[0] / scale), mouseY = (int)(my[0] / scale);
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        TextRenderer tr = client.textRenderer;

        // FPS
        if (OBFUtilities.config.debugShowFps) {
            int fps = client.getCurrentFps();
            int w = tr.getWidth("FPS: " + fps) + PAD*2, h = 14;
            int x = OBFUtilities.config.debugFpsX, y = OBFUtilities.config.debugFpsY;
            if (down && !draggingFps && !draggingCoords && !draggingRam
                    && mouseX>=x && mouseX<=x+w && mouseY>=y && mouseY<=y+h) {
                draggingFps = true; dragFpsOffX = mouseX-x; dragFpsOffY = mouseY-y;
            }
            if (draggingFps) {
                if (down) { OBFUtilities.config.debugFpsX = mouseX-dragFpsOffX; OBFUtilities.config.debugFpsY = mouseY-dragFpsOffY; }
                else { draggingFps = false; OBFUtilities.config.save(); }
            }
        }

        // Coords
        if (OBFUtilities.config.debugShowCoords) {
            int px=(int)client.player.getX(), py=(int)client.player.getY(), pz=(int)client.player.getZ();
            int w = tr.getWidth("X:"+px+" Y:"+py+" Z:"+pz+" (S)") + PAD*2, h = 14;
            int x = OBFUtilities.config.debugCoordsX, y = OBFUtilities.config.debugCoordsY;
            if (down && !draggingFps && !draggingCoords && !draggingRam
                    && mouseX>=x && mouseX<=x+w && mouseY>=y && mouseY<=y+h) {
                draggingCoords = true; dragCoordsOffX = mouseX-x; dragCoordsOffY = mouseY-y;
            }
            if (draggingCoords) {
                if (down) { OBFUtilities.config.debugCoordsX = mouseX-dragCoordsOffX; OBFUtilities.config.debugCoordsY = mouseY-dragCoordsOffY; }
                else { draggingCoords = false; OBFUtilities.config.save(); }
            }
        }

        // RAM c'était pour bench le mod mais c'est stylé donc je laisse
        if (OBFUtilities.config.debugShowRam) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory()-rt.freeMemory())/1024/1024;
            long max  = rt.maxMemory()/1024/1024;
            int w = tr.getWidth("RAM: "+used+"Mo / "+max+"Mo (100%)") + PAD*2, h = 14;
            int x = OBFUtilities.config.debugRamX, y = OBFUtilities.config.debugRamY;
            if (down && !draggingFps && !draggingCoords && !draggingRam
                    && mouseX>=x && mouseX<=x+w && mouseY>=y && mouseY<=y+h) {
                draggingRam = true; dragRamOffX = mouseX-x; dragRamOffY = mouseY-y;
            }
            if (draggingRam) {
                if (down) { OBFUtilities.config.debugRamX = mouseX-dragRamOffX; OBFUtilities.config.debugRamY = mouseY-dragRamOffY; }
                else { draggingRam = false; OBFUtilities.config.save(); }
            }
        }
    }


    public static int[] getFpsBounds(TextRenderer tr, MinecraftClient client) {
        int fps = client.getCurrentFps();
        int w = tr.getWidth("FPS: "+fps)+PAD*2;
        return new int[]{ OBFUtilities.config.debugFpsX, OBFUtilities.config.debugFpsY, w, 14 };
    }
    public static int[] getCoordsBounds(TextRenderer tr, MinecraftClient client) {
        if (client.player == null) return new int[]{0,0,0,0};
        int px=(int)client.player.getX(), py=(int)client.player.getY(), pz=(int)client.player.getZ();
        int w = tr.getWidth("X:"+px+" Y:"+py+" Z:"+pz+" (S)")+PAD*2;
        return new int[]{ OBFUtilities.config.debugCoordsX, OBFUtilities.config.debugCoordsY, w, 14 };
    }
    public static int[] getRamBounds(TextRenderer tr) {
        Runtime rt = Runtime.getRuntime();
        long used=(rt.totalMemory()-rt.freeMemory())/1024/1024, max=rt.maxMemory()/1024/1024;
        int w = tr.getWidth("RAM: "+used+"Mo / "+max+"Mo (100%)")+PAD*2;
        return new int[]{ OBFUtilities.config.debugRamX, OBFUtilities.config.debugRamY, w, 14 };
    }

    public static boolean inConfigScreen = false;

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(v, max)); }
}