package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.function.IntFunction;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class SliderRow extends SettingRow {

    private static final int TRACK_W = 90;
    private static final int LABEL_W = 44;
    private static final int KNOB    = 8;

    private final int min, max;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final IntFunction<String> label;
    private boolean dragging = false;

    public SliderRow(String title, String description, int min, int max,
                     IntSupplier getter, IntConsumer setter, IntFunction<String> label) {
        super(title, description);
        this.min = min;
        this.max = max;
        this.getter = getter;
        this.setter = setter;
        this.label = label;
    }

    @Override protected int controlWidth() { return TRACK_W + LABEL_W; }

    private int trackX() { return x + width - Theme.ROW_PAD - controlWidth() + LABEL_W; }

    @Override
    protected void renderControl(DrawContext ctx, int cx, int cy, int mouseX, int mouseY) {
        float p = (getter.getAsInt() - min) / (float) (max - min);
        int tx = cx + LABEL_W;
        int midY = cy + controlHeight() / 2;

        String text = label.apply(getter.getAsInt());
        ctx.drawText(tr(), text, tx - 6 - tr().getWidth(text), midY - 4, Theme.TEXT, false);

        Draw.round(ctx, tx, midY - 2, TRACK_W, 4, Theme.SWITCH_OFF);
        int fill = Math.round((TRACK_W - KNOB) * p) + KNOB / 2;
        Draw.round(ctx, tx, midY - 2, fill, 4, Theme.ACCENT_DARK);

        boolean hov = dragging || overControl(mouseX, mouseY);
        int knobX = tx + Math.round((TRACK_W - KNOB) * p);
        Draw.round(ctx, knobX, midY - KNOB / 2, KNOB, KNOB, hov ? 0xFFFFFFFF : 0xFFD0D5DD);
    }

    private void update(double mouseX) {
        float p = (float) ((mouseX - trackX() - KNOB / 2.0) / (TRACK_W - KNOB));
        p = Math.max(0f, Math.min(1f, p));
        setter.accept(min + Math.round(p * (max - min)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !overControl(mouseX, mouseY) || mouseX < trackX() - KNOB) return false;
        dragging = true;
        update(mouseX);
        Draw.playClick();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!dragging) return false;
        update(mouseX);
        return true;
    }

    @Override
    public void mouseReleased() { dragging = false; }
}
