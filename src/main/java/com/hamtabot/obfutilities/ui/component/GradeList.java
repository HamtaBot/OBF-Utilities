package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.grade.GradeTracker;
import com.hamtabot.obfutilities.kit.KitTracker;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Live grade statuses of one server. */
public class GradeList extends Component {

    private static final int LINE = 16, PAD = 4;

    private final List<GradeTracker.Grade> grades;

    public GradeList(KitTracker.Server server) { grades = GradeTracker.grades(server); }

    @Override public int getHeight() { return PAD * 2 + LINE * grades.size(); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.card(ctx, x, y, width, getHeight(), Theme.SURFACE, Theme.BORDER);
        for (int i = 0; i < grades.size(); i++) {
            GradeTracker.Grade grade = grades.get(i);
            int ty = y + PAD + i * LINE + (LINE - 8) / 2 + 1;
            GradeTracker.State s = GradeTracker.state(grade);
            String status;
            int color;
            switch (s.status) {
                case PERMANENT -> { status = "Permanent"; color = Theme.SUCCESS; }
                case TEMPORARY -> { status = "Expire dans " + KitTracker.format(GradeTracker.remaining(s)); color = Theme.WARNING; }
                case INACTIVE -> { status = "Non actif"; color = Theme.TEXT_DIM; }
                default -> { status = "Inconnu"; color = Theme.TEXT_DIM; }
            }
            ctx.drawText(tr(), grade.name(), x + Theme.ROW_PAD, ty, s.status == GradeTracker.Status.INACTIVE ? Theme.TEXT_MUTED : Theme.TEXT, false);
            ctx.drawText(tr(), status, x + width - Theme.ROW_PAD - tr().getWidth(status), ty, color, false);
        }
    }
}
