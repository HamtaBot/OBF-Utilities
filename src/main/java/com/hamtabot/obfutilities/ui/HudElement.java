package com.hamtabot.obfutilities.ui;

/** A HUD the editor moves and resizes. */
public interface HudElement {

    float MIN_SCALE = 0.5f, MAX_SCALE = 2f;

    static float clampScale(float s) { return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s)); }

    boolean isShown();

    /** {x, y, width, height} on screen. */
    int[] bounds();

    float scale();

    void setScale(float s);

    void setPos(int x, int y);

    /** Back to the default position and size. */
    void resetPos();

    default boolean isMouseOver(double mx, double my) {
        if (!isShown()) return false;
        int[] b = bounds();
        return Draw.inside(mx, my, b[0], b[1], b[2], b[3]);
    }

    default boolean isInResizeGrip(double mx, double my) {
        if (!isShown()) return false;
        int[] b = bounds();
        return Draw.inResizeGrip(mx, my, b[0] + b[2], b[1] + b[3]);
    }
}
