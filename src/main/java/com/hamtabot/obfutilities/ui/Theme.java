package com.hamtabot.obfutilities.ui;

import com.hamtabot.obfutilities.config.ModConfig;

/**
 * The colours of the whole UI. They are plain fields rather than constants: {@link #apply} recomputes them
 * from the appearance settings, so every screen and HUD follows the chosen glass and text colours.
 */
public final class Theme {

    private Theme() {}

    public static final int DEF_GLASS   = 0x141821;
    public static final int DEF_TEXT    = 0xE8EAED;
    public static final int DEF_MUTED   = 0x9AA2B0;
    public static final int DEF_DIM     = 0x737B8A;
    public static final int DEF_ACCENT  = 0x4FC3F7;
    public static final int DEF_PURPLE  = 0xC084FC;
    public static final int DEF_SUCCESS = 0x4ADE80;
    public static final int DEF_WARNING = 0xFBBF24;
    public static final int DEF_DANGER  = 0xF87171;
    public static final float DEF_OPACITY = 0.62f;

    /** False draws the classic opaque template instead of the glass one. */
    public static boolean GLASS;

    /** Dims the game behind a window. */
    public static int OVERLAY;
    /** Darker bands of the classic template; the glass one leaves them out. */
    public static int HEADER, SIDEBAR;
    /** The glass itself: tint and opacity, lighter at the top than at the bottom. */
    public static int WINDOW_TOP, WINDOW;
    /** The rim of light around a glass panel, brighter at the top. */
    public static int RIM_TOP, RIM_BOTTOM;
    public static int WINDOW_BORDER;

    public static int SURFACE, SURFACE_HOVER, SURFACE_ACTIVE, BORDER, DIVIDER;

    public static int TEXT, TEXT_MUTED, TEXT_DIM;

    public static int ACCENT, ACCENT_DARK, ACCENT_LINE, ACCENT_SURFACE;
    public static int PURPLE, SUCCESS, WARNING;
    public static int DANGER, DANGER_DARK, DANGER_STRONG, DANGER_SURFACE, DANGER_LINE;

    public static int SWITCH_OFF, INPUT;

    public static final int ROW_PAD     = 7;
    public static final int ROW_GAP     = 4;
    public static final int BUTTON_H    = 16;
    public static final int CLOSE_SIZE  = 16;
    public static final int LINE_H      = 10;

    static { apply(new ModConfig()); }

    public static void apply(ModConfig c) {
        GLASS = c.glassEnabled;
        applyColors(c);
        if (GLASS) applyGlass(c); else applyClassic(c);
    }

    private static void applyColors(ModConfig c) {
        TEXT       = opaque(c.textColor);
        TEXT_MUTED = opaque(c.textMutedColor);
        TEXT_DIM   = opaque(c.textDimColor);
        ACCENT  = opaque(c.accentColor);
        PURPLE  = opaque(c.purpleColor);
        SUCCESS = opaque(c.successColor);
        WARNING = opaque(c.warningColor);
        DANGER  = opaque(c.dangerColor);
        DANGER_STRONG = opaque(Draw.lerpColor(c.dangerColor, 0x000000, 0.14f));
    }

    /** The opaque template from before the glass one. */
    private static void applyClassic(ModConfig c) {
        OVERLAY = 0xB0050608;
        WINDOW = WINDOW_TOP = 0xF2121419;
        WINDOW_BORDER = RIM_TOP = RIM_BOTTOM = 0xFF233243;
        HEADER = SIDEBAR = 0xFF0E1014;
        SURFACE        = 0xFF1A1D24;
        SURFACE_HOVER  = 0xFF22262F;
        SURFACE_ACTIVE = 0xFF2A2F3A;
        BORDER         = 0xFF2A2F3A;
        DIVIDER        = 0xFF1F232B;
        SWITCH_OFF     = 0xFF3A404C;
        INPUT          = 0xFF0F1115;

        boolean accent = (c.accentColor & 0xFFFFFF) == DEF_ACCENT, danger = (c.dangerColor & 0xFFFFFF) == DEF_DANGER;
        ACCENT_DARK    = accent ? 0xFF1E6F96 : opaque(Draw.lerpColor(c.accentColor, 0x000000, 0.42f));
        ACCENT_LINE    = accent ? 0xFF1B3A4F : opaque(Draw.lerpColor(c.accentColor, 0x121419, 0.75f));
        ACCENT_SURFACE = accent ? 0xFF172430 : opaque(Draw.lerpColor(c.accentColor, 0x121419, 0.85f));
        DANGER_DARK    = danger ? 0xFF7F1D1D : opaque(Draw.lerpColor(c.dangerColor, 0x000000, 0.50f));
        DANGER_STRONG  = danger ? 0xFFD63B3B : DANGER_STRONG;
        DANGER_SURFACE = danger ? 0xFF2A171B : opaque(Draw.lerpColor(c.dangerColor, 0x121419, 0.85f));
        DANGER_LINE    = danger ? 0xFF4D2127 : opaque(Draw.lerpColor(c.dangerColor, 0x121419, 0.70f));
    }

    private static void applyGlass(ModConfig c) {
        int glass = c.glassColor & 0xFFFFFF;
        float opacity = Math.max(0f, Math.min(1f, c.glassOpacity));
        // surfaces and lines are laid over the glass: light ink on a dark glass, dark ink on a light one
        boolean light = luminance(glass) > 0.55f;
        int ink = light ? 0x000000 : 0xFFFFFF;

        OVERLAY = 0x59000000;
        HEADER = SIDEBAR = 0;
        WINDOW     = alpha(glass, opacity);
        WINDOW_TOP = alpha(Draw.lerpColor(glass, 0xFFFFFF, 0.13f), opacity);
        RIM_TOP    = alpha(0xFFFFFF, 0.55f);
        RIM_BOTTOM = alpha(0xFFFFFF, 0.14f);
        WINDOW_BORDER = alpha(0xFFFFFF, 0.30f);

        SURFACE        = alpha(ink, 0.07f);
        SURFACE_HOVER  = alpha(ink, 0.12f);
        SURFACE_ACTIVE = alpha(ink, 0.18f);
        BORDER         = alpha(ink, 0.15f);
        DIVIDER        = alpha(ink, 0.10f);
        SWITCH_OFF     = alpha(ink, 0.22f);
        INPUT          = light ? alpha(0xFFFFFF, 0.35f) : alpha(0x000000, 0.30f);

        ACCENT_DARK    = opaque(Draw.lerpColor(c.accentColor, 0x000000, 0.42f));
        ACCENT_LINE    = alpha(c.accentColor, 0.40f);
        ACCENT_SURFACE = alpha(c.accentColor, 0.18f);
        DANGER_DARK    = alpha(Draw.lerpColor(c.dangerColor, 0x000000, 0.45f), 0.85f);
        DANGER_SURFACE = alpha(c.dangerColor, 0.16f);
        DANGER_LINE    = alpha(c.dangerColor, 0.40f);
    }

    private static int opaque(int rgb) { return 0xFF000000 | (rgb & 0xFFFFFF); }

    private static int alpha(int rgb, float a) { return (Math.round(a * 255f) << 24) | (rgb & 0xFFFFFF); }

    private static float luminance(int rgb) {
        return (0.299f * ((rgb >> 16) & 0xFF) + 0.587f * ((rgb >> 8) & 0xFF) + 0.114f * (rgb & 0xFF)) / 255f;
    }
}
