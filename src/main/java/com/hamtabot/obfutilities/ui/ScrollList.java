package com.hamtabot.obfutilities.ui;

import com.hamtabot.obfutilities.ui.component.Component;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class ScrollList {

    private static final int SCROLLBAR_W = 3;
    private static final int SCROLL_STEP = 24;

    private final List<Component> components = new ArrayList<>();
    private final Anim scroll = new Anim(0f);
    private float target = 0f;
    private int x, y, width, height;
    private int contentHeight;
    private boolean draggingBar = false;
    private double dragStartY, dragStartScroll;

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setComponents(List<Component> list, boolean keepScroll) {
        components.clear();
        components.addAll(list);
        if (!keepScroll) { target = 0f; scroll.set(0f); }
    }

    public boolean isEmpty() { return components.isEmpty(); }

    public void reveal(Component c) {
        layout(0);
        int top = c.getY() - y, bottom = top + c.getHeight();
        if (bottom > target + height) target = bottom - height;
        else if (top < target) target = top;
        target = Math.max(0, Math.min(target, maxScroll()));
        scroll.set(target);
    }

    private int innerWidth() { return width - SCROLLBAR_W - 4; }

    private int maxScroll() { return Math.max(0, contentHeight - height); }

    private void layout(int offset) {
        int cy = y - offset;
        for (Component c : components) {
            c.setBounds(x, cy, innerWidth());
            cy += c.getHeight() + Theme.ROW_GAP;
        }
        contentHeight = cy - (y - offset) - Theme.ROW_GAP;
    }

    public void render(DrawContext ctx, int mouseX, int mouseY) {
        layout(0);
        target = Math.max(0, Math.min(target, maxScroll()));
        int offset = Math.round(scroll.update(target, 20f));
        layout(offset);

        boolean inside = Draw.inside(mouseX, mouseY, x, y, width, height);
        int mx = inside ? mouseX : -1, my = inside ? mouseY : -1;

        ctx.enableScissor(x, y, x + width, y + height);
        for (Component c : components) {
            int h = c.getHeight();
            if (c.getY() + h < y || c.getY() > y + height) continue;
            c.render(ctx, mx, my);
        }
        ctx.disableScissor();

        if (maxScroll() > 0) renderScrollbar(ctx, offset, mouseX, mouseY);
    }

    private int[] thumb(int offset) {
        int thumbH = Math.max(16, height * height / Math.max(1, contentHeight));
        int thumbY = y + Math.round((height - thumbH) * (offset / (float) maxScroll()));
        return new int[]{ thumbY, thumbH };
    }

    private void renderScrollbar(DrawContext ctx, int offset, int mouseX, int mouseY) {
        int bx = x + width - SCROLLBAR_W;
        int[] t = thumb(offset);
        boolean hov = draggingBar || Draw.inside(mouseX, mouseY, bx - 3, y, SCROLLBAR_W + 3, height);
        Draw.round(ctx, bx, y, SCROLLBAR_W, height, 0x14FFFFFF);
        Draw.round(ctx, bx, t[0], SCROLLBAR_W, t[1], hov ? Theme.ACCENT : 0xFF4A5160);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!Draw.inside(mx, my, x, y, width, height)) {
            for (Component c : components) c.mouseClicked(-1, -1, button);
            return false;
        }
        if (maxScroll() > 0 && button == 0 && mx >= x + width - SCROLLBAR_W - 3) {
            draggingBar = true;
            dragStartY = my;
            dragStartScroll = target;
            return true;
        }
        boolean handled = false;
        for (Component c : new ArrayList<>(components)) {
            if (c.mouseClicked(mx, my, button)) handled = true;
        }
        return handled;
    }

    public boolean mouseDragged(double mx, double my) {
        if (!draggingBar) return false;
        int thumbH = thumb(0)[1];
        float ratio = maxScroll() / (float) Math.max(1, height - thumbH);
        target = (float) Math.max(0, Math.min(maxScroll(), dragStartScroll + (my - dragStartY) * ratio));
        scroll.set(target);
        return true;
    }

    public void mouseReleased() { draggingBar = false; }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!Draw.inside(mx, my, x, y, width, height)) return false;
        target = (float) Math.max(0, Math.min(maxScroll(), target - amount * SCROLL_STEP));
        return true;
    }

    public boolean keyPressed(int keyCode) {
        for (Component c : components) if (c.keyPressed(keyCode)) return true;
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        for (Component c : components) if (c.charTyped(chr, modifiers)) return true;
        return false;
    }

    public boolean isCapturingKeys() {
        for (Component c : components) if (c.isCapturingKeys()) return true;
        return false;
    }
}
