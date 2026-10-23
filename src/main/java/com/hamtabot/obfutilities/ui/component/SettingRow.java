package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Anim;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

import java.util.List;

public abstract class SettingRow extends Component {

    protected final String title;
    protected final String description;
    private final Anim hover = new Anim(0f);

    private List<OrderedText> wrapped;
    private int wrappedFor = -1;

    protected SettingRow(String title, String description) {
        this.title = title;
        this.description = description;
    }

    protected abstract int controlWidth();

    protected abstract void renderControl(DrawContext ctx, int cx, int cy, int mouseX, int mouseY);

    protected int controlHeight() { return Theme.BUTTON_H; }

    private int textWidth() { return width - Theme.ROW_PAD * 3 - controlWidth(); }

    private List<OrderedText> lines() {
        if (description == null || description.isEmpty()) return List.of();
        if (wrappedFor != textWidth()) {
            wrapped = Draw.wrap(tr(), description, textWidth());
            wrappedFor = textWidth();
        }
        return wrapped;
    }

    @Override
    public int getHeight() {
        int text = Theme.LINE_H + lines().size() * Theme.LINE_H;
        return Math.max(text, controlHeight()) + Theme.ROW_PAD * 2 - 2;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        int h = getHeight();
        float hv = hover.update(isHovered(mouseX, mouseY) ? 1f : 0f, 18f);
        Draw.card(ctx, x, y, width, h, Draw.lerpColor(Theme.SURFACE, Theme.SURFACE_HOVER, hv), Theme.BORDER);

        int tx = x + Theme.ROW_PAD;
        int ty = y + Theme.ROW_PAD;
        ctx.drawText(tr(), Draw.ellipsize(tr(), title, textWidth()), tx, ty, Theme.TEXT, false);
        Draw.drawWrapped(ctx, tr(), lines(), tx, ty + Theme.LINE_H + 1, Theme.TEXT_MUTED);

        int cx = x + width - Theme.ROW_PAD - controlWidth();
        int cy = y + (h - controlHeight()) / 2;
        renderControl(ctx, cx, cy, mouseX, mouseY);
    }

    protected boolean overControl(double mx, double my) {
        int h = getHeight();
        int cx = x + width - Theme.ROW_PAD - controlWidth();
        int cy = y + (h - controlHeight()) / 2;
        return Draw.inside(mx, my, cx, cy, controlWidth(), controlHeight());
    }
}
