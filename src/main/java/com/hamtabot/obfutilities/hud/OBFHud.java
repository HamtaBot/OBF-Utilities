package com.hamtabot.obfutilities.hud;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.StatsReader;
import com.hamtabot.obfutilities.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class OBFHud {

    private int posX = 10;
    private int posY = 10;
    private boolean visible = true;

    private static final int BASE_PANEL_WIDTH = 220;
    private static final int PADDING          = 8;

    private int s(int v) { return Math.round(v * OBFUtilities.config.scaleHud); }
    private int panelWidth() { return s(BASE_PANEL_WIDTH); }

    private int cachedTotalPlaced = -1;
    private int cachedTotalMined  = -1;
    private int cachedTotalKills  = -1;
    private final Map<Integer, Integer> cachedTotalCustom = new HashMap<>();
    private int tickCounter = 0;

    private double lastX = 0, lastY = 0, lastZ = 0;
    private long lastActivityTime = System.currentTimeMillis();
    private static final long AFK_THRESHOLD_MS = 30_000;

    private static final Path POS_FILE = Paths.get("obfutilities_hud_pos.properties");

    private static final int C_BG        = 0xCC0A0A0A;
    private static final int C_BORDER    = 0xFF4FC3F7;
    private static final int C_TITLE     = 0xFFFFD54F;
    private static final int C_SECTION_B = 0xFF4FC3F7;
    private static final int C_SECTION_M = 0xFF66BB6A;
    private static final int C_SECTION_K = 0xFFFF5252;
    private static final int C_SECTION_C = 0xFFCE93D8;
    private static final int C_SECTION_A = 0xFFCE93D8;
    private static final int C_LABEL     = 0xFFB0BEC5;
    private static final int C_VAL_B     = 0xFF00E5FF;
    private static final int C_VAL_M     = 0xFF66BB6A;
    private static final int C_VAL_K     = 0xFFFF5252;
    private static final int C_VAL_C     = 0xFFCE93D8;
    private static final int C_TOTAL     = 0xFFFFD54F;
    private static final int C_RATE      = 0xFF90A4AE;
    private static final int C_TIME      = 0xFF80CBC4;
    private static final int C_AD_OK     = 0xFF66BB6A;
    private static final int C_AD_WAIT   = 0xFFFF5252;

    public OBFHud() { loadPosition(); }

    public void render(DrawContext context, float tickDelta) {
        if (!visible) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer tr = client.textRenderer;
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int pw = panelWidth();
        int panelH = computePanelHeight();

        posX = Math.max(0, Math.min(posX, screenW - pw));
        posY = Math.max(0, Math.min(posY, screenH - panelH));

        context.fill(posX+3, posY+3, posX+pw+3, posY+panelH+3, 0x55000000);
        context.fill(posX, posY, posX+pw, posY+panelH, C_BG);
        context.fill(posX,      posY,           posX+pw, posY+1,         C_BORDER);
        context.fill(posX,      posY+panelH-1,  posX+pw, posY+panelH,    C_BORDER);
        context.fill(posX,      posY,           posX+1,  posY+panelH,    C_BORDER);
        context.fill(posX+pw-1, posY,           posX+pw, posY+panelH,    C_BORDER);
        context.fill(posX+1, posY+1, posX+s(8), posY+2, 0xFF81D4FA);
        context.fill(posX+1, posY+1, posX+2, posY+s(8), 0xFF81D4FA);

        for (int i = 0; i < 3; i++) {
            context.fill(posX+pw-s(16)+i*s(4), posY+s(5), posX+pw-s(14)+i*s(4), posY+s(7), 0xFF546E7A);
            context.fill(posX+pw-s(16)+i*s(4), posY+s(9), posX+pw-s(14)+i*s(4), posY+s(11), 0xFF546E7A);
        }

        drawResizeGrip(context, posX, posY, pw, panelH);

        int ty = posY + s(PADDING);

        String timeStr = formatSessionTime();
        context.drawTextWithShadow(tr, "§e◆ OBF Utilitaire", posX+s(PADDING), ty, C_TITLE);
        context.drawText(tr, "§7"+timeStr, posX+pw-s(PADDING)-tr.getWidth(timeStr), ty, C_TIME, false);
        ty += s(12);
        context.fill(posX+s(PADDING), ty, posX+pw-s(PADDING), ty+1, 0xFF1A3A4A);
        ty += s(6);

        String toggleKey = getBoundKeyName(OBFUtilities.keyToggleHud);
        String configKey = getBoundKeyName(OBFUtilities.keyOpenConfig);
        String hintText  = "["+toggleKey+"] cacher  ["+configKey+"] config";
        context.drawText(tr, hintText, posX+pw/2-tr.getWidth(hintText)/2, ty, 0xFF546E7A, false);
        ty += s(11);
        context.fill(posX+s(PADDING), ty, posX+pw-s(PADDING), ty+1, 0xFF1A2A2A);
        ty += s(6);

        if (OBFUtilities.config.showBlocksPlaced) {
            ty = drawSection(context, tr, ty, "§b◈ Blocs posés", C_SECTION_B,
                    OBFUtilities.getSessionBlocksPlaced(), OBFUtilities.config.serverTotalPlaced, "blocs",
                    OBFUtilities.getRateBlocksPlaced(), C_VAL_B);
        }
        if (OBFUtilities.config.showBlocksMined) {
            ty = drawSection(context, tr, ty, "§a⛏ Blocs minés", C_SECTION_M,
                    OBFUtilities.getSessionBlocksMined(), OBFUtilities.config.serverTotalMined, "blocs",
                    OBFUtilities.getRateBlocksMined(), C_VAL_M);
        }
        if (OBFUtilities.config.showMobsKilled) {
            ty = drawSection(context, tr, ty, "§c⚔ Mobs tués", C_SECTION_K,
                    OBFUtilities.getSessionKills(), OBFUtilities.config.serverTotalKills, "mobs",
                    OBFUtilities.getRateKills(), C_VAL_K);
        }

        List<ModConfig.CustomBlockEntry> customs = OBFUtilities.config.customBlocks;
        for (int i = 0; i < customs.size(); i++) {
            ModConfig.CustomBlockEntry e = customs.get(i);
            if (!e.enabled) continue;
            String mode  = e.trackPlaced ? "posé" : "miné";
            String label = "§d◉ " + formatBlockName(e.blockId) + " ("+mode+")";
            ty = drawSection(context, tr, ty, label, C_SECTION_C,
                    OBFUtilities.getSessionCustom(i), cachedTotalCustom.getOrDefault(i, -1), "blocs",
                    OBFUtilities.getRateCustom(i), C_VAL_C);
        }

        if (OBFUtilities.config.showAdTimer) {
            ty = drawAdSection(context, tr, ty);
        }

        renderConnectionNotice(context, tr, computePanelHeight());
    }

    private void drawResizeGrip(DrawContext ctx, int px, int py, int pw, int ph) {
        int gx = px+pw-8, gy = py+ph-8;
        ctx.fill(gx, gy, px+pw, py+ph, 0x55FFD54F);
        ctx.fill(gx+2, gy+6, gx+4, gy+8, 0xAAFFD54F);
        ctx.fill(gx+4, gy+4, gx+6, gy+6, 0xAAFFD54F);
        ctx.fill(gx+6, gy+2, gx+8, gy+4, 0xAAFFD54F);
    }

    private int drawSection(DrawContext ctx, TextRenderer tr, int ty,
                            String label, int labelColor,
                            int session, int total, String unit, float rate, int valueColor) {
        int pw = panelWidth();
        ctx.drawText(tr, label, posX+s(PADDING), ty, labelColor, false);
        ty += s(10);
        String sVal = formatNumber(session)+" "+unit;
        ctx.drawText(tr, "Session :", posX+s(PADDING)+s(4), ty, C_LABEL, false);
        ctx.drawText(tr, sVal, posX+pw-s(PADDING)-tr.getWidth(sVal), ty, valueColor, false);
        ty += s(10);
        String tVal = total >= 0 ? formatNumber(total)+" "+unit : "N/A";
        ctx.drawText(tr, "Total :", posX+s(PADDING)+s(4), ty, C_LABEL, false);
        ctx.drawText(tr, tVal, posX+pw-s(PADDING)-tr.getWidth(tVal), ty, C_TOTAL, false);
        ty += s(10);
        if (rate > 0f) {
            String rStr = String.format("%.1f/sec", rate);
            ctx.drawText(tr, rStr, posX+pw-s(PADDING)-tr.getWidth(rStr), ty, C_RATE, false);
            ty += s(10);
        }
        ctx.fill(posX+s(PADDING), ty, posX+pw-s(PADDING), ty+1, 0xFF1A2A2A);
        ty += s(6);
        return ty;
    }

    private int drawAdSection(DrawContext ctx, TextRenderer tr, int ty) {
        int pw = panelWidth();
        ctx.drawText(tr, "§d⏰ Temps prochaine pub", posX+s(PADDING), ty, C_SECTION_A, false);
        ty += s(10);
        if (OBFUtilities.isAdAvailable()) {
            String sv = "Pub disponible !";
            ctx.drawText(tr, sv, posX+pw-s(PADDING)-tr.getWidth(sv), ty, C_AD_OK, false);
        } else {
            long rem = Math.max(0, OBFUtilities.getAdCooldownMs()-(System.currentTimeMillis()-OBFUtilities.getLastAdTime()));
            String sv = formatAdTimer(rem);
            ctx.drawText(tr, sv, posX+pw-s(PADDING)-tr.getWidth(sv), ty, C_AD_WAIT, false);
        }
        ty += s(11);
        return ty;
    }

    private int computePanelHeight() {
        int h = s(PADDING) + s(12) + 1 + s(6) + s(11) + 1 + s(6);
        if (OBFUtilities.config.showBlocksPlaced) h += sectionH(OBFUtilities.getRateBlocksPlaced());
        if (OBFUtilities.config.showBlocksMined)  h += sectionH(OBFUtilities.getRateBlocksMined());
        if (OBFUtilities.config.showMobsKilled)   h += sectionH(OBFUtilities.getRateKills());
        for (int i = 0; i < OBFUtilities.config.customBlocks.size(); i++) {
            if (OBFUtilities.config.customBlocks.get(i).enabled)
                h += sectionH(OBFUtilities.getRateCustom(i));
        }
        if (OBFUtilities.config.showAdTimer) h += s(10) + s(11);
        h += s(PADDING);
        return h;
    }

    private int sectionH(float rate) { return s(10) + s(10) + s(10) + (rate > 0 ? s(10) : 0) + 1 + s(6); }

    private void renderConnectionNotice(DrawContext context, TextRenderer tr, int panelH) {
        long elapsed   = System.currentTimeMillis() - OBFUtilities.getConnectionTime();
        long remaining = OBFUtilities.getNoticeDurationMs() - elapsed;
        if (remaining <= 0) return;

        long secs = remaining / 1000;
        long mins = secs / 60;
        secs = secs % 60;
        String timer = mins > 0 ? mins + "m" + String.format("%02d", secs) + "s" : secs + "s";

        String[] lines = {
                "§cLes totaux sont calculés",
                "§cà la connexion uniquement.",
                "§8Disparaît dans " + timer
        };

        int noticeY = posY + panelH + s(4);
        for (String line : lines) {
            context.drawTextWithShadow(tr, line, posX + s(PADDING), noticeY, 0xFFFFFFFF);
            noticeY += s(10);
        }
    }

    public void tick(MinecraftClient client) {
        OBFUtilities.updateRates();

        if (client.player != null) {
            double cx=client.player.getX(), cy=client.player.getY(), cz=client.player.getZ();
            if (Math.abs(cx-lastX)>0.01||Math.abs(cy-lastY)>0.01||Math.abs(cz-lastZ)>0.01) {
                lastActivityTime=System.currentTimeMillis(); lastX=cx; lastY=cy; lastZ=cz;
            }
        }
        if (OBFUtilities.shouldRequestStats()) { lastActivityTime=System.currentTimeMillis(); OBFUtilities.clearActivityFlag(); }

        tickCounter++;
        if (tickCounter >= 40) {
            tickCounter = 0;
            boolean isAfk = System.currentTimeMillis()-lastActivityTime > AFK_THRESHOLD_MS;
            if (!isAfk) StatsReader.requestStatsUpdate();
            cachedTotalPlaced = StatsReader.getTotalBlocksPlaced();
            cachedTotalMined  = StatsReader.getTotalBlocksMined();
            cachedTotalKills  = StatsReader.getTotalMobsKilled();
            for (int i = 0; i < OBFUtilities.config.customBlocks.size(); i++) {
                ModConfig.CustomBlockEntry e = OBFUtilities.config.customBlocks.get(i);
                if (e.enabled) cachedTotalCustom.put(i, StatsReader.getTotalCustomBlock(e.blockId, e.trackPlaced));
            }
        }

        while (OBFUtilities.keyToggleHud.wasPressed()) visible = !visible;
        while (OBFUtilities.keyOpenConfig.wasPressed()) {
            if (client.currentScreen == null && visible) client.setScreen(new OBFConfigScreen(this));
        }
        while (OBFUtilities.keyAddWaypoint.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new com.hamtabot.obfutilities.waypoint.WaypointCreateScreen(null));
        }
        while (OBFUtilities.keyOpenWaypoints.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new OBFConfigScreen(this));
        }
    }



    public int getPosX()  { return posX; }
    public int getPosY()  { return posY; }
    public void setPos(int x, int y) { posX=x; posY=y; }

    public void savePosition() {
        Properties props = new Properties();
        props.setProperty("hud_x", String.valueOf(posX));
        props.setProperty("hud_y", String.valueOf(posY));
        try (OutputStream out = Files.newOutputStream(POS_FILE)) { props.store(out, "OBF HUD Position"); }
        catch (Exception e) { OBFUtilities.LOGGER.error("[OBF] "+e.getMessage()); }
    }

    private void loadPosition() {
        if (Files.exists(POS_FILE)) {
            try (InputStream in = Files.newInputStream(POS_FILE)) {
                Properties p = new Properties(); p.load(in);
                posX=Math.max(0, Math.min(Integer.parseInt(p.getProperty("hud_x","10")), 7680));
                posY=Math.max(0, Math.min(Integer.parseInt(p.getProperty("hud_y","10")), 4320));
            } catch (Exception e) { posX=10; posY=10; }
        }
    }

    public boolean isVisible()      { return visible; }
    public int getComputedPanelHeight() { return computePanelHeight(); }
    public void setVisible(boolean v) { this.visible=v; }

    private String formatNumber(int n)     { return String.format("%,d", n).replace(',', '.'); }
    private String formatAdTimer(long ms)  { return String.format("%02dm %02ds", (ms/60000)%60, (ms/1000)%60); }
    private String formatSessionTime() {
        long e = System.currentTimeMillis() - OBFUtilities.getSessionStartTime();
        long sv = (e/1000)%60, m = (e/60000)%60, h = e/3600000;
        return h > 0 ? String.format("%dh%02dm%02ds",h,m,sv) : String.format("%dm%02ds",m,sv);
    }
    private String formatBlockName(String id) {
        if (id == null || id.isEmpty()) return "Unknown";
        String n = id.contains(":") ? id.split(":", 2)[1] : id;
        StringBuilder sb = new StringBuilder();
        for (String p : n.split("_")) if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        return sb.toString().trim();
    }
    private String getBoundKeyName(net.minecraft.client.option.KeyBinding kb) {
        return kb == null ? "?" : kb.getBoundKeyLocalizedText().getString();
    }
}