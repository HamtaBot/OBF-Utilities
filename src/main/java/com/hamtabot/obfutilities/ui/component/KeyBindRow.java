package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.util.Format;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class KeyBindRow extends SettingRow {

    private static final int CONTROL_W = 76;

    private final IntSupplier getter;
    private final IntConsumer setter;
    private final boolean clearable;
    private boolean listening = false;

    public KeyBindRow(String title, String description, IntSupplier getter, IntConsumer setter) {
        this(title, description, getter, setter, false);
    }

    public KeyBindRow(String title, String description, IntSupplier getter, IntConsumer setter, boolean clearable) {
        super(title, description);
        this.getter = getter;
        this.setter = setter;
        this.clearable = clearable;
    }

    @Override protected int controlWidth() { return CONTROL_W; }

    @Override
    protected void renderControl(DrawContext ctx, int cx, int cy, int mouseX, int mouseY) {
        int key = getter.getAsInt();
        String label = listening ? "> ... <" : key < 0 ? "Aucune" : Format.keyName(key);
        Draw.button(ctx, tr(), cx, cy, CONTROL_W, controlHeight(), label,
                listening ? Draw.ButtonStyle.PRIMARY : Draw.ButtonStyle.SECONDARY, overControl(mouseX, mouseY), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (overControl(mouseX, mouseY)) {
            listening = !listening;
            Draw.playClick();
            return true;
        }
        listening = false;
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (!listening) return false;
        if (clearable && (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE)) setter.accept(-1);
        else if (keyCode != GLFW.GLFW_KEY_ESCAPE) setter.accept(keyCode);
        listening = false;
        return true;
    }

    @Override public boolean isCapturingKeys() { return listening; }
}
