package com.hamtabot.obfutilities.hud;

import com.hamtabot.obfutilities.kit.KitTracker;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.HudElement;
import com.hamtabot.obfutilities.ui.HudPos;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** A card titled after the active server, listing one name and status per line (kits, grades). */
public abstract class ListCardHud implements HudElement {

    private static final int PAD = 5, BAR_GAP = 8, LINE = 10, COL_GAP = 10;

    public record Row(String name, String status, int color) {}

    private final String word;
    private final int accent, defaultX, defaultY;
    /** The widest status a row can show, so the card doesn't wobble every second. */
    private final String widestStatus;

    protected ListCardHud(String word, int accent, String widestStatus, int defaultX, int defaultY) {
        this.word = word;
        this.accent = accent;
        this.widestStatus = widestStatus;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
    }

    protected abstract List<Row> rows();

    protected abstract HudPos pos();

    protected abstract void storeScale(float s);

    @Override
    public void setScale(float s) { storeScale(HudElement.clampScale(s)); }

    @Override
    public void setPos(int x, int y) {
        int[] b = bounds();
        pos().set(x, y, b[2], b[3]);
    }

    @Override
    public void resetPos() {
        setScale(1f);
        setPos(defaultX, defaultY);
    }

    private String title() {
        KitTracker.Server server = KitTracker.activeServer();
        return server == KitTracker.Server.ONEBLOCK ? word : word + " " + server.label;
    }

    private int[] size(TextRenderer tr, String title, List<Row> rows) {
        int nameW = tr.getWidth(title), statusW = 0;
        for (Row r : rows) {
            nameW = Math.max(nameW, tr.getWidth(r.name()));
            statusW = Math.max(statusW, tr.getWidth(r.status()));
        }
        if (statusW > 0) statusW = Math.max(statusW, tr.getWidth(widestStatus));
        int w = BAR_GAP + nameW + (statusW > 0 ? COL_GAP + statusW : 0) + PAD;
        int h = PAD * 2 + LINE * (rows.size() + 1) + 2;
        return new int[]{ w, h };
    }

    public void render(DrawContext ctx, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !isShown()) return;
        TextRenderer tr = client.textRenderer;
        String title = title();
        List<Row> rows = rows();
        int[] sz = size(tr, title, rows);
        int[] b = bounds(sz);
        float scale = scale();

        ctx.getMatrices().push();
        ctx.getMatrices().translate(b[0], b[1], 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        // one batch for the whole card instead of a flush per fill/drawText
        ctx.draw(() -> {
            Draw.overlayCard(ctx, 0, 0, sz[0], sz[1], accent, editing);
            ctx.drawText(tr, title, BAR_GAP, PAD, accent, false);
            ctx.fill(BAR_GAP, PAD + LINE, sz[0] - PAD, PAD + LINE + 1, Theme.DIVIDER);
            int y = PAD + LINE + 3;
            for (Row r : rows) {
                ctx.drawText(tr, r.name(), BAR_GAP, y, r.status().isEmpty() ? r.color() : Theme.TEXT, false);
                ctx.drawText(tr, r.status(), sz[0] - PAD - tr.getWidth(r.status()), y, r.color(), false);
                y += LINE;
            }
        });
        ctx.getMatrices().pop();

        if (editing) Draw.resizeGrip(ctx, b[0] + b[2], b[1] + b[3]);
    }

    @Override
    public int[] bounds() {
        return bounds(size(MinecraftClient.getInstance().textRenderer, title(), rows()));
    }

    private int[] bounds(int[] size) {
        float scale = scale();
        int w = Math.round(size[0] * scale), h = Math.round(size[1] * scale);
        int[] p = pos().resolve(w, h, defaultX, defaultY);
        return new int[]{ p[0], p[1], w, h };
    }
}
