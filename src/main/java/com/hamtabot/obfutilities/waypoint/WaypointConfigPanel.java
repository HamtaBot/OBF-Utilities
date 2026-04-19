package com.hamtabot.obfutilities.waypoint;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public class WaypointConfigPanel extends Screen {

    public static final int PANEL_W = 300;
    private static final int ROW_H   = 36;
    private static final int VISIBLE = 5;
    private static final int HEADER_H = 20;

    public static int panelX = -1, panelY = 20;
    private boolean dragging = false;
    private int dragOffX, dragOffY;

    private static int scrollOffset = 0;
    private static int confirmDeleteIndex = -1;
    private static boolean showShareAll = false;
    private static String shareTarget = "";

    private TextFieldWidget shareTargetField;
    private final Screen parent;

    public WaypointConfigPanel(Screen parent) {
        super(Text.literal("Waypoints"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (panelX == -1) { panelX = Math.max(10, this.width - PANEL_W - 10); panelY = 20; }
        panelX = Math.max(0, Math.min(panelX, this.width - PANEL_W));
        panelY = Math.max(0, Math.min(panelY, this.height - computeHeight()));

        int bx = panelX + 10, bw = PANEL_W - 20;
        int by = panelY + HEADER_H + 10;

        boolean[] stWp = { OBFUtilities.config.waypointsEnabled };
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Waypoints : " + (stWp[0] ? "§aON" : "§cOFF")),
                btn -> { stWp[0]=!stWp[0]; OBFUtilities.config.waypointsEnabled=stWp[0]; btn.setMessage(Text.literal("Waypoints : "+(stWp[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, 18).build());
        by += 24;

        String keyName = OBFUtilities.keyAddWaypoint != null ? OBFUtilities.keyAddWaypoint.getBoundKeyLocalizedText().getString() : "N";
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§a+ Créer un waypoint  [" + keyName + "]"),
                btn -> MinecraftClient.getInstance().setScreen(new WaypointCreateScreen(() -> MinecraftClient.getInstance().setScreen(this)))
        ).dimensions(bx, by, bw, 18).build());
        by += 26;

        List<Waypoint> all = WaypointManager.getAll();
        int maxScroll = Math.max(0, all.size() - VISIBLE);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int i = 0; i < VISIBLE && i + scrollOffset < all.size(); i++) {
            final int idx = i + scrollOffset;
            Waypoint wp = all.get(idx);
            int rowY = by + i * ROW_H;

            int btnW = (bw - 4) / 3;

            addDrawableChild(ButtonWidget.builder(
                    Text.literal(wp.enabled ? "§aVisible" : "§8Masqué"),
                    btn -> { wp.enabled = !wp.enabled; WaypointManager.save(); btn.setMessage(Text.literal(wp.enabled ? "§aVisible" : "§8Masqué")); }
            ).dimensions(bx, rowY + 16, btnW, 16).build());

            addDrawableChild(ButtonWidget.builder(
                    Text.literal("§b⇗ Partager"),
                    btn -> shareWaypoint(wp)
            ).dimensions(bx + btnW + 2, rowY + 16, btnW, 16).build());

            if (confirmDeleteIndex == idx) {
                addDrawableChild(ButtonWidget.builder(Text.literal("§cConfirmer ?"),
                        btn -> { WaypointManager.remove(idx); confirmDeleteIndex = -1; scrollOffset = Math.max(0, scrollOffset-1); clearAndInit(); }
                ).dimensions(bx + (btnW+2)*2, rowY + 16, btnW, 16).build());
            } else {
                addDrawableChild(ButtonWidget.builder(Text.literal("§c✗ Supprimer"),
                        btn -> { confirmDeleteIndex = idx; clearAndInit(); }
                ).dimensions(bx + (btnW+2)*2, rowY + 16, btnW, 16).build());
            }
        }
        by += VISIBLE * ROW_H + 6;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("§b⇗ Partager tous (dimension courante)"),
                btn -> { showShareAll = !showShareAll; clearAndInit(); }
        ).dimensions(bx, by, bw, 18).build());
        by += 24;

        if (showShareAll) {
            shareTargetField = new TextFieldWidget(textRenderer, bx, by, bw, 16, Text.literal(""));
            shareTargetField.setPlaceholder(Text.literal("Pseudo du destinataire..."));
            shareTargetField.setText(shareTarget);
            shareTargetField.setChangedListener(s -> shareTarget = s);
            addDrawableChild(shareTargetField);
            by += 22;
            addDrawableChild(ButtonWidget.builder(Text.literal("§bEnvoyer tous"),
                    btn -> shareAllWaypoints(shareTarget.trim())
            ).dimensions(bx, by, bw, 18).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelH = computeHeight();
        panelX = Math.max(0, Math.min(panelX, this.width - PANEL_W));
        panelY = Math.max(0, Math.min(panelY, this.height - panelH));

        context.fill(panelX, panelY, panelX+PANEL_W, panelY+panelH, 0xEE0D0D0D);
        context.fill(panelX, panelY, panelX+PANEL_W, panelY+1, 0xFFFFD54F);
        context.fill(panelX, panelY+panelH-1, panelX+PANEL_W, panelY+panelH, 0xFFFFD54F);
        context.fill(panelX, panelY, panelX+1, panelY+panelH, 0xFFFFD54F);
        context.fill(panelX+PANEL_W-1, panelY, panelX+PANEL_W, panelY+panelH, 0xFFFFD54F);
        context.fill(panelX+1, panelY+1, panelX+PANEL_W-1, panelY+HEADER_H, 0xFF1A1A1A);
        context.drawTextWithShadow(textRenderer, "§e◆ Waypoints  §7— glisser pour déplacer", panelX+8, panelY+6, 0xFFFFD54F);

        int by = panelY + HEADER_H + 10 + 18 + 24 + 18 + 26;
        List<Waypoint> all = WaypointManager.getAll();

        if (all.isEmpty()) {
            context.drawText(textRenderer, "§7Aucun waypoint créé.", panelX+10, by+10, 0xFFB0BEC5, false);
        }

        for (int i = 0; i < VISIBLE && i + scrollOffset < all.size(); i++) {
            int idx = i + scrollOffset;
            Waypoint wp = all.get(idx);
            int rowY = by + i * ROW_H;
            boolean currentDim = WaypointManager.getCurrentDimension().equals(wp.dimension);

            context.fill(panelX+10, rowY, panelX+PANEL_W-10, rowY+ROW_H-2, currentDim ? 0x221A3A4A : 0x11FFFFFF);

            context.fill(panelX+14, rowY+4, panelX+24, rowY+14, wp.color);

            context.drawTextWithShadow(textRenderer, "§f" + wp.name, panelX+28, rowY+3, wp.color);
            context.drawText(textRenderer, "§7" + wp.x+", "+wp.y+", "+wp.z+"  §8["+wp.getDimShort()+"]", panelX+28, rowY+14, 0xFF888888, false);

            if (confirmDeleteIndex == idx) {
                context.drawText(textRenderer, "§cConfirmer la suppression ?", panelX+28, rowY+ROW_H-18+3, 0xFFFF5252, false);
            }
        }

        int listStart = by;
        int listH = VISIBLE * ROW_H;
        if (all.size() > VISIBLE) {
            int sbH = Math.max(16, listH * VISIBLE / all.size());
            int maxScroll = all.size() - VISIBLE;
            int sbTrack = listH - sbH;
            int sbY = listStart + (maxScroll > 0 ? scrollOffset * sbTrack / maxScroll : 0);
            context.fill(panelX+PANEL_W-8, listStart, panelX+PANEL_W-2, listStart+listH, 0xFF1A1A1A);
            context.fill(panelX+PANEL_W-8, sbY, panelX+PANEL_W-2, sbY+sbH, 0xFF4FC3F7);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= panelX && mouseX <= panelX+PANEL_W
                && mouseY >= panelY && mouseY <= panelY+HEADER_H) {
            dragging = true; dragOffX=(int)mouseX-panelX; dragOffY=(int)mouseY-panelY; return true;
        }
        int by = panelY + HEADER_H + 10 + 18 + 24 + 18 + 26;
        int listH = VISIBLE * ROW_H;
        List<Waypoint> all = WaypointManager.getAll();
        if (all.size() > VISIBLE && mouseX >= panelX+PANEL_W-8 && mouseX <= panelX+PANEL_W-2
                && mouseY >= by && mouseY < by+listH) {
            int maxScroll = all.size() - VISIBLE;
            scrollOffset = Math.max(0, Math.min((int)(((mouseY-by)/(double)listH)*all.size()), maxScroll));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && button == 0) {
            panelX = Math.max(0, Math.min((int)mouseX-dragOffX, this.width-PANEL_W));
            panelY = Math.max(0, Math.min((int)mouseY-dragOffY, this.height-computeHeight()));
            OBFUtilities.config.cfgWpX = panelX; OBFUtilities.config.cfgWpY = panelY;
            clearAndInit(); return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false; return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int by = panelY + HEADER_H + 10 + 18 + 24 + 18 + 26;
        int listH = VISIBLE * ROW_H;
        if (mouseX >= panelX && mouseX <= panelX+PANEL_W && mouseY >= by && mouseY < by+listH) {
            scrollOffset = Math.max(0, Math.min(scrollOffset-(int)amount, Math.max(0, WaypointManager.getAll().size()-VISIBLE)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void close() { OBFUtilities.config.save(); MinecraftClient.getInstance().setScreen(parent); }

    @Override
    public boolean shouldPause() { return false; }

    private void shareWaypoint(Waypoint wp) {
        if (shareTarget.trim().isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        client.getNetworkHandler().sendChatCommand("msg " + shareTarget.trim() + " [OBF-WP:" + wp.toShareString() + "]");
    }

    private void shareAllWaypoints(String target) {
        if (target.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        List<Waypoint> dimWps = WaypointManager.getForCurrentDimension();
        if (dimWps.isEmpty()) return;
        StringBuilder current = new StringBuilder("[OBF-WPS:");
        for (Waypoint wp : dimWps) {
            String part = wp.toShareString();
            if (current.length() + part.length() + 2 > 230) {
                client.getNetworkHandler().sendChatCommand("msg " + target + " " + current + "]");
                current = new StringBuilder("[OBF-WPS:");
            }
            if (current.length() > 9) current.append(";");
            current.append(part);
        }
        if (current.length() > 9) client.getNetworkHandler().sendChatCommand("msg " + target + " " + current + "]");
    }

    public static int computeHeight() {
        int h = HEADER_H + 10 + 18 + 24 + 18 + 26 + VISIBLE * ROW_H + 6 + 18 + 24;
        if (showShareAll) h += 22 + 18 + 6;
        return h + 14;
    }
}