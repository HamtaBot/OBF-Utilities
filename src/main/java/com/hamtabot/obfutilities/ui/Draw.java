package com.hamtabot.obfutilities.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.List;

public final class Draw {

    private Draw() {}

    public enum ButtonStyle { PRIMARY, SECONDARY, DANGER, GHOST }

    public static void round(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) { ctx.fill(x, y, x + w, y + h, color); return; }
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void card(DrawContext ctx, int x, int y, int w, int h, int fill, int border) {
        round(ctx, x, y, w, h, border);
        round(ctx, x + 1, y + 1, w - 2, h - 2, fill);
    }

    public static void shadow(DrawContext ctx, int x, int y, int w, int h) {
        for (int i = 1; i <= 4; i++) {
            int alpha = 0x18 - i * 4;
            round(ctx, x - i, y - i + 2, w + i * 2, h + i * 2, alpha << 24);
        }
    }

    private static final int ACCENT_BG = 0xFF1F7AA8;

    public static void button(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                              String label, ButtonStyle style, boolean hovered, boolean enabled) {
        int bg, border, text;
        switch (style) {
            case PRIMARY -> { bg = hovered ? 0xFF2B8DBF : ACCENT_BG; border = 0xFF3AA7DD; text = 0xFFFFFFFF; }
            case DANGER  -> { bg = hovered ? 0xFF9B2C2C : Theme.DANGER_DARK; border = 0xFFB93838; text = 0xFFFFE4E4; }
            case GHOST   -> { bg = hovered ? Theme.SURFACE_HOVER : 0x00000000; border = hovered ? Theme.BORDER : 0x00000000; text = hovered ? Theme.TEXT : Theme.TEXT_MUTED; }
            default      -> { bg = hovered ? Theme.SURFACE_ACTIVE : Theme.SURFACE_HOVER; border = hovered ? 0xFF3C4250 : Theme.BORDER; text = Theme.TEXT; }
        }
        if (!enabled) { bg = Theme.SURFACE; border = Theme.DIVIDER; text = Theme.TEXT_DIM; }
        card(ctx, x, y, w, h, bg, border);
        String shown = ellipsize(tr, label, w - 6);
        ctx.drawText(tr, shown, x + (w - tr.getWidth(shown)) / 2, y + (h - 8) / 2 + 1, text, false);
    }

    public static void closeButton(DrawContext ctx, int x, int y, int size, boolean hovered) {
        card(ctx, x, y, size, size, hovered ? Theme.DANGER_STRONG : Theme.DANGER_SURFACE,
                hovered ? Theme.DANGER : Theme.DANGER_LINE);
        cross(ctx, x + size / 2f, y + size / 2f, size * 0.42f, hovered ? 0xFFFFFFFF : Theme.DANGER);
    }

    private static void cross(DrawContext ctx, float cx, float cy, float extent, int color) {
        int half = Math.round(extent * 0.7071f * 4);
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy, 0);
        m.scale(0.25f, 0.25f, 1f);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45));
        ctx.fill(-half, -3, half, 3, color);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90));
        ctx.fill(-half, -3, half, 3, color);
        m.pop();
    }

    private static final Identifier LOGO = new Identifier("obfutilities", "textures/gui/logo.png");
    private static final int LOGO_RES = 64;

    private static void logo(DrawContext ctx, int x, int y, int size) {
        RenderSystem.enableBlend();
        ctx.drawTexture(LOGO, x, y, size, size, 0, 0, LOGO_RES, LOGO_RES, LOGO_RES, LOGO_RES);
        RenderSystem.disableBlend();
    }

    public static int brand(DrawContext ctx, TextRenderer tr, int x, int y, int h, int logoSize, int gap) {
        logo(ctx, x, y + (h - logoSize) / 2 + 1, logoSize);
        int tx = x + logoSize + gap, ty = y + (h - 8) / 2 + 1;
        ctx.drawText(tr, "OBF", tx, ty, Theme.ACCENT, true);
        ctx.drawText(tr, " Utilities", tx + tr.getWidth("OBF"), ty, Theme.TEXT, true);
        return tx + tr.getWidth("OBF Utilities");
    }

    public static void window(DrawContext ctx, int screenW, int screenH, int x, int y, int w, int h, int headerH) {
        ctx.fill(0, 0, screenW, screenH, Theme.OVERLAY);
        shadow(ctx, x, y, w, h);
        card(ctx, x, y, w, h, Theme.WINDOW, Theme.WINDOW_BORDER);
        round(ctx, x + 1, y + 1, w - 2, headerH, Theme.HEADER);
        accentLine(ctx, x + 1, y + headerH, w - 2);
    }

    public static void inputFrame(DrawContext ctx, int x, int y, int w, int h, boolean focused) {
        card(ctx, x, y, w, h, Theme.INPUT, focused ? Theme.ACCENT_DARK : Theme.BORDER);
    }

    public static void segments(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                                String[] labels, int selected, int mouseX, int mouseY) {
        card(ctx, x, y, w, h, Theme.INPUT, Theme.BORDER);
        int n = labels.length;
        for (int i = 0; i < n; i++) {
            int sx = x + w * i / n + 2, sw = w * (i + 1) / n - w * i / n - 4;
            boolean sel = i == selected;
            if (sel) round(ctx, sx, y + 2, sw, h - 4, Theme.ACCENT_DARK);
            else if (inside(mouseX, mouseY, sx, y, sw, h)) round(ctx, sx, y + 2, sw, h - 4, Theme.SURFACE_HOVER);
            String l = ellipsize(tr, labels[i], sw - 4);
            ctx.drawText(tr, l, sx + (sw - tr.getWidth(l)) / 2, y + (h - 8) / 2, sel ? Theme.TEXT : Theme.TEXT_MUTED, false);
        }
    }

    public static int segmentAt(double mouseX, double mouseY, int x, int y, int w, int h, int count) {
        if (!inside(mouseX, mouseY, x, y, w, h)) return -1;
        return Math.min(count - 1, (int) ((mouseX - x) * count / w));
    }

    public static final int RESIZE_GRIP = 8;

    public static void resizeGrip(DrawContext ctx, int right, int bottom) {
        int gx = right - RESIZE_GRIP, gy = bottom - RESIZE_GRIP;
        ctx.fill(gx + 2, gy + 6, gx + 4, gy + 8, Theme.ACCENT);
        ctx.fill(gx + 4, gy + 4, gx + 6, gy + 6, Theme.ACCENT);
        ctx.fill(gx + 6, gy + 2, gx + 8, gy + 4, Theme.ACCENT);
    }

    public static boolean inResizeGrip(double mx, double my, int right, int bottom) {
        return inside(mx, my, right - RESIZE_GRIP, bottom - RESIZE_GRIP, RESIZE_GRIP, RESIZE_GRIP);
    }

    public static void title(String title, int color, String subtitle) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return;
        client.inGameHud.setTitle(Text.literal(title).styled(s -> s.withColor(color & 0xFFFFFF)));
        client.inGameHud.setSubtitle(Text.literal(subtitle).styled(s -> s.withColor(Theme.TEXT_MUTED & 0xFFFFFF)));
        client.inGameHud.setTitleTicks(10, 70, 20);
    }

    public static void overlayCard(DrawContext ctx, int x, int y, int w, int h, int accent, boolean editing) {
        card(ctx, x, y, w, h, Theme.WINDOW, editing ? Theme.ACCENT : Theme.WINDOW_BORDER);
        ctx.fill(x + 1, y + 3, x + 3, y + h - 3, accent);
    }

    public static void headerTitle(DrawContext ctx, TextRenderer tr, int x, int y, int headerH, String text, int barColor) {
        int ty = y + (headerH - 8) / 2 + 1;
        round(ctx, x, ty - 1, 3, 10, barColor);
        ctx.drawText(tr, text, x + 8, ty, Theme.TEXT, true);
    }

    private static final int[] ACCENT_GRADIENT = new int[24];
    static {
        for (int i = 0; i < ACCENT_GRADIENT.length; i++) {
            ACCENT_GRADIENT[i] = lerpColor(Theme.ACCENT, Theme.ACCENT_LINE, i / (float) ACCENT_GRADIENT.length);
        }
    }

    public static void accentLine(DrawContext ctx, int x, int y, int w) {
        int steps = Math.max(1, Math.min(w, ACCENT_GRADIENT.length));
        for (int i = 0; i < steps; i++) {
            int x0 = x + w * i / steps, x1 = x + w * (i + 1) / steps;
            ctx.fill(x0, y, x1, y + 1, ACCENT_GRADIENT[i * ACCENT_GRADIENT.length / steps]);
        }
    }

    public static void playClick() {
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    public static final int SWITCH_W = 22, SWITCH_H = 12;

    public static void toggle(DrawContext ctx, int x, int y, float progress, boolean hovered) {
        int track = lerpColor(Theme.SWITCH_OFF, Theme.ACCENT_DARK, progress);
        if (hovered) track = lerpColor(track, 0xFFFFFFFF, 0.08f);
        round(ctx, x, y, SWITCH_W, SWITCH_H, track);
        int knobX = x + 2 + Math.round((SWITCH_W - 12) * progress);
        int knob = lerpColor(0xFFB8BEC8, 0xFFFFFFFF, progress);
        round(ctx, knobX, y + 2, 8, 8, knob);
    }

    public static String ellipsize(TextRenderer tr, String s, int maxW) {
        if (maxW <= 0) return "";
        if (tr.getWidth(s) <= maxW) return s;
        return tr.trimToWidth(s, Math.max(0, maxW - tr.getWidth("…"))) + "…";
    }

    public static List<OrderedText> wrap(TextRenderer tr, String s, int maxW) {
        return tr.wrapLines(Text.literal(s), Math.max(10, maxW));
    }

    public static int drawWrapped(DrawContext ctx, TextRenderer tr, List<OrderedText> lines, int x, int y, int color) {
        for (OrderedText line : lines) {
            ctx.drawText(tr, line, x, y, color, false);
            y += Theme.LINE_H;
        }
        return y;
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = b >>> 24, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
