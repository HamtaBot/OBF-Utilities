package com.hamtabot.obfutilities.bossbar;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

/** The server's info boss bars (money, tokens, island...) redrawn as one card, a row per bar. */
public final class InfoBarHud {

    private static final int PAD = 6, ROW_H = 13, CHIP_GAP = 13, SHRINK_SLACK = 24;
    public static final int DEFAULT_Y = 4;
    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2f;

    private static List<List<BossBars.Chip>> preview;
    // values change width every tick; the card only shrinks once the gap is clear so it doesn't wobble
    private static int stickyW, stickyRows;

    public static final BarLayout.Element LAYOUT = InfoBarHud::rawBounds;

    private InfoBarHud() {}

    public static boolean isShown() { return OBFUtilities.config.infoBarsEnabled; }

    static void resetWidth() { stickyW = stickyRows = 0; }

    private static boolean separators() { return OBFUtilities.config.infoBarsSeparators; }

    /** Separators belong to the background: they fade with it and are gone on a transparent card. */
    private static int separator(int color) { return Draw.withOpacity(color, OBFUtilities.config.infoBarsOpacity); }

    private static List<List<BossBars.Chip>> rows(boolean editing) {
        List<List<BossBars.Chip>> live = BossBars.infos();
        if (!live.isEmpty() || !editing) return live;
        if (preview == null) {
            preview = List.of(
                    BossBars.chips(Text.literal("Argent 1.7Mi  Mobcoins 222.81m  Vote party 286/300  Tokens 1090"), false),
                    BossBars.chips(Text.literal("ɴɪᴠᴇᴀᴜ ᴅ'ɪʟᴇ 56.2m   ʙᴀɴᴋ ᴅ'ɪʟᴇ 26.9Mi   ᴛᴇᴍᴘѕ ᴀꜰᴋ 0m"), false));
        }
        return preview;
    }

    /** Width of the row without its hidden infos, 0 when they are all hidden. */
    private static int rowWidth(List<BossBars.Chip> row) {
        int w = 0;
        for (BossBars.Chip chip : row) {
            if (BossBars.isInfoHidden(chip)) continue;
            w += (w > 0 ? CHIP_GAP : 0) + chip.width();
        }
        return w;
    }

    private static int shownRows(List<List<BossBars.Chip>> rows) {
        int n = 0;
        for (List<BossBars.Chip> row : rows) if (rowWidth(row) > 0) n++;
        return n;
    }

    private static int[] size(List<List<BossBars.Chip>> rows) {
        int content = 0;
        for (List<BossBars.Chip> row : rows) content = Math.max(content, rowWidth(row));
        int shown = shownRows(rows);
        int w = PAD * 2 + content;
        if (shown != stickyRows || w > stickyW || w < stickyW - SHRINK_SLACK) {
            stickyW = w;
            stickyRows = shown;
        }
        return new int[]{ stickyW, PAD * 2 + shown * ROW_H - 3 };
    }

    public static void render(DrawContext ctx, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !isShown()) return;
        int[] b = BarLayout.bounds(LAYOUT, editing);
        if (b == null) return;
        List<List<BossBars.Chip>> rows = rows(editing);
        if (shownRows(rows) == 0) return;
        TextRenderer tr = client.textRenderer;
        int[] sz = size(rows);
        float scale = OBFUtilities.config.infoBarsScale;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(b[0], b[1], 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.draw(() -> {
            float opacity = OBFUtilities.config.infoBarsOpacity;
            Draw.ringCard(ctx, 0, 0, sz[0], sz[1], Draw.withOpacity(Theme.WINDOW, opacity),
                    editing ? Theme.ACCENT : Draw.withOpacity(Theme.WINDOW_BORDER, opacity));
            int y = PAD;
            for (List<BossBars.Chip> row : rows) {
                if (rowWidth(row) == 0) continue;
                if (y > PAD && separators()) ctx.fill(PAD, y - 3, sz[0] - PAD, y - 2, separator(Theme.DIVIDER));
                renderRow(ctx, tr, row, y + 1, sz[0] - PAD * 2);
                y += ROW_H;
            }
        });
        ctx.getMatrices().pop();

        if (editing) Draw.resizeGrip(ctx, b[0] + b[2], b[1] + b[3]);
    }

    /** Spreads the row over the card width: the widest row keeps CHIP_GAP, shorter ones get wider gaps. */
    private static void renderRow(DrawContext ctx, TextRenderer tr, List<BossBars.Chip> row, int y, int innerW) {
        int shown = 0, content = 0;
        for (BossBars.Chip chip : row) {
            if (BossBars.isInfoHidden(chip)) continue;
            shown++;
            content += chip.width();
        }
        if (shown == 1) {
            for (BossBars.Chip chip : row) {
                if (!BossBars.isInfoHidden(chip)) drawChip(ctx, tr, chip, PAD + (innerW - content) / 2, y);
            }
            return;
        }
        float gap = (innerW - content) / (float) (shown - 1);
        float x = PAD;
        boolean first = true;
        for (BossBars.Chip chip : row) {
            if (BossBars.isInfoHidden(chip)) continue;
            if (!first) {
                int line = Math.round(x + gap / 2f);
                if (separators()) ctx.fill(line, y - 1, line + 1, y + 8, separator(Theme.BORDER));
                x += gap;
            }
            first = false;
            drawChip(ctx, tr, chip, Math.round(x), y);
            x += chip.width();
        }
    }

    /** Draws the chip's icons and text from {@code x} and returns where it ends. */
    public static int drawChip(DrawContext ctx, TextRenderer tr, BossBars.Chip chip, int x, int y) {
        for (BossBars.Piece p : chip.pieces()) {
            x += p.gapBefore();
            if (p.dy() == 0f) {
                ctx.drawText(tr, p.text(), x, y, 0xFFFFFFFF, false);
            } else {
                // half-pixel offsets are common for icons, a translate keeps them exact on high GUI scales
                ctx.getMatrices().push();
                ctx.getMatrices().translate(0, p.dy(), 0);
                ctx.drawText(tr, p.text(), x, y, 0xFFFFFFFF, false);
                ctx.getMatrices().pop();
            }
            x += p.width();
        }
        return x;
    }

    /** Where the card is drawn in the HUD editor, after the other blocks were moved out of its way. */
    public static int[] bounds() {
        int[] b = BarLayout.bounds(LAYOUT, true);
        return b != null ? b : new int[]{ 0, 0, 0, 0 };
    }

    private static int[] rawBounds(boolean editing) {
        if (MinecraftClient.getInstance().player == null || !isShown()) return null;
        List<List<BossBars.Chip>> rows = rows(editing);
        if (shownRows(rows) == 0) return null;
        return bounds(size(rows));
    }

    private static int[] bounds(int[] size) {
        ModConfig cfg = OBFUtilities.config;
        int w = Math.round(size[0] * cfg.infoBarsScale), h = Math.round(size[1] * cfg.infoBarsScale);
        int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int[] p = cfg.infoBarsPos.resolve(w, h, (screenW - w) / 2, DEFAULT_Y);
        return new int[]{ p[0], p[1], w, h };
    }

    public static boolean isMouseOver(double mx, double my) {
        if (!isShown()) return false;
        int[] b = bounds();
        return Draw.inside(mx, my, b[0], b[1], b[2], b[3]);
    }

    public static boolean isInResizeGrip(double mx, double my) {
        if (!isShown()) return false;
        int[] b = bounds();
        return Draw.inResizeGrip(mx, my, b[0] + b[2], b[1] + b[3]);
    }

    public static void setPos(int x, int y) {
        int[] b = bounds();
        OBFUtilities.config.infoBarsPos.set(x, y, b[2], b[3]);
        BarLayout.invalidate();
    }

    public static void resetPos() {
        int[] b = bounds();
        int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        setPos((screenW - b[2]) / 2, DEFAULT_Y);
    }

    public static void setScale(float s) {
        OBFUtilities.config.infoBarsScale = clampScale(s);
        BarLayout.invalidate();
    }

    public static float clampScale(float s) { return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s)); }
}
