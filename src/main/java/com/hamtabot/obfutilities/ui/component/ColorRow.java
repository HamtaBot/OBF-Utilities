package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Anim;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** A colour setting: a swatch that unfolds into hue, saturation and brightness sliders and a row of presets. */
public class ColorRow extends Component {

    private static final int PAD = Theme.ROW_PAD, HEAD_H = 24, LINE = 14, TRACK_H = 6, KNOB = 8, LABEL_W = 62;
    private static final int SWATCH_W = 22, SWATCH_H = 12, PRESET = 12, PRESET_GAP = 3, RESET = 12;
    private static final String[] LABELS = { "Teinte", "Saturation", "Luminosité" };
    private static final int[] PRESETS = {
            0xFFFFFF, 0xE8EAED, 0x9AA2B0, 0x141821, 0x000000, 0xF87171, 0xFB923C, 0xFBBF24,
            0x4ADE80, 0x2DD4BF, 0x4FC3F7, 0x818CF8, 0xC084FC, 0xF472B6
    };
    private static final int[] HUES = new int[7];
    static {
        for (int i = 0; i < HUES.length; i++) HUES[i] = 0xFF000000 | MathHelper.hsvToRgb(i == 6 ? 0f : i / 6f, 1f, 1f);
    }

    private final String title, description;
    private final int def;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final Anim hover = new Anim(0f);
    /** Kept apart from the colour so the hue survives a trip through grey or black. */
    private final float[] hsv = new float[3];
    private int hsvFor = -1;
    private boolean open;
    private int dragging = -1;

    public ColorRow(String title, String description, int def, IntSupplier getter, IntConsumer setter) {
        this.title = title;
        this.description = description;
        this.def = def & 0xFFFFFF;
        this.getter = getter;
        this.setter = setter;
    }

    private int value() { return getter.getAsInt() & 0xFFFFFF; }

    private void syncHsv() {
        int rgb = value();
        if (rgb == hsvFor) return;
        hsvFor = rgb;
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        hsv[2] = max;
        hsv[1] = max == 0f ? 0f : d / max;
        if (d == 0f) return;
        float h = max == r ? (g - b) / d + (g < b ? 6f : 0f) : max == g ? (b - r) / d + 2f : (r - g) / d + 4f;
        hsv[0] = h / 6f;
    }

    private void store() {
        // hue 1 wraps to 0 in the conversion; the slider keeps its own value so the knob stays put
        hsvFor = MathHelper.hsvToRgb(Math.min(hsv[0], 0.9999f), hsv[1], hsv[2]) & 0xFFFFFF;
        setter.accept(hsvFor);
    }

    @Override public int getHeight() { return open ? HEAD_H + LINE * 3 + PRESET + PAD + 4 : HEAD_H; }

    private int trackX() { return x + PAD + LABEL_W; }
    private int trackW() { return width - PAD * 2 - LABEL_W; }
    private int lineY(int i) { return y + HEAD_H + i * LINE; }
    private int presetY() { return y + HEAD_H + LINE * 3 + 3; }
    private int presetCount() { return Math.min(PRESETS.length, (width - PAD * 2 + PRESET_GAP) / (PRESET + PRESET_GAP)); }
    private int swatchX() { return x + width - PAD - SWATCH_W; }
    private int resetX() { return swatchX() - 6 - RESET; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        syncHsv();
        int h = getHeight(), rgb = value();
        boolean overHead = Draw.inside(mouseX, mouseY, x, y, width, HEAD_H);
        float hv = hover.update(overHead ? 1f : 0f, 18f);
        Draw.card(ctx, x, y, width, h, Draw.lerpColor(Theme.SURFACE, Theme.SURFACE_HOVER, hv), Theme.BORDER);

        int ty = y + (HEAD_H - 8) / 2;
        boolean changed = rgb != def;
        String hex = String.format("#%06X", rgb);
        int hexX = (changed ? resetX() : swatchX()) - 6 - tr().getWidth(hex);
        int titleW = tr().getWidth(title);
        ctx.drawText(tr(), title, x + PAD, ty, Theme.TEXT, false);
        String desc = Draw.ellipsize(tr(), description, hexX - 8 - (x + PAD + titleW + 6));
        if (desc.length() > 1) ctx.drawText(tr(), desc, x + PAD + titleW + 6, ty, Theme.TEXT_MUTED, false);
        ctx.drawText(tr(), hex, hexX, ty, Theme.TEXT_MUTED, false);
        if (changed) {
            boolean hov = Draw.inside(mouseX, mouseY, resetX(), y + (HEAD_H - RESET) / 2, RESET, RESET);
            Draw.restartIcon(ctx, resetX() + RESET / 2f, y + HEAD_H / 2f, hov ? Theme.TEXT : Theme.TEXT_DIM);
        }
        Draw.card(ctx, swatchX(), y + (HEAD_H - SWATCH_H) / 2, SWATCH_W, SWATCH_H, 0xFF000000 | rgb, Theme.BORDER);
        if (!open) return;

        int tx = trackX(), tw = trackW();
        int full = 0xFF000000 | MathHelper.hsvToRgb(Math.min(hsv[0], 0.9999f), 1f, 1f);
        int vivid = 0xFF000000 | MathHelper.hsvToRgb(Math.min(hsv[0], 0.9999f), hsv[1], 1f);
        for (int i = 0; i < 3; i++) {
            int ly = lineY(i), cy = ly + (LINE - TRACK_H) / 2;
            ctx.drawText(tr(), LABELS[i], x + PAD, ly + (LINE - 8) / 2, Theme.TEXT_MUTED, false);
            int[] stops = i == 0 ? HUES : i == 1 ? new int[]{ 0xFFFFFFFF, full } : new int[]{ 0xFF000000, vivid };
            Draw.gradient(ctx, tx, cy, tw, TRACK_H, stops);
            int kx = tx + Math.round((tw - KNOB) * hsv[i]);
            Draw.card(ctx, kx, ly + (LINE - KNOB) / 2, KNOB, KNOB, 0xFFFFFFFF, 0x66000000);
        }
        int py = presetY();
        for (int i = 0; i < presetCount(); i++) {
            int px = x + PAD + i * (PRESET + PRESET_GAP);
            boolean hov = Draw.inside(mouseX, mouseY, px, py, PRESET, PRESET);
            Draw.card(ctx, px, py, PRESET, PRESET, 0xFF000000 | PRESETS[i], PRESETS[i] == rgb || hov ? Theme.TEXT : Theme.BORDER);
        }
    }

    private void drag(double mouseX) {
        float p = (float) ((mouseX - trackX() - KNOB / 2.0) / (trackW() - KNOB));
        hsv[dragging] = Math.max(0f, Math.min(1f, p));
        store();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;
        if (mouseY < y + HEAD_H) {
            if (value() != def && Draw.inside(mouseX, mouseY, resetX(), y, RESET, HEAD_H)) setter.accept(def);
            else open = !open;
            Draw.playClick();
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (mouseY >= lineY(i) && mouseY < lineY(i) + LINE && mouseX >= trackX() - KNOB) {
                syncHsv();
                dragging = i;
                drag(mouseX);
                return true;
            }
        }
        for (int i = 0; i < presetCount(); i++) {
            if (Draw.inside(mouseX, mouseY, x + PAD + i * (PRESET + PRESET_GAP), presetY(), PRESET, PRESET)) {
                setter.accept(PRESETS[i]);
                Draw.playClick();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY) {
        if (dragging < 0) return false;
        drag(mouseX);
        return true;
    }

    @Override
    public void mouseReleased() { dragging = -1; }
}
