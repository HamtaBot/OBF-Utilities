package com.hamtabot.obfutilities.hud;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.input.Keybinds;
import com.hamtabot.obfutilities.stats.AdTimer;
import com.hamtabot.obfutilities.stats.AfkTimer;
import com.hamtabot.obfutilities.stats.ServerStats;
import com.hamtabot.obfutilities.stats.SessionStats;
import com.hamtabot.obfutilities.stats.StatCounter;
import com.hamtabot.obfutilities.stats.StatsReader;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.HudPos;
import com.hamtabot.obfutilities.ui.Theme;
import com.hamtabot.obfutilities.util.Format;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class OBFHud {

    private static final int BASE_PANEL_WIDTH = 220;
    private static final int PADDING          = 6;
    private static final int HEADER_H         = 20;
    private static final int LOGO_SIZE        = 14;
    private static final int CARD_GAP         = 4;
    private static final int STAT_CARD_H      = 38;
    private static final int AD_CARD_H        = 16;

    private static final int ICON = 10, ICON_W = ICON + 3;
    private static final ItemStack ICON_PLACED = new ItemStack(Items.BRICKS);
    private static final ItemStack ICON_MINED  = new ItemStack(Items.IRON_PICKAXE);
    private static final ItemStack ICON_KILLS  = new ItemStack(Items.IRON_SWORD);
    private static final ItemStack ICON_AD     = new ItemStack(Items.PAPER);
    private static final ItemStack ICON_AFK    = new ItemStack(Items.RED_BED);

    private final Map<String, ItemStack> customIcons = new HashMap<>();
    // items and the logo flush the buffer themselves, so they are drawn after the batched fills/text
    private final List<Runnable> deferred = new ArrayList<>();

    private static final Path POS_FILE =Paths.get("obfutilities_hud_pos.properties");

    private int posX = 10;
    private int posY = 10;
    private final HudPos pos = new HudPos();
    private boolean visible = true;

    public OBFHud() { loadPosition(); }

    private int s(int v) { return Math.round(v * OBFUtilities.config.scaleHud); }

    public int getWidth() { return s(BASE_PANEL_WIDTH); }

    public void render(DrawContext ctx, boolean editing) {
        if (!visible && !editing) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ModConfig cfg = OBFUtilities.config;
        TextRenderer tr = client.textRenderer;
        int pw = getWidth();
        int ph = getHeight();

        int[] p = pos.resolve(pw, ph, posX, posY);
        posX = p[0];
        posY = p[1];

        deferred.clear();
        ctx.draw(() -> renderPanel(ctx, tr, cfg, pw, ph, editing));
        ctx.draw(() -> deferred.forEach(Runnable::run));
    }

    private void renderPanel(DrawContext ctx, TextRenderer tr, ModConfig cfg, int pw, int ph, boolean editing) {
        Draw.shadow(ctx, posX, posY, pw, ph);
        Draw.card(ctx, posX, posY, pw, ph, Theme.WINDOW, Theme.WINDOW_BORDER);
        drawHeader(ctx, tr, pw);
        if (editing) Draw.resizeGrip(ctx, posX + pw, posY + ph);

        int ty = posY + s(HEADER_H) + 1 + s(5);
        String hint = "[" + Format.keyName(Keybinds.toggleHud) + "] cacher  ·  [" + Format.keyName(Keybinds.openConfig) + "] config";
        hint = Draw.ellipsize(tr, hint, pw - s(PADDING) * 2);
        ctx.drawText(tr, hint, posX + (pw - tr.getWidth(hint)) / 2, ty, Theme.TEXT_DIM, false);
        ty += s(10) + s(4);

        if (cfg.showBlocksPlaced) ty = drawStatCard(ctx, tr, ty, ICON_PLACED, "Blocs posés", Theme.ACCENT,  SessionStats.PLACED, cfg.serverTotalPlaced, "blocs");
        if (cfg.showBlocksMined)  ty = drawStatCard(ctx, tr, ty, ICON_MINED,  "Blocs minés", Theme.SUCCESS, SessionStats.MINED,  cfg.serverTotalMined,  "blocs");
        if (cfg.showMobsKilled)   ty = drawStatCard(ctx, tr, ty, ICON_KILLS,  "Mobs tués",   Theme.DANGER,  SessionStats.KILLS,  cfg.serverTotalKills,  "mobs");

        for (ModConfig.CustomBlockEntry e : cfg.customBlocks) {
            if (!e.enabled) continue;
            String label = Format.blockName(e.blockId) + " (" + (e.trackPlaced ? "posé" : "miné") + ")";
            ItemStack icon = customIcons.computeIfAbsent(e.blockId, id -> Format.icon(id, true));
            ty = drawStatCard(ctx, tr, ty, icon, label, Theme.PURPLE, SessionStats.custom(e), StatsReader.getCustomTotal(e), "blocs");
        }

        if (cfg.showAdTimer)  ty = drawAdCard(ctx, tr, ty);
        if (cfg.showAfkTimer) drawAfkCard(ctx, tr, ty);

        renderConnectionNotice(ctx, tr, ph);
    }

    private void drawHeader(DrawContext ctx, TextRenderer tr, int pw) {
        int hh = s(HEADER_H);
        Draw.round(ctx, posX + 1, posY + 1, pw - 2, hh, Theme.HEADER);
        Draw.accentLine(ctx, posX + 1, posY + hh, pw - 2);

        int bx0 = posX + s(PADDING), by0 = posY;
        deferred.add(() -> Draw.brand(ctx, tr, bx0, by0, hh, s(LOGO_SIZE), s(5)));

        int textY = posY + (hh - 8) / 2 + 1;
        String time = Format.duration(System.currentTimeMillis() - SessionStats.getSessionStartTime());
        int tw = tr.getWidth(time);
        int bx = posX + pw - s(PADDING) - tw - 8;
        Draw.card(ctx, bx, textY - 3, tw + 8, 13, Theme.ACCENT_SURFACE, Theme.ACCENT_LINE);
        ctx.drawText(tr, time, bx + 4, textY, Theme.ACCENT, false);
    }

    private int drawStatCard(DrawContext ctx, TextRenderer tr, int ty, ItemStack icon, String label, int color,
                             StatCounter counter, int total, String unit) {
        int left = posX + s(PADDING), w = getWidth() - s(PADDING) * 2, right = left + w - s(6);
        int h = s(STAT_CARD_H);
        Draw.card(ctx, left, ty, w, h, Theme.SURFACE, Theme.BORDER);
        ctx.fill(left + 1, ty + s(4), left + 3, ty + h - s(4), color);

        int tx = left + s(8), y = ty + s(4);
        String rate = counter.getRate() > 0f ? String.format("%.1f/s", counter.getRate()) : null;
        int rateW = rate != null ? tr.getWidth(rate) + s(6) : 0;
        int iy = y - 1;
        deferred.add(() -> Draw.item(ctx, icon, tx, iy, ICON));
        ctx.drawText(tr, Draw.ellipsize(tr, label, right - tx - ICON_W - rateW), tx + ICON_W, y, color, false);
        if (rate != null) ctx.drawText(tr, rate, right - tr.getWidth(rate), y, Theme.ACCENT, false);
        y += s(11);

        String sVal = Format.number(counter.getSession()) + " " + unit;
        ctx.drawText(tr, "Session", tx, y, Theme.TEXT_MUTED, false);
        ctx.drawText(tr, sVal, right - tr.getWidth(sVal), y, color, false);
        y += s(10);

        String tVal = total >= 0 ? Format.number(total) + " " + unit : "N/A";
        ctx.drawText(tr, "Total", tx, y, Theme.TEXT_MUTED, false);
        ctx.drawText(tr, tVal, right - tr.getWidth(tVal), y, total >= 0 ? Theme.TEXT : Theme.TEXT_DIM, false);

        return ty + h + s(CARD_GAP);
    }

    private int drawAdCard(DrawContext ctx, TextRenderer tr, int ty) {
        int left = posX + s(PADDING), w = getWidth() - s(PADDING) * 2, right = left + w - s(6);
        int h = s(AD_CARD_H);
        boolean ok = AdTimer.isAvailable();
        Draw.card(ctx, left, ty, w, h, Theme.SURFACE, Theme.BORDER);
        ctx.fill(left + 1, ty + s(3), left + 3, ty + h - s(3), Theme.PURPLE);

        int y = ty + (h - 8) / 2 + 1;
        int iy = y - 1;
        deferred.add(() -> Draw.item(ctx, ICON_AD, left + s(8), iy, ICON));
        ctx.drawText(tr, "Prochaine pub", left + s(8) + ICON_W, y, Theme.PURPLE, false);
        String sv = ok ? "Disponible !" : Format.countdown(AdTimer.getRemainingMs());
        ctx.drawText(tr, sv, right - tr.getWidth(sv), y, ok ? Theme.SUCCESS : Theme.WARNING, false);
        return ty + h + s(CARD_GAP);
    }

    private void drawAfkCard(DrawContext ctx, TextRenderer tr, int ty) {
        int left = posX + s(PADDING), w = getWidth() - s(PADDING) * 2, right = left + w - s(6);
        int h = s(AD_CARD_H);
        boolean active = AfkTimer.isActive();
        Draw.card(ctx, left, ty, w, h, Theme.SURFACE, Theme.BORDER);
        ctx.fill(left + 1, ty + s(3), left + 3, ty + h - s(3), Theme.WARNING);

        int y = ty + (h - 8) / 2 + 1;
        int iy = y - 1;
        deferred.add(() -> Draw.item(ctx, ICON_AFK, left + s(8), iy, ICON));
        ctx.drawText(tr, active ? "Temps AFK (en cours)" : "Temps AFK", left + s(8) + ICON_W, y, Theme.WARNING, false);
        String sv = Format.duration(AfkTimer.getElapsedMs());
        ctx.drawText(tr, sv, right - tr.getWidth(sv), y, active ? Theme.SUCCESS : Theme.TEXT_MUTED, false);
    }

    public int getHeight() {
        ModConfig cfg = OBFUtilities.config;
        int h = s(HEADER_H) + 1 + s(5) + s(10) + s(4);
        int cards = 0;
        if (cfg.showBlocksPlaced) cards++;
        if (cfg.showBlocksMined)  cards++;
        if (cfg.showMobsKilled)   cards++;
        for (ModConfig.CustomBlockEntry e : cfg.customBlocks) {
            if (e.enabled) cards++;
        }
        h += cards * (s(STAT_CARD_H) + s(CARD_GAP));
        if (cfg.showAdTimer)  h += s(AD_CARD_H) + s(CARD_GAP);
        if (cfg.showAfkTimer) h += s(AD_CARD_H) + s(CARD_GAP);
        return h - s(CARD_GAP) + s(PADDING);
    }

    private void renderConnectionNotice(DrawContext ctx, TextRenderer tr, int panelH) {
        long remaining = ServerStats.getNoticeRemainingMs();
        if (remaining <= 0) return;

        String timer = Format.duration(remaining);

        int x = posX + s(PADDING), noticeY = posY + panelH + s(4);
        ctx.drawTextWithShadow(tr, "Les totaux sont calculés", x, noticeY, Theme.DANGER);
        ctx.drawTextWithShadow(tr, "à la connexion uniquement.", x, noticeY + s(10), Theme.DANGER);
        ctx.drawTextWithShadow(tr, "Disparaît dans " + timer, x, noticeY + s(20), Theme.TEXT_DIM);
    }

    public boolean isInResizeGrip(double mx, double my) {
        return Draw.inResizeGrip(mx, my, posX + getWidth(), posY + getHeight());
    }

    public boolean isMouseOver(double mx, double my) {
        return Draw.inside(mx, my, posX, posY, getWidth(), getHeight());
    }

    public int  getPosX() { return posX; }
    public int  getPosY() { return posY; }
    public void setPos(int x, int y) {
        pos.set(x, y, getWidth(), getHeight());
        posX = x;
        posY = y;
    }

    public boolean isVisible()     { return visible; }
    public void    toggleVisible() { visible = !visible; }

    public void savePosition() {
        Properties props = new Properties();
        props.setProperty("hud_x", String.valueOf(posX));
        props.setProperty("hud_y", String.valueOf(posY));
        if (pos.isSet()) {
            props.setProperty("hud_fx", String.valueOf(pos.x));
            props.setProperty("hud_fy", String.valueOf(pos.y));
        }
        try (OutputStream out = Files.newOutputStream(POS_FILE)) { props.store(out, "OBF HUD Position"); }
        catch (Exception e) { OBFUtilities.LOGGER.error("[OBF] "+e.getMessage()); }
    }

    private void loadPosition() {
        if (!Files.exists(POS_FILE)) return;
        try (InputStream in = Files.newInputStream(POS_FILE)) {
            Properties p = new Properties();
            p.load(in);
            posX = Math.max(0, Math.min(Integer.parseInt(p.getProperty("hud_x", "10")), 7680));
            posY = Math.max(0, Math.min(Integer.parseInt(p.getProperty("hud_y", "10")), 4320));
            pos.x = Float.parseFloat(p.getProperty("hud_fx", "-1"));
            pos.y = Float.parseFloat(p.getProperty("hud_fy", "-1"));
        } catch (Exception e) { posX = 10; posY = 10; }
    }
}
