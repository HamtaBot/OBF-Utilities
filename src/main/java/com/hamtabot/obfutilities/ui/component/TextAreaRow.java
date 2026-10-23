package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class TextAreaRow extends Component {

    private static final int PAD = Theme.ROW_PAD, LINES = 4, BOX_H = LINES * 9 + 8, COUNTER_H = 12;

    private final String placeholder;
    private final int maxLength;
    private final Consumer<String> onChange;
    private String value;
    private ThemedEditBox box;

    public TextAreaRow(String placeholder, int maxLength, String value, Consumer<String> onChange) {
        this.placeholder = placeholder;
        this.maxLength = maxLength;
        this.value = value;
        this.onChange = onChange;
        rebuild(100);
    }

    @Override
    public void setBounds(int x, int y, int width) {
        super.setBounds(x, y, width);
        if (box.getWidth() != width - PAD * 2) rebuild(width - PAD * 2);
        box.setX(x + PAD);
        box.setY(y + PAD);
    }

    private void rebuild(int w) {
        boolean focused = box != null && box.isFocused();
        box = new ThemedEditBox(placeholder, w);
        box.setMaxLength(maxLength);
        box.setText(value);
        box.setChangeListener(v -> { value = v; onChange.accept(v); });
        box.setFocused(focused);
    }

    @Override
    public int getHeight() { return PAD + BOX_H + COUNTER_H + PAD - 4; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.card(ctx, x, y, width, getHeight(), Theme.SURFACE, Theme.BORDER);
        box.render(ctx, mouseX, mouseY, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = Draw.inside(mouseX, mouseY, box.getX(), box.getY(), box.getWidth(), box.getHeight());
        box.setFocused(inside);
        return inside && box.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (!box.isFocused()) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { box.setFocused(false); return true; }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) return true;
        box.keyPressed(keyCode, 0, 0);
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) { return box.isFocused() && box.charTyped(chr, modifiers); }

    @Override
    public boolean isCapturingKeys() { return box.isFocused(); }

    private static class ThemedEditBox extends EditBoxWidget {

        ThemedEditBox(String placeholder, int width) {
            super(tr(), 0, 0, width, BOX_H, Text.literal(placeholder), Text.empty());
        }

        @Override
        protected void drawBox(DrawContext ctx, int x, int y, int w, int h) {
            Draw.inputFrame(ctx, x, y, w, h, isFocused());
        }
    }
}
