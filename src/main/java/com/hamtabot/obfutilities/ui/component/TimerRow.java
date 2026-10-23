package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.timer.TimerEntry;
import com.hamtabot.obfutilities.timer.Timers;
import com.hamtabot.obfutilities.ui.Anim;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;

public class TimerRow extends Component {

    private static final int H = 30, BTN = 16;

    private enum Action { PLAY, RESTART, EDIT }

    private final int index;
    private final TimerEntry timer;
    private final Runnable onEdit, onDelete;
    private final Anim knob;

    public TimerRow(int index, TimerEntry timer, Runnable onEdit, Runnable onDelete) {
        this.index = index;
        this.timer = timer;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.knob = new Anim(timer.showOnScreen ? 1f : 0f);
    }

    @Override public int getHeight() { return H; }

    private int delX()    { return x + width - Theme.ROW_PAD - BTN; }
    private int switchX() { return delX() - 8 - Draw.SWITCH_W; }
    private int actionX(Action a) { return switchX() - 8 - (Action.values().length - a.ordinal()) * (BTN + 3) + 3; }
    private int ctrlY(int h) { return y + (H - h) / 2; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean hov = isHovered(mouseX, mouseY);
        Draw.card(ctx, x, y, width, H, hov ? Theme.SURFACE_HOVER : Theme.SURFACE, Theme.BORDER);
        ctx.fill(x + 1, y + 3, x + 3, y + H - 3, Timers.color(timer));

        int textW = actionX(Action.PLAY) - (x + 10) - 4;
        ctx.drawText(tr(), Draw.ellipsize(tr(), Timers.label(index, timer), textW), x + 10, y + 6, Theme.TEXT, false);
        String kind = timer.type().label + "  ·  ";
        int kindW = tr().getWidth(kind);
        ctx.drawText(tr(), Draw.ellipsize(tr(), kind, textW), x + 10, y + 17, Theme.TEXT_DIM, false);
        if (kindW < textW) {
            ctx.drawText(tr(), Draw.ellipsize(tr(), Timers.status(timer), textW - kindW), x + 10 + kindW, y + 17, Timers.statusColor(timer), false);
        }

        for (Action a : Action.values()) renderAction(ctx, a, mouseX, mouseY);

        Draw.toggle(ctx, switchX(), ctrlY(Draw.SWITCH_H), knob.update(timer.showOnScreen ? 1f : 0f, 16f),
                Draw.inside(mouseX, mouseY, switchX(), ctrlY(Draw.SWITCH_H), Draw.SWITCH_W, Draw.SWITCH_H));
        Draw.closeButton(ctx, delX(), ctrlY(BTN), BTN, Draw.inside(mouseX, mouseY, delX(), ctrlY(BTN), BTN, BTN));
    }

    private void renderAction(DrawContext ctx, Action a, int mouseX, int mouseY) {
        int bx = actionX(a), by = ctrlY(BTN);
        boolean hov = Draw.inside(mouseX, mouseY, bx, by, BTN, BTN);
        boolean primary = a == Action.PLAY && !timer.running;
        Draw.button(ctx, tr(), bx, by, BTN, BTN, "", primary ? Draw.ButtonStyle.PRIMARY : Draw.ButtonStyle.SECONDARY, hov, true);

        int cx = bx + BTN / 2, cy = by + BTN / 2, c = 0xFFFFFFFF;
        switch (a) {
            case PLAY -> {
                if (timer.running) Draw.pauseIcon(ctx, cx, cy, c);
                else Draw.playIcon(ctx, cx, cy, c);
            }
            case RESTART -> Draw.restartIcon(ctx, cx, cy, c);
            case EDIT    -> Draw.editIcon(ctx, cx, cy, c);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;
        if (Draw.inside(mouseX, mouseY, delX(), ctrlY(BTN), BTN, BTN)) {
            Draw.playClick();
            onDelete.run();
            return true;
        }
        for (Action a : Action.values()) {
            if (!Draw.inside(mouseX, mouseY, actionX(a), ctrlY(BTN), BTN, BTN)) continue;
            Draw.playClick();
            switch (a) {
                case PLAY    -> timer.toggle();
                case RESTART -> timer.restart();
                case EDIT    -> onEdit.run();
            }
            return true;
        }
        if (Draw.inside(mouseX, mouseY, switchX() - 4, y, Draw.SWITCH_W + 8, H)) {
            timer.showOnScreen = !timer.showOnScreen;
            Draw.playClick();
            return true;
        }
        return false;
    }
}
