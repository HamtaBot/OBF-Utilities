package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ChipsRow extends Component {

    private static final int CHIP_H = 16, GAP = 4;

    private record Chip(String label, BooleanSupplier getter, Consumer<Boolean> setter) {}

    private final String title;
    private final List<Chip> chips = new ArrayList<>();

    public ChipsRow(String title) { this.title = title; }

    public ChipsRow add(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        chips.add(new Chip(label, getter, setter));
        return this;
    }

    private int chipWidth(Chip c) { return tr().getWidth(c.label()) + 20; }

    private List<int[]> layout() {
        List<int[]> pos = new ArrayList<>();
        int innerW = width - Theme.ROW_PAD * 2;
        int cx = 0, cy = 0;
        for (Chip c : chips) {
            int w = chipWidth(c);
            if (cx > 0 && cx + w > innerW) { cx = 0; cy += CHIP_H + GAP; }
            pos.add(new int[]{ Theme.ROW_PAD + cx, Theme.ROW_PAD + Theme.LINE_H + 4 + cy });
            cx += w + GAP;
        }
        return pos;
    }

    @Override
    public int getHeight() {
        List<int[]> pos = layout();
        int last = pos.isEmpty() ? Theme.ROW_PAD + Theme.LINE_H : pos.get(pos.size() - 1)[1] + CHIP_H;
        return last + Theme.ROW_PAD;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.card(ctx, x, y, width, getHeight(), Theme.SURFACE, Theme.BORDER);
        ctx.drawText(tr(), title, x + Theme.ROW_PAD, y + Theme.ROW_PAD, Theme.TEXT, false);

        List<int[]> pos = layout();
        for (int i = 0; i < chips.size(); i++) {
            Chip c = chips.get(i);
            int cx = x + pos.get(i)[0], cy = y + pos.get(i)[1], w = chipWidth(c);
            boolean on = c.getter().getAsBoolean();
            boolean hov = Draw.inside(mouseX, mouseY, cx, cy, w, CHIP_H);
            int bg = on ? (hov ? 0xFF22536B : 0xFF1A4256) : (hov ? Theme.SURFACE_ACTIVE : Theme.SURFACE_HOVER);
            int border = on ? Theme.ACCENT_DARK : Theme.BORDER;
            Draw.card(ctx, cx, cy, w, CHIP_H, bg, border);
            Draw.round(ctx, cx + 5, cy + 6, 4, 4, on ? Theme.SUCCESS : Theme.TEXT_DIM);
            ctx.drawText(tr(), c.label(), cx + 13, cy + 4, on ? Theme.TEXT : Theme.TEXT_MUTED, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        List<int[]> pos = layout();
        for (int i = 0; i < chips.size(); i++) {
            Chip c = chips.get(i);
            if (Draw.inside(mouseX, mouseY, x + pos.get(i)[0], y + pos.get(i)[1], chipWidth(c), CHIP_H)) {
                c.setter().accept(!c.getter().getAsBoolean());
                Draw.playClick();
                return true;
            }
        }
        return false;
    }
}
