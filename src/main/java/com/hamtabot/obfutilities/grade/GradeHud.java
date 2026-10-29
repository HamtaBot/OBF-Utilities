package com.hamtabot.obfutilities.grade;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.hud.ListCardHud;
import com.hamtabot.obfutilities.kit.KitTracker;
import com.hamtabot.obfutilities.ui.HudPos;
import com.hamtabot.obfutilities.ui.Theme;

import java.util.ArrayList;
import java.util.List;

public final class GradeHud extends ListCardHud {

    public static final GradeHud INSTANCE = new GradeHud();
    private static final String EMPTY = "Ouvre /grade";
    private static final long SOON_MS = 86_400_000L;

    private GradeHud() { super("Grades", Theme.PURPLE, "00j 00h", 10, 230); }

    @Override
    public boolean isShown() {
        ModConfig cfg = OBFUtilities.config;
        return cfg.gradesEnabled && cfg.gradeHudEnabled;
    }

    @Override public float scale() { return OBFUtilities.config.gradeHudScale; }

    @Override protected void storeScale(float s) { OBFUtilities.config.gradeHudScale = s; }

    @Override protected HudPos pos() { return OBFUtilities.config.gradeHudPos; }

    @Override
    protected List<Row> rows() {
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
}
