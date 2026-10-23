package com.hamtabot.obfutilities.ui.component;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public abstract class Component {

    protected int x, y, width;

    public void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public int getY() { return y; }

    public abstract int getHeight();

    public abstract void render(DrawContext ctx, int mouseX, int mouseY);

    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }

    public boolean keyPressed(int keyCode) { return false; }

    public boolean charTyped(char chr, int modifiers) { return false; }

    public boolean isCapturingKeys() { return false; }

    public boolean isHovered(double mx, double my) {
        return mx >= x && mx < x + width && my >= y && my < y + getHeight();
    }

    protected static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }
}
