package com.hamtabot.obfutilities.ui;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.debug.DebugOverlay;
import com.hamtabot.obfutilities.hud.InventoryHud;
import com.hamtabot.obfutilities.hud.OBFHud;
import com.hamtabot.obfutilities.timer.TimerEntry;
import com.hamtabot.obfutilities.timer.Timers;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {

    private static final int SNAP = 5;
    private static final int GUIDE_COLOR = 0x884FC3F7;

    private final Screen parent;
    private final OBFHud hud = OBFUtilities.getHud();

    private DebugOverlay.Element draggedOverlay = null;
    private int draggedTimer = -1, resizingTimer = -1;
    private boolean draggingHud = false, resizingHud = false;
    private boolean draggingInv = false, resizingInv = false;
    private int dragOffX, dragOffY;
    private double resizeStartX;
    private float resizeStartScale;
    private int guideX = -1, guideY = -1;

    public HudEditScreen(Screen parent) {
        super(Text.literal("Positionner le HUD"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x60000000);

        ctx.fill(width / 2, 0, width / 2 + 1, height, 0x14FFFFFF);
        ctx.fill(0, height / 2, width, height / 2 + 1, 0x14FFFFFF);
        if (guideX >= 0) ctx.fill(guideX, 0, guideX + 1, height, GUIDE_COLOR);
        if (guideY >= 0) ctx.fill(0, guideY, width, guideY + 1, GUIDE_COLOR);

        InventoryHud.render(ctx, true);
        hud.render(ctx, true);
        if (draggingHud || resizingHud || (draggedOverlay == null && hud.isMouseOver(mouseX, mouseY))) {
            ctx.drawBorder(hud.getPosX() - 1, hud.getPosY() - 1, hud.getWidth() + 2, hud.getHeight() + 2, Theme.ACCENT);
        }
        DebugOverlay.render(ctx, true);
        Timers.render(ctx, true);

        renderToolbar(ctx, mouseX, mouseY);

        float scale = resizingHud ? OBFUtilities.config.scaleHud : resizingInv ? OBFUtilities.config.invHudScale : resizingTimer >= 0 ? Timers.all().get(resizingTimer).scale : -1;
        if (scale > 0) ctx.drawTooltip(textRenderer, Text.literal(Math.round(scale * 100) + " %"), mouseX, mouseY);
    }

    private static final String HINT = "Glisse pour déplacer · coin en bas à droite ou molette pour la taille";

    private int[] toolbar() {
        int w = Math.min(width - 12, Math.max(textRenderer.getWidth(HINT) + 20, 260));
        int h = 44;
        return new int[]{ (width - w) / 2, height - h - 8, w, h };
    }

    private int[] toolbarButton(int index) {
        int[] t = toolbar();
        int bw = (t[2] - 30) / 2;
        int bx = t[0] + 10 + index * (bw + 10);
        return new int[]{ bx, t[1] + t[3] - Theme.BUTTON_H - 7, bw, Theme.BUTTON_H };
    }

    private void renderToolbar(DrawContext ctx, int mouseX, int mouseY) {
        int[] t = toolbar();
        Draw.shadow(ctx, t[0], t[1], t[2], t[3]);
        Draw.card(ctx, t[0], t[1], t[2], t[3], Theme.WINDOW, Theme.WINDOW_BORDER);
        String hint = Draw.ellipsize(textRenderer, HINT, t[2] - 16);
        ctx.drawText(textRenderer, hint, t[0] + (t[2] - textRenderer.getWidth(hint)) / 2, t[1] + 7, Theme.TEXT_MUTED, false);

        int[] r = toolbarButton(0), d = toolbarButton(1);
        Draw.button(ctx, textRenderer, r[0], r[1], r[2], r[3], "Réinitialiser", Draw.ButtonStyle.SECONDARY,
                Draw.inside(mouseX, mouseY, r[0], r[1], r[2], r[3]), true);
        Draw.button(ctx, textRenderer, d[0], d[1], d[2], d[3], "Terminé", Draw.ButtonStyle.PRIMARY,
                Draw.inside(mouseX, mouseY, d[0], d[1], d[2], d[3]), true);
    }

    private void resetPositions() {
        ModConfig cfg = OBFUtilities.config;
        cfg.scaleHud = 1.0f;
        hud.setPos(10, 10);
        cfg.invHudScale = 1.0f;
        InventoryHud.setPos(10, 60);
        DebugOverlay.setPos(DebugOverlay.Element.FPS,    10, height - 80);
        DebugOverlay.setPos(DebugOverlay.Element.COORDS, 10, height - 62);
        DebugOverlay.setPos(DebugOverlay.Element.RAM,    10, height - 44);
        for (int i = 0; i < Timers.all().size(); i++) {
            TimerEntry t = Timers.all().get(i);
            t.scale = 1f;
            int[] b = Timers.bounds(i);
            t.pos.set(width - 10 - b[2], 10 + i * (Timers.H + 4), b[2], b[3]);
        }
    }

    private int timerAt(double mx, double my) {
        for (int i = Timers.all().size() - 1; i >= 0; i--) {
            if (!Timers.all().get(i).showOnScreen) continue;
            int[] b = Timers.bounds(i);
            if (Draw.inside(mx, my, b[0], b[1], b[2], b[3])) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int[] r = toolbarButton(0), d = toolbarButton(1);
        if (Draw.inside(mouseX, mouseY, r[0], r[1], r[2], r[3])) { resetPositions(); return true; }
        if (Draw.inside(mouseX, mouseY, d[0], d[1], d[2], d[3])) { close(); return true; }

        int timer = timerAt(mouseX, mouseY);
        if (timer >= 0) {
            if (Timers.isInResizeGrip(timer, mouseX, mouseY)) {
                resizingTimer = timer;
                resizeStartX = mouseX;
                resizeStartScale = Timers.all().get(timer).scale;
            } else {
                int[] b = Timers.bounds(timer);
                draggedTimer = timer;
                dragOffX = (int) mouseX - b[0];
                dragOffY = (int) mouseY - b[1];
            }
            return true;
        }
        for (DebugOverlay.Element e : DebugOverlay.Element.values()) {
            if (!DebugOverlay.isEnabled(e)) continue;
            int[] b = DebugOverlay.bounds(e, textRenderer);
            if (Draw.inside(mouseX, mouseY, b[0], b[1], b[2], b[3])) {
                draggedOverlay = e;
                dragOffX = (int) mouseX - b[0];
                dragOffY = (int) mouseY - b[1];
                return true;
            }
        }
        if (InventoryHud.isInResizeGrip(mouseX, mouseY)) {
            resizingInv = true;
            resizeStartX = mouseX;
            resizeStartScale = OBFUtilities.config.invHudScale;
            return true;
        }
        if (hud.isInResizeGrip(mouseX, mouseY)) {
            resizingHud = true;
            resizeStartX = mouseX;
            resizeStartScale = OBFUtilities.config.scaleHud;
            return true;
        }
        if (hud.isMouseOver(mouseX, mouseY)) {
            draggingHud = true;
            dragOffX = (int) mouseX - hud.getPosX();
            dragOffY = (int) mouseY - hud.getPosY();
            return true;
        }
        if (InventoryHud.isMouseOver(mouseX, mouseY)) {
            int[] b = InventoryHud.bounds();
            draggingInv = true;
            dragOffX = (int) mouseX - b[0];
            dragOffY = (int) mouseY - b[1];
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (resizingHud) {
            OBFUtilities.config.scaleHud = ModConfig.clampScale(resizeStartScale + (float) (mouseX - resizeStartX) / 200f);
            return true;
        }
        if (resizingInv) {
            InventoryHud.setScale(resizeStartScale + (float) (mouseX - resizeStartX) / 200f);
            return true;
        }
        if (draggingInv) {
            int[] b = InventoryHud.bounds();
            int[] p = snap((int) mouseX - dragOffX, (int) mouseY - dragOffY, b[2], b[3]);
            InventoryHud.setPos(p[0], p[1]);
            return true;
        }
        if (resizingTimer >= 0) {
            Timers.all().get(resizingTimer).setScale(resizeStartScale + (float) (mouseX - resizeStartX) / 100f);
            return true;
        }
        if (draggedTimer >= 0) {
            int[] b = Timers.bounds(draggedTimer);
            int[] p = snap((int) mouseX - dragOffX, (int) mouseY - dragOffY, b[2], b[3]);
            TimerEntry t = Timers.all().get(draggedTimer);
            t.pos.set(p[0], p[1], b[2], b[3]);
            return true;
        }
        if (draggingHud) {
            int[] p = snap((int) mouseX - dragOffX, (int) mouseY - dragOffY, hud.getWidth(), hud.getHeight());
            hud.setPos(p[0], p[1]);
            return true;
        }
        if (draggedOverlay != null) {
            int[] b = DebugOverlay.bounds(draggedOverlay, textRenderer);
            int[] p = snap((int) mouseX - dragOffX, (int) mouseY - dragOffY, b[2], b[3]);
            DebugOverlay.setPos(draggedOverlay, p[0], p[1]);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    private int[] snap(int x, int y, int w, int h) {
        guideX = -1;
        guideY = -1;
        x = Math.max(0, Math.min(x, width - w));
        y = Math.max(0, Math.min(y, height - h));

        int cx = width / 2, cy = height / 2;
        if (Math.abs(x + w / 2 - cx) <= SNAP) { x = cx - w / 2; guideX = cx; }
        else if (x <= SNAP) x = 0;
        else if (width - (x + w) <= SNAP) x = width - w;

        if (Math.abs(y + h / 2 - cy) <= SNAP) { y = cy - h / 2; guideY = cy; }
        else if (y <= SNAP) y = 0;
        else if (height - (y + h) <= SNAP) y = height - h;

        return new int[]{ x, y };
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingHud = resizingHud = false;
        draggingInv = resizingInv = false;
        draggedOverlay = null;
        draggedTimer = resizingTimer = -1;
        guideX = guideY = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int timer = timerAt(mouseX, mouseY);
        if (timer < 0 && InventoryHud.isMouseOver(mouseX, mouseY)) {
            InventoryHud.setScale(OBFUtilities.config.invHudScale + (float) amount * 0.1f);
            return true;
        }
        if (timer < 0) return super.mouseScrolled(mouseX, mouseY, amount);
        TimerEntry t = Timers.all().get(timer);
        t.setScale(t.scale + (float) amount * 0.1f);
        return true;
    }

    @Override
    public void removed() {
        OBFUtilities.config.save();
        hud.savePosition();
    }

    @Override public void close() { client.setScreen(parent); }

    @Override public boolean shouldPause() { return false; }
}
