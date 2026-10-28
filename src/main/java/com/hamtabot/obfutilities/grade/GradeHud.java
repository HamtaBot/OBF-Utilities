package com.hamtabot.obfutilities.grade;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.kit.KitTracker;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class GradeHud {

    private static final int PAD = 5, BAR_GAP = 8, LINE = 10, COL_GAP = 10;
    public static final int DEFAULT_X = 10, DEFAULT_Y = 230;
    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2f;
    private static final String EMPTY = "Ouvre /grade";
    private static final long SOON_MS = 86_400_000L;

    private record Row(String name, String status, int color) {}

    private GradeHud() {}

    public static boolean isShown() {
        ModConfig cfg = OBFUtilities.config;
        return cfg.gradesEnabled && cfg.gradeHudEnabled;
    }

    private static String title() {
        KitTracker.Server server = KitTracker.activeServer();
        return server == KitTracker.Server.ONEBLOCK ? "Grades" : "Grades " + server.label;
    }

    private static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (GradeTracker.Grade grade : GradeTracker.grades(KitTracker.activeServer())) {
            GradeTracker.State s = GradeTracker.state(grade);
            switch (s.status) {
                case PERMANENT -> rows.add(new Row(grade.name(), "Permanent", Theme.SUCCESS));
                case TEMPORARY -> {
                    long left = GradeTracker.remaining(s);
                    rows.add(new Row(grade.name(), KitTracker.format(left), left <= SOON_MS ? Theme.WARNING : Theme.TEXT_MUTED));
                }
                default -> {}
            }
        }
        if (rows.isEmpty()) rows.add(new Row(EMPTY, "", Theme.TEXT_DIM));
        return rows;
    }

    private static int[] size(TextRenderer tr, List<Row> rows) {
        int nameW = tr.getWidth(title()), statusW = 0;
        for (Row r : rows) {
            nameW = Math.max(nameW, tr.getWidth(r.name()));
            statusW = Math.max(statusW, tr.getWidth(r.status()));
        }
        // reserve room for the widest countdown so the card doesn't wobble every second
        if (statusW > 0) statusW = Math.max(statusW, tr.getWidth("00j 00h"));
        int w = BAR_GAP + nameW + (statusW > 0 ? COL_GAP + statusW : 0) + PAD;
        int h = PAD * 2 + LINE * (rows.size() + 1) + 2;
        return new int[]{ w, h };
    }

    public static void render(DrawContext ctx, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !isShown()) return;
        TextRenderer tr = client.textRenderer;
        List<Row> rows = rows();
        int[] sz = size(tr, rows);
        int[] b = bounds(sz);
        float scale = OBFUtilities.config.gradeHudScale;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(b[0], b[1], 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        // one batch for the whole card instead of a flush per fill/drawText
        ctx.draw(() -> {
            Draw.overlayCard(ctx, 0, 0, sz[0], sz[1], Theme.PURPLE, editing);
            ctx.drawText(tr, title(), BAR_GAP, PAD, Theme.PURPLE, false);
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

    public static int[] bounds() {
        return bounds(size(MinecraftClient.getInstance().textRenderer, rows()));
    }

    private static int[] bounds(int[] size) {
        ModConfig cfg = OBFUtilities.config;
        int w = Math.round(size[0] * cfg.gradeHudScale), h = Math.round(size[1] * cfg.gradeHudScale);
        int[] p = cfg.gradeHudPos.resolve(w, h, DEFAULT_X, DEFAULT_Y);
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
        OBFUtilities.config.gradeHudPos.set(x, y, b[2], b[3]);
    }

    public static void setScale(float s) { OBFUtilities.config.gradeHudScale = clampScale(s); }

    public static float clampScale(float s) { return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s)); }
}
