package com.hamtabot.obfutilities.bossbar;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.HudElement;
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
    /** Passes of {@link #drawChip}: everything, only the pack icons, only the text. */
    public static final int ALL = -1, ICONS = 1, TEXT = 2;

    private static List<List<BossBars.Chip>> preview;
    // values change width every tick; the card only shrinks once the gap is clear so it doesn't wobble
    private static int stickyW, stickyRows;
    // what rawBounds measured for this frame's layout, reused by render: {width, height}
    private static List<List<BossBars.Chip>> frameRows;
    private static int[] frameSize;

    /** The card as a block of the layout and the HUD editor. */
    public static final BarLayout.Block BLOCK = new BarLayout.Block() {
        @Override public int[] rawBounds(boolean editing) { return InfoBarHud.rawBounds(editing); }
        @Override public boolean isShown() { return InfoBarHud.isShown(); }
        @Override public float scale() { return OBFUtilities.config.infoBarsScale; }

        @Override
        public void setScale(float s) {
            OBFUtilities.config.infoBarsScale = HudElement.clampScale(s);
            BarLayout.invalidate();
        }

        @Override
        public void setPos(int x, int y) {
            int[] b = bounds();
            OBFUtilities.config.infoBarsPos.set(x, y, b[2], b[3]);
            BarLayout.invalidate();
        }

        @Override
        public void resetPos() {
            setScale(1f);
            int[] b = bounds();
            int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
            setPos((screenW - b[2]) / 2, DEFAULT_Y);
        }
    };

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

    private static int[] rawBounds(boolean editing) {
        frameRows = null;
        if (MinecraftClient.getInstance().player == null || !isShown()) return null;
        List<List<BossBars.Chip>> rows = rows(editing);
        int content = 0, shown = 0;
        for (List<BossBars.Chip> row : rows) {
            int w = rowWidth(row);
            if (w == 0) continue;
            content = Math.max(content, w);
            shown++;
        }
        if (shown == 0) return null;
        int w = PAD * 2 + content;
        if (shown != stickyRows || w > stickyW || w < stickyW - SHRINK_SLACK) {
            stickyW = w;
            stickyRows = shown;
        }
        frameRows = rows;
        frameSize = new int[]{ stickyW, PAD * 2 + shown * ROW_H - 3 };

        ModConfig cfg = OBFUtilities.config;
        int sw = Math.round(frameSize[0] * cfg.infoBarsScale), sh = Math.round(frameSize[1] * cfg.infoBarsScale);
        int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int[] p = cfg.infoBarsPos.resolve(sw, sh, (screenW - sw) / 2, DEFAULT_Y);
        return new int[]{ p[0], p[1], sw, sh };
    }

    public static void render(DrawContext ctx, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !isShown()) return;
        int[] b = BarLayout.bounds(BLOCK, editing);
        if (b == null || frameRows == null) return;
        List<List<BossBars.Chip>> rows = frameRows;
        int[] sz = frameSize;
        TextRenderer tr = client.textRenderer;
        float scale = OBFUtilities.config.infoBarsScale;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(b[0], b[1], 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        // background and separators, then the pack icons, then the text: switching render layer flushes the
        // buffer, so drawing layer by layer costs three draws instead of a few per info
        ctx.draw(() -> {
            float opacity = OBFUtilities.config.infoBarsOpacity;
            Draw.card(ctx, 0, 0, sz[0], sz[1], Draw.withOpacity(Theme.WINDOW, opacity),
                    editing ? Theme.ACCENT : Draw.withOpacity(Theme.WINDOW_BORDER, opacity));
            for (int pass = 0; pass <= TEXT; pass++) {
                int y = PAD;
                for (List<BossBars.Chip> row : rows) {
                    if (rowWidth(row) == 0) continue;
                    if (pass == 0 && y > PAD && separators()) ctx.fill(PAD, y - 3, sz[0] - PAD, y - 2, separator(Theme.DIVIDER));
                    renderRow(ctx, tr, row, y + 1, sz[0] - PAD * 2, pass);
                    y += ROW_H;
                }
            }
        });
        ctx.getMatrices().pop();

        if (editing) Draw.resizeGrip(ctx, b[0] + b[2], b[1] + b[3]);
    }

    /**
     * Spreads the row over the card width: the widest row keeps CHIP_GAP, shorter ones get wider gaps.
     * Pass 0 draws the separators, the others the chips' icons or text.
     */
    private static void renderRow(DrawContext ctx, TextRenderer tr, List<BossBars.Chip> row, int y, int innerW, int pass) {
        int shown = 0, content = 0;
        for (BossBars.Chip chip : row) {
            if (BossBars.isInfoHidden(chip)) continue;
            shown++;
            content += chip.width();
        }
        if (shown == 1) {
            if (pass == 0) return;
            for (BossBars.Chip chip : row) {
                if (!BossBars.isInfoHidden(chip)) drawChip(ctx, tr, chip, PAD + (innerW - content) / 2, y, pass);
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
                if (pass == 0 && separators()) ctx.fill(line, y - 1, line + 1, y + 8, separator(Theme.BORDER));
                x += gap;
            }
            first = false;
            if (pass != 0) drawChip(ctx, tr, chip, Math.round(x), y, pass);
            x += chip.width();
        }
    }

    /** Draws the chip from {@code x}: {@link #ALL} its pieces, or only its {@link #ICONS} or its {@link #TEXT}. */
    public static void drawChip(DrawContext ctx, TextRenderer tr, BossBars.Chip chip, int x, int y, int pass) {
        for (BossBars.Piece p : chip.pieces()) {
            x += p.gapBefore();
            if (pass == ALL || (pass == ICONS) == p.icon()) {
                if (p.dy() == 0f) {
                    ctx.drawText(tr, p.text(), x, y, 0xFFFFFFFF, false);
                } else {
                    // half-pixel offsets are common for icons, a translate keeps them exact on high GUI scales
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(0, p.dy(), 0);
                    ctx.drawText(tr, p.text(), x, y, 0xFFFFFFFF, false);
                    ctx.getMatrices().pop();
                }
            }
            x += p.width();
        }
    }
}
