package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Draw;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class ButtonRow extends SettingRow {

    private final Supplier<String> label;
    private final Runnable action;
    private final BooleanSupplier enabled;
    private final Draw.ButtonStyle style;
    private final int minWidth;

    public ButtonRow(String title, String description, Supplier<String> label, Draw.ButtonStyle style,
                     BooleanSupplier enabled, Runnable action) {
        super(title, description);
        this.label = label;
        this.action = action;
        this.enabled = enabled;
        this.style = style;
        this.minWidth = tr().getWidth(label.get()) + 16;
    }

    public ButtonRow(String title, String description, String label, Draw.ButtonStyle style, Runnable action) {
        this(title, description, () -> label, style, () -> true, action);
    }

    @Override
    protected int controlWidth() {
        int w = Math.max(minWidth, tr().getWidth(label.get()) + 16);
        return Math.min(w, width / 2);
    }

    @Override
    protected void renderControl(DrawContext ctx, int cx, int cy, int mouseX, int mouseY) {
        boolean on = enabled.getAsBoolean();
        Draw.button(ctx, tr(), cx, cy, controlWidth(), controlHeight(), label.get(), style,
                on && overControl(mouseX, mouseY), on);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !overControl(mouseX, mouseY) || !enabled.getAsBoolean()) return false;
        Draw.playClick();
        action.run();
        return true;
    }
}
