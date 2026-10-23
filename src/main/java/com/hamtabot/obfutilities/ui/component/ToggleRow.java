package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Anim;
import com.hamtabot.obfutilities.ui.Draw;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ToggleRow extends SettingRow {

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Anim knob;

    public ToggleRow(String title, String description, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(title, description);
        this.getter = getter;
        this.setter = setter;
        this.knob = new Anim(getter.getAsBoolean() ? 1f : 0f);
    }

    @Override protected int controlWidth()  { return Draw.SWITCH_W; }
    @Override protected int controlHeight() { return Draw.SWITCH_H; }

    @Override
    protected void renderControl(DrawContext ctx, int cx, int cy, int mouseX, int mouseY) {
        float p = knob.update(getter.getAsBoolean() ? 1f : 0f, 16f);
        Draw.toggle(ctx, cx, cy, p, isHovered(mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;
        setter.accept(!getter.getAsBoolean());
        Draw.playClick();
        return true;
    }
}
