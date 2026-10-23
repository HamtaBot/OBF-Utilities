package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

import java.util.List;

public class InfoBox extends Component {

    private final String text;
    private final int color;
    private List<OrderedText> lines;
    private int linesFor = -1;

    public InfoBox(String text, int color) {
        this.text = text;
        this.color = color;
    }

    private List<OrderedText> lines() {
        int w = width - Theme.ROW_PAD * 2 - 3;
        if (linesFor != w) { lines = Draw.wrap(tr(), text, w); linesFor = w; }
        return lines;
    }

    @Override public int getHeight() { return lines().size() * Theme.LINE_H + Theme.ROW_PAD * 2 - 2; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        int h = getHeight();
        Draw.tintedCard(ctx, x, y, width, h, color);
        Draw.drawWrapped(ctx, tr(), lines(), x + Theme.ROW_PAD + 3, y + Theme.ROW_PAD, Draw.lerpColor(color, Theme.TEXT, 0.55f));
    }
}
