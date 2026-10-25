package com.hamtabot.obfutilities.debug;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.HudPos;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

public class DebugOverlay {

    public enum Element { FPS, COORDS, RAM }

    private static final int PAD = 6, BAR_GAP = 7, H = 16, GAUGE_W = 32;

    private record Seg(String text, int color) {}

    private record Pill(List<Seg> segs, int accent, int gaugePct, int gaugeColor) {
        int textWidth(TextRenderer tr) {
            int w = 0;
            for (Seg s : segs) w += tr.getWidth(s.text());
            return w;
        }

        int width(TextRenderer tr) { return BAR_GAP + textWidth(tr) + (gaugePct >= 0 ? 5 + GAUGE_W : 0) + PAD; }
    }

    public static void render(DrawContext ctx, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        for (Element e : Element.values()) {
            if (isEnabled(e)) renderPill(ctx, client, e, editing);
        }
    }

    private static void renderPill(DrawContext ctx, MinecraftClient client, Element e, boolean editing) {
        TextRenderer tr = client.textRenderer;
        Pill pill = pill(client, e);
        int[] b = bounds(e, pill.width(tr));
        int x = b[0], y = b[1], w = b[2];

        Draw.overlayCard(ctx, x, y, w, H, pill.accent(), editing);

        int tx = x + BAR_GAP, ty = y + (H - 8) / 2 + 1;
        for (Seg s : pill.segs()) {
            ctx.drawText(tr, s.text(), tx, ty, s.color(), false);
            tx += tr.getWidth(s.text());
        }

        if (pill.gaugePct() >= 0) {
            int gx = tx + 5, gy = y + (H - 4) / 2;
            Draw.round(ctx, gx, gy, GAUGE_W, 4, Theme.SWITCH_OFF);
            Draw.round(ctx, gx, gy, Math.max(2, GAUGE_W * pill.gaugePct() / 100), 4, pill.gaugeColor());
        }
    }

    private static Pill pill(MinecraftClient client, Element e) {
        return switch (e) { case FPS -> fpsPill(client); case COORDS -> coordsPill(client.player); case RAM -> ramPill(); };
    }

    private static int status(boolean good, boolean ok) { return good ? Theme.SUCCESS : ok ? Theme.WARNING : Theme.DANGER; }

    private static Pill fpsPill(MinecraftClient client) {
        int fps = client.getCurrentFps();
        int col = status(fps >= 60, fps >= 30);
        return new Pill(List.of(new Seg(String.valueOf(fps), col), new Seg(" FPS", Theme.TEXT_MUTED)), col, -1, 0);
    }

    private static Pill coordsPill(PlayerEntity p) {
        float yaw = ((p.getYaw() % 360) + 360) % 360;
        String dir = yaw < 45 || yaw >= 315 ? "S" : yaw < 135 ? "O" : yaw < 225 ? "N" : "E";
        return new Pill(List.of(
                new Seg("X ", Theme.TEXT_MUTED), new Seg(String.valueOf((int) p.getX()), Theme.TEXT),
                new Seg("  Y ", Theme.TEXT_MUTED), new Seg(String.valueOf((int) p.getY()), Theme.TEXT),
                new Seg("  Z ", Theme.TEXT_MUTED), new Seg(String.valueOf((int) p.getZ()), Theme.TEXT),
                new Seg("  " + dir, Theme.ACCENT)), Theme.ACCENT, -1, 0);
    }

    private static Pill ramPill() {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long max  = rt.maxMemory() / 1024 / 1024;
        int pct   = (int) (used * 100 / max);
        return new Pill(List.of(new Seg(String.valueOf(used), Theme.TEXT), new Seg(" / " + max + " Mo", Theme.TEXT_MUTED)),
                Theme.PURPLE, pct, status(pct < 60, pct < 80));
    }

    public static int[] bounds(Element e, TextRenderer tr) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return new int[]{0, 0, 0, 0};
        return bounds(e, pill(client, e).width(tr));
    }

    private static int[] bounds(Element e, int w) {
        int[] p = pos(e).resolve(w, H, legacyX(e), legacyY(e));
        return new int[]{ p[0], p[1], w, H };
    }

    public static boolean isEnabled(Element e) {
        ModConfig c = OBFUtilities.config;
        return switch (e) { case FPS -> c.debugShowFps; case COORDS -> c.debugShowCoords; case RAM -> c.debugShowRam; };
    }

    private static HudPos pos(Element e) {
        ModConfig c = OBFUtilities.config;
        return switch (e) { case FPS -> c.debugFpsPos; case COORDS -> c.debugCoordsPos; case RAM -> c.debugRamPos; };
    }

    private static int legacyX(Element e) {
        ModConfig c = OBFUtilities.config;
        return switch (e) { case FPS -> c.debugFpsX; case COORDS -> c.debugCoordsX; case RAM -> c.debugRamX; };
    }

    private static int legacyY(Element e) {
        ModConfig c = OBFUtilities.config;
        return switch (e) { case FPS -> c.debugFpsY; case COORDS -> c.debugCoordsY; case RAM -> c.debugRamY; };
    }

    public static void setPos(Element e, int x, int y) {
        int[] b = bounds(e, tr());
        pos(e).set(x, y, b[2], b[3]);
    }

    private static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }

}
