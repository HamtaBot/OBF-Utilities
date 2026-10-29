package com.hamtabot.obfutilities.bossbar;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.HudPos;
import com.hamtabot.obfutilities.ui.Theme;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Job, skill and quest boss bars redrawn as progress cards. Each type is its own movable block holding one
 * card per bar the server shows, since several jobs, skills or quests can progress at once: the cards
 * sit side by side and wrap when the screen is full.
 */
public enum ProgressHud implements BarLayout.Element {

    JOBS(ProgressBar.Type.JOB, 46, true),
    SKILLS(ProgressBar.Type.SKILL, 92, true),
    QUESTS(ProgressBar.Type.QUEST, 138, true);

    private static final int PAD = 6, ICON = 10, TEXT_X = PAD + ICON + 4;
    private static final int H = 38, GAP = 3, MIN_W = 190, TRACK_Y = 17, TRACK_H = 7, INFO_Y = 28;
    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2f;
    private static final String LEVEL = "Niv. ", LEVEL_UP = "▲ Niv. ";
    private static final long LEAVE_MS = 320;
    private static final float LEAVE_RISE = 5f, LEAVE_SHRINK = 0.06f;

    public final ProgressBar.Type type;
    private final int defaultY;
    private final boolean sideBySide;
    private List<ProgressBar> preview;
    // cards on screen in order, including the ones fading out after the server removed their bar
    private final List<ProgressBar> shown = new ArrayList<>();
    private final Map<ProgressBar, Long> leaving = new IdentityHashMap<>();

    ProgressHud(ProgressBar.Type type, int defaultY, boolean sideBySide) {
        this.type = type;
        this.defaultY = defaultY;
        this.sideBySide = sideBySide;
    }

    public boolean isShown() { return enabled(OBFUtilities.config); }

    public boolean enabled(ModConfig cfg) {
        return switch (this) { case JOBS -> cfg.jobBarsEnabled; case SKILLS -> cfg.skillBarsEnabled; case QUESTS -> cfg.questBarsEnabled; };
    }

    public void setEnabled(boolean v) {
        ModConfig cfg = OBFUtilities.config;
        switch (this) { case JOBS -> cfg.jobBarsEnabled = v; case SKILLS -> cfg.skillBarsEnabled = v; case QUESTS -> cfg.questBarsEnabled = v; }
    }

    public float scale() {
        ModConfig cfg = OBFUtilities.config;
        return switch (this) { case JOBS -> cfg.jobBarsScale; case SKILLS -> cfg.skillBarsScale; case QUESTS -> cfg.questBarsScale; };
    }

    public void setScale(float s) {
        ModConfig cfg = OBFUtilities.config;
        s = clampScale(s);
        switch (this) { case JOBS -> cfg.jobBarsScale = s; case SKILLS -> cfg.skillBarsScale = s; case QUESTS -> cfg.questBarsScale = s; }
        BarLayout.invalidate();
    }

    public float opacity() {
        ModConfig cfg = OBFUtilities.config;
        return switch (this) { case JOBS -> cfg.jobBarsOpacity; case SKILLS -> cfg.skillBarsOpacity; case QUESTS -> cfg.questBarsOpacity; };
    }

    public void setOpacity(float o) {
        ModConfig cfg = OBFUtilities.config;
        o = Math.max(0f, Math.min(1f, o));
        switch (this) { case JOBS -> cfg.jobBarsOpacity = o; case SKILLS -> cfg.skillBarsOpacity = o; case QUESTS -> cfg.questBarsOpacity = o; }
    }

    private HudPos pos() {
        ModConfig cfg = OBFUtilities.config;
        return switch (this) { case JOBS -> cfg.jobBarsPos; case SKILLS -> cfg.skillBarsPos; case QUESTS -> cfg.questBarsPos; };
    }

    public static void renderAll(DrawContext ctx, boolean editing) {
        for (ProgressHud hud : values()) hud.render(ctx, editing);
    }

    private List<ProgressBar> bars(boolean editing) {
        List<ProgressBar> live = BossBars.bars(type);
        if (!live.isEmpty() || !editing) return live;
        if (preview == null) preview = List.of(ProgressBar.preview(type));
        return preview;
    }

    /** Keeps a removed bar in its slot while it fades out, so the cards below only move up once it is gone. */
    private List<ProgressBar> shownBars() {
        List<ProgressBar> live = BossBars.bars(type);
        long now = System.currentTimeMillis();
        for (ProgressBar bar : shown) {
            if (!live.contains(bar)) leaving.putIfAbsent(bar, now);
        }
        for (ProgressBar bar : live) {
            if (shown.contains(bar)) { leaving.remove(bar); continue; }
            // the same job/skill/quest coming back during its fade-out takes its slot back
            int slot = -1;
            for (int i = 0; i < shown.size(); i++) {
                ProgressBar old = shown.get(i);
                if (leaving.containsKey(old) && old.name.equals(bar.name)) { slot = i; break; }
            }
            if (slot >= 0) {
                leaving.remove(shown.get(slot));
                bar.fill.set(shown.get(slot).fill.update(shown.get(slot).target(), 0f));
                shown.set(slot, bar);
            } else {
                shown.add(bar);
            }
        }
        shown.removeIf(bar -> {
            Long start = leaving.get(bar);
            if (start == null || now - start < LEAVE_MS) return false;
            leaving.remove(bar);
            return true;
        });
        return shown;
    }

    /** 0 while the card is fully shown, eased up to 1 when its fade-out ends. */
    private float leaveProgress(ProgressBar bar) {
        Long start = leaving.get(bar);
        if (start == null) return 0f;
        float t = Math.min(1f, (System.currentTimeMillis() - start) / (float) LEAVE_MS);
        return 1f - (1f - t) * (1f - t);
    }

    private static int cardWidth(TextRenderer tr, ProgressBar bar) {
        int left = TEXT_X + tr.getWidth(bar.name()) + (bar.gain.isEmpty() ? 0 : 4 + tr.getWidth(bar.gain));
        int right = bar.levelText.isEmpty() ? 0 : 10 + tr.getWidth(LEVEL_UP + bar.levelText);
        int bottom = PAD + tr.getWidth(bar.progressText) + 10 + tr.getWidth("100,0 %");
        return Math.max(MIN_W, Math.max(left + right, bottom) + PAD);
    }

    /** {card width, block width, block height, columns}, unscaled. */
    private int[] size(TextRenderer tr, List<ProgressBar> bars) {
        int w = MIN_W;
        for (ProgressBar bar : bars) w = Math.max(w, cardWidth(tr, bar));
        int n = bars.size(), cols = 1;
        if (sideBySide && n > 1) {
            int room = Math.round(MinecraftClient.getInstance().getWindow().getScaledWidth() / scale());
            cols = Math.max(1, Math.min(n, (room + GAP) / (w + GAP)));
        }
        int rows = (n + cols - 1) / cols;
        return new int[]{ w, cols * w + (cols - 1) * GAP, rows * H + Math.max(0, rows - 1) * GAP, cols };
    }

    private List<ProgressBar> current(boolean editing) {
        return editing ? bars(true) : shownBars();
    }

    @Override
    public int[] rawBounds(boolean editing) {
        if (MinecraftClient.getInstance().player == null || !isShown()) return null;
        List<ProgressBar> bars = current(editing);
        if (bars.isEmpty()) return null;
        int[] sz = size(MinecraftClient.getInstance().textRenderer, bars);
        float scale = scale();
        int w = Math.round(sz[1] * scale), h = Math.round(sz[2] * scale);
        int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int[] p = pos().resolve(w, h, (screenW - w) / 2, defaultY);
        return new int[]{ p[0], p[1], w, h };
    }

    public void render(DrawContext ctx, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !isShown()) {
            shown.clear();
            leaving.clear();
            return;
        }
        int[] b = BarLayout.bounds(this, editing);
        if (b == null) return;
        List<ProgressBar> bars = current(editing);
        if (bars.isEmpty()) return;
        TextRenderer tr = client.textRenderer;
        int[] sz = size(tr, bars);
        int w = sz[0], cols = sz[3];
        float scale = scale();
        MatrixStack m = ctx.getMatrices();

        m.push();
        m.translate(b[0], b[1], 0);
        m.scale(scale, scale, 1f);
        float opacity = opacity();
        // every steady card in one batch, icons included; only a card fading out needs a batch of its own
        ctx.draw(() -> {
            for (int i = 0; i < bars.size(); i++) {
                if (!editing && leaving.containsKey(bars.get(i))) continue;
                m.push();
                m.translate(cardX(i, cols, w), cardY(i, cols), 0);
                renderCard(ctx, tr, bars.get(i), 0, w, editing, opacity);
                m.pop();
            }
        });
        if (!editing && !leaving.isEmpty()) {
            for (int i = 0; i < bars.size(); i++) {
                ProgressBar bar = bars.get(i);
                if (!leaving.containsKey(bar)) continue;
                m.push();
                m.translate(cardX(i, cols, w), cardY(i, cols), 0);
                renderLeaving(ctx, tr, bar, 0, w, leaveProgress(bar), opacity);
                m.pop();
            }
        }
        m.pop();

        if (editing) Draw.resizeGrip(ctx, b[0] + b[2], b[1] + b[3]);
    }

    private static int cardX(int i, int cols, int w) { return (i % cols) * (w + GAP); }

    private static int cardY(int i, int cols) { return (i / cols) * (H + GAP); }

    /** A removed card fades out while rising a little and shrinking towards its centre. */
    private static void renderLeaving(DrawContext ctx, TextRenderer tr, ProgressBar bar, int y, int w, float t, float opacity) {
        float alpha = 1f - t;
        if (alpha <= 0.02f) return;
        MatrixStack m = ctx.getMatrices();
        m.push();
        float s = 1f - LEAVE_SHRINK * t;
        m.translate(w / 2f, y + H / 2f - LEAVE_RISE * t, 0);
        m.scale(s, s, 1f);
        m.translate(-w / 2f, -(y + H / 2f), 0);
        // the shader colour fades fills, text and icon together; it has to stay set until the batch is flushed
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        ctx.draw(() -> renderCard(ctx, tr, bar, y, w, false, opacity));
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        m.pop();
    }

    /** {@code opacity} only fades the card background: the text and the progress bar stay readable. */
    private static void renderCard(DrawContext ctx, TextRenderer tr, ProgressBar bar, int y, int w, boolean editing, float opacity) {
        boolean levelUp = bar.justLeveledUp();
        int border = levelUp ? Theme.SUCCESS : editing ? Theme.ACCENT : Draw.withOpacity(Theme.WINDOW_BORDER, opacity);
        Draw.ringCard(ctx, 0, y, w, H, Draw.withOpacity(Theme.WINDOW, opacity), border);
        Draw.itemSprite(ctx, bar.icon, PAD, y + PAD - 1, ICON);

        int ty = y + PAD + 1;
        int right = w - PAD;
        if (!bar.levelText.isEmpty()) {
            int x = right - tr.getWidth(bar.levelText);
            ctx.drawText(tr, bar.levelText, x, ty, levelUp || bar.maxed ? Theme.SUCCESS : Theme.TEXT, false);
            String prefix = levelUp ? LEVEL_UP : LEVEL;
            x -= tr.getWidth(prefix);
            ctx.drawText(tr, prefix, x, ty, levelUp ? Theme.SUCCESS : Theme.TEXT_MUTED, false);
            right = x - 10;
        }
        String gain = bar.gain;
        int nameMax = right - TEXT_X - (gain.isEmpty() ? 0 : 4 + tr.getWidth(gain));
        String name = Draw.ellipsize(tr, bar.name(), nameMax);
        ctx.drawText(tr, name, TEXT_X, ty, bar.color, false);
        if (!gain.isEmpty()) ctx.drawText(tr, gain, TEXT_X + tr.getWidth(name) + 4, ty, Theme.SUCCESS, false);

        int tx = PAD, tw = w - PAD * 2;
        Draw.card(ctx, tx, y + TRACK_Y, tw, TRACK_H, Theme.INPUT, Theme.BORDER);
        int inner = tw - 2;
        int filled = Math.round(inner * bar.fill.update(bar.target(), 10f));
        if (filled > 0) {
            int fy = y + TRACK_Y + 1, fh = TRACK_H - 2;
            int steps = Math.min(ProgressBar.GRADIENT_STEPS, filled);
            for (int i = 0; i < steps; i++) {
                int x0 = tx + 1 + filled * i / steps, x1 = tx + 1 + filled * (i + 1) / steps;
                ctx.fill(x0, fy, x1, fy + fh, bar.gradient[i * ProgressBar.GRADIENT_STEPS / steps]);
            }
            // light top edge so the fill reads as a bar, not a flat block
            ctx.fill(tx + 1, fy, tx + 1 + filled, fy + 1, 0x33FFFFFF);
        }

        int iy = y + INFO_Y;
        ctx.drawText(tr, bar.progressText, PAD, iy, Theme.TEXT_MUTED, false);
        String pct = bar.percentText();
        ctx.drawText(tr, pct, w - PAD - tr.getWidth(pct), iy, bar.maxed ? Theme.SUCCESS : bar.color, false);
    }

    /** Where the block is drawn in the HUD editor, after the other blocks were moved out of its way. */
    public int[] bounds() {
        int[] b = BarLayout.bounds(this, true);
        return b != null ? b : new int[]{ 0, 0, 0, 0 };
    }

    public boolean isMouseOver(double mx, double my) {
        if (!isShown()) return false;
        int[] b = bounds();
        return Draw.inside(mx, my, b[0], b[1], b[2], b[3]);
    }

    public boolean isInResizeGrip(double mx, double my) {
        if (!isShown()) return false;
        int[] b = bounds();
        return Draw.inResizeGrip(mx, my, b[0] + b[2], b[1] + b[3]);
    }

    public void setPos(int x, int y) {
        int[] b = bounds();
        pos().set(x, y, b[2], b[3]);
        BarLayout.invalidate();
    }

    public void resetPos() {
        setScale(1f);
        int[] b = bounds();
        int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        setPos((screenW - b[2]) / 2, defaultY);
    }

    public static float clampScale(float s) { return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s)); }
}
