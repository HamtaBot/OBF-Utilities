package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;

public class SectionTitle extends Component {

    private final String text;

    public SectionTitle(String text) { this.text = text.toUpperCase(); }

    @Override public int getHeight() { return 16; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        int ty = y + 6;
        ctx.drawText(tr(), text, x + 1, ty, Theme.TEXT_DIM, false);
        int lineX = x + tr().getWidth(text) + 7;
        if (lineX < x + width) ctx.fill(lineX, ty + 4, x + width, ty + 5, Theme.DIVIDER);
    }
}
