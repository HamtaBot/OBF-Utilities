package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.bossbar.ProgressBar;
import com.hamtabot.obfutilities.objective.ObjectiveEntry;
import com.hamtabot.obfutilities.objective.Objectives;
import com.hamtabot.obfutilities.ui.Anim;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;

public class ObjectiveRow extends Component {

    private static final int H = 30, BTN = 16;

    private final ObjectiveEntry objective;
    private final Runnable onEdit, onDelete;
    private final Anim knob;

    public ObjectiveRow(ObjectiveEntry objective, Runnable onEdit, Runnable onDelete) {
        this.objective = objective;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.knob = new Anim(objective.showOnScreen ? 1f : 0f);
    }

    @Override public int getHeight() { return H; }

    private int delX()     { return x + width - Theme.ROW_PAD - BTN; }
    private int switchX()  { return delX() - 8 - Draw.SWITCH_W; }
    private int editX()    { return switchX() - 8 - BTN; }
    private int restartX() { return editX() - 3 - BTN; }
    private int ctrlY(int h) { return y + (H - h) / 2; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean hov = isHovered(mouseX, mouseY);
        boolean done = objective.isDone();
        Draw.card(ctx, x, y, width, H, hov ? Theme.SURFACE_HOVER : Theme.SURFACE, Theme.BORDER);
        ctx.fill(x + 1, y + 3, x + 3, y + H - 3, done ? Theme.SUCCESS : objective.action().color);

        ProgressBar card = Objectives.card(objective);
        int textW = restartX() - (x + 10) - 4;
        ctx.drawText(tr(), Draw.ellipsize(tr(), objective.displayName(), textW), x + 10, y + 6, Theme.TEXT, false);
        String status = card.progressText + "  ·  " + card.percentText();
        ctx.drawText(tr(), Draw.ellipsize(tr(), status, textW), x + 10, y + 17, done ? Theme.SUCCESS : Theme.TEXT_DIM, false);

        renderAction(ctx, restartX(), false, mouseX, mouseY);
        renderAction(ctx, editX(), true, mouseX, mouseY);

        Draw.toggle(ctx, switchX(), ctrlY(Draw.SWITCH_H), knob.update(objective.showOnScreen ? 1f : 0f, 16f),
                Draw.inside(mouseX, mouseY, switchX(), ctrlY(Draw.SWITCH_H), Draw.SWITCH_W, Draw.SWITCH_H));
        Draw.closeButton(ctx, delX(), ctrlY(BTN), BTN, Draw.inside(mouseX, mouseY, delX(), ctrlY(BTN), BTN, BTN));
    }

    private void renderAction(DrawContext ctx, int bx, boolean edit, int mouseX, int mouseY) {
        int by = ctrlY(BTN);
        Draw.button(ctx, tr(), bx, by, BTN, BTN, "", Draw.ButtonStyle.SECONDARY, Draw.inside(mouseX, mouseY, bx, by, BTN, BTN), true);
        if (edit) Draw.editIcon(ctx, bx + BTN / 2, by + BTN / 2, 0xFFFFFFFF);
        else Draw.restartIcon(ctx, bx + BTN / 2, by + BTN / 2, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;
        if (Draw.inside(mouseX, mouseY, delX(), ctrlY(BTN), BTN, BTN)) {
            Draw.playClick();
            onDelete.run();
            return true;
        }
        if (Draw.inside(mouseX, mouseY, restartX(), ctrlY(BTN), BTN, BTN)) {
            Draw.playClick();
            Objectives.restart(objective);
            return true;
        }
        if (Draw.inside(mouseX, mouseY, editX(), ctrlY(BTN), BTN, BTN)) {
            Draw.playClick();
            onEdit.run();
            return true;
        }
        if (Draw.inside(mouseX, mouseY, switchX() - 4, y, Draw.SWITCH_W + 8, H)) {
            objective.showOnScreen = !objective.showOnScreen;
            Draw.playClick();
            return true;
        }
        return false;
    }
}
