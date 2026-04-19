package com.hamtabot.obfutilities.hud;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OBFConfigScreen extends Screen {

    private final OBFHud hud;
    private final ModConfig cfg;

    private static int leftX = -1, leftY = 20;
    private static final int LEFT_W = 220;
    private boolean draggingLeft = false;
    private int dragLeftOffX, dragLeftOffY;

    private static int rightX = -1, rightY = 20;
    private static final int RIGHT_W = 260;
    private boolean draggingRight = false;
    private int dragRightOffX, dragRightOffY;

    private static final int PANEL_HEADER_H = 20;

    private static boolean addingMode = false;
    private static String  pendingMode = "posé";

    private TextFieldWidget searchField;
    private List<String> allBlocks    = new ArrayList<>();
    private List<String> filteredBlocks = new ArrayList<>();
    private int blockScrollOffset = 0;
    private static final int BLOCK_LIST_VISIBLE = 6;
    private static final int BLOCK_ROW_H        = 18;
    private static final int BLOCK_LIST_HEIGHT  = BLOCK_LIST_VISIBLE * BLOCK_ROW_H;

    private boolean draggingScrollbar = false;
    private int scrollbarDragStartY = 0, scrollbarDragStartOffset = 0;

    private static int toolX = -1, toolY = 20;
    private static final int TOOL_W = 200;
    private boolean draggingTool = false;
    private int dragToolOffX, dragToolOffY;

    private static int debugX = -1, debugY = 20;
    private static final int DEBUG_W = 200;
    private boolean draggingDebug = false;
    private int dragDebugOffX, dragDebugOffY;

    private static int wpX = -1, wpY = 20;
    private static int arX = -1, arY = 20;
    private static final int AR_W = 230;
    private boolean draggingAr = false;
    private int dragArOffX, dragArOffY;
    private static boolean listeningForKey = false;
    private static final int WP_W = 300;
    private boolean draggingWp = false;
    private int dragWpOffX, dragWpOffY;
    private static int wpScrollOffset = 0;
    private static int wpConfirmDelete = -1;



    private static int brightX = -1, brightY = 20;
    private static final int BRIGHT_W = 210;
    private boolean draggingBright = false;
    private int dragBrightOffX, dragBrightOffY;

    private boolean draggingHud = false;
    private int dragHudOffX, dragHudOffY;
    private static final int HUD_W = 220;

    public OBFConfigScreen(OBFHud hud) {
        super(Text.literal("OBF Utilitaire - Configuration"));
        this.hud = hud;
        this.cfg = OBFUtilities.config;
    }

    @Override
    protected void init() {

        if (leftX == -1) {
            if (OBFUtilities.config.cfgLeftX != -1) {

                leftX   = OBFUtilities.config.cfgLeftX;   leftY   = OBFUtilities.config.cfgLeftY;
                rightX  = OBFUtilities.config.cfgRightX;  rightY  = OBFUtilities.config.cfgRightY;
                toolX   = OBFUtilities.config.cfgToolX;   toolY   = OBFUtilities.config.cfgToolY;
                brightX = OBFUtilities.config.cfgBrightX; brightY = OBFUtilities.config.cfgBrightY;
                debugX  = OBFUtilities.config.cfgDebugX;  debugY  = OBFUtilities.config.cfgDebugY;
                wpX     = OBFUtilities.config.cfgWpX != -1 ? OBFUtilities.config.cfgWpX : -1;
                wpY     = OBFUtilities.config.cfgWpY;
                arX     = OBFUtilities.config.cfgAttackRemapX != -1 ? OBFUtilities.config.cfgAttackRemapX : -1;
                arY     = OBFUtilities.config.cfgAttackRemapY;
            } else {

                int row1Y = 10;
                int gap   = 8;

                leftX   = 10;                           leftY   = row1Y;
                rightX  = leftX + LEFT_W + gap;         rightY  = row1Y;
                toolX   = rightX + RIGHT_W + gap;       toolY   = row1Y;

                int row2Y = row1Y + Math.max(computeLeftH(), Math.max(computeRightH(), computeToolH())) + gap;
                brightX = 10;                           brightY = row2Y;
                debugX  = brightX + BRIGHT_W + gap;    debugY  = row2Y;
                int row3Y = row2Y + computeBrightH() + gap;
                wpX = 10; wpY = row3Y;
                arX = WP_W + 18; arY = row3Y;

            }
        }
        com.hamtabot.obfutilities.debug.DebugOverlay.inConfigScreen = true;
        clampPanels();
        rebuildBlockList();
        initToolPanel();
        initBrightPanel();
        initDebugPanel();
        initWpPanel();
        initArPanel();


        int bx = leftX + 10, bw = LEFT_W - 20;
        int by = leftY + PANEL_HEADER_H + 10 + 14;

        addToggleButton(bx, by, bw, "Blocs posés", cfg.showBlocksPlaced, v -> cfg.showBlocksPlaced = v); by += 22;
        addToggleButton(bx, by, bw, "Blocs minés", cfg.showBlocksMined,  v -> cfg.showBlocksMined  = v); by += 22;
        addToggleButton(bx, by, bw, "Mobs tués",   cfg.showMobsKilled,   v -> cfg.showMobsKilled   = v); by += 22;
        addToggleButton(bx, by, bw, "Pub timer",   cfg.showAdTimer,      v -> cfg.showAdTimer      = v); by += 22;
        by += 26 - 22;

        addDrawableChild(ButtonWidget.builder(Text.literal("Réinitialiser la session"),
                btn -> OBFUtilities.resetSession()).dimensions(bx, by, bw, 18).build());
        by += 24;
        if (OBFUtilities.canRefreshStats()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("§bActualiser les stats globales"),
                    btn -> {
                        if (net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler() != null) {
                            net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("utilitiesstats");
                            OBFUtilities.onStatsRefreshed();
                            saveAllPositions(); clearAndInit();
                        }
                    }).dimensions(bx, by, bw, 18).build());
        } else {

            long rem = OBFUtilities.getStatsRefreshCooldownRemaining();
            long m = rem / 60000, s = (rem / 1000) % 60;
            String timerStr = "Attendez " + String.format("%dm %02ds", m, s) + " avant d'actualiser";
            ButtonWidget cooldownBtn = ButtonWidget.builder(Text.literal("§7" + timerStr), btn -> {}).dimensions(bx, by, bw, 18).build();
            cooldownBtn.active = false;
            addDrawableChild(cooldownBtn);
        }

        int rx = rightX + 10, rw = RIGHT_W - 20;
        int ry = rightY + PANEL_HEADER_H + 10 + 14;

        for (int i = 0; i < cfg.customBlocks.size(); i++) {
            final int idx = i;
            ModConfig.CustomBlockEntry e = cfg.customBlocks.get(i);

            boolean[] st = { e.enabled };
            addDrawableChild(ButtonWidget.builder(Text.literal(st[0] ? "§aON" : "§cOFF"),
                    btn -> { st[0]=!st[0]; e.enabled=st[0]; btn.setMessage(Text.literal(st[0]?"§aON":"§cOFF")); }
            ).dimensions(rx, ry, 36, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(e.trackPlaced ? "Posé" : "Miné"),
                    btn -> { e.trackPlaced=!e.trackPlaced; btn.setMessage(Text.literal(e.trackPlaced?"Posé":"Miné")); }
            ).dimensions(rx+40, ry, 44, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("§cX"),
                    btn -> { cfg.customBlocks.remove(idx); saveAllPositions(); clearAndInit(); }
            ).dimensions(rx+rw-20, ry, 20, 16).build());

            ry += 20;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("§a+ Ajouter un bloc"),
                btn -> { addingMode = !addingMode; saveAllPositions(); clearAndInit(); }
        ).dimensions(rx, ry, rw, 18).build());
        ry += 26;

        if (addingMode) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Mode : " + pendingMode),
                    btn -> { pendingMode = pendingMode.equals("posé") ? "miné" : "posé"; btn.setMessage(Text.literal("Mode : "+pendingMode)); rebuildBlockList(); }
            ).dimensions(rx, ry, rw, 18).build());
            ry += 24;

            searchField = new TextFieldWidget(textRenderer, rx, ry, rw, 16, Text.literal(""));
            searchField.setPlaceholder(Text.literal("Chercher un bloc..."));
            searchField.setChangedListener(s -> {
                filteredBlocks = allBlocks.stream().filter(b -> b.contains(s.toLowerCase())).collect(Collectors.toList());
                blockScrollOffset = 0;
            });
            addDrawableChild(searchField);
            searchField.setEditable(true);
            setFocused(searchField);
        }
    }

    private void initDebugPanel() {
        int bx = debugX + 10, bw = DEBUG_W - 20;
        int by = debugY + PANEL_HEADER_H + 10 + 14;

        boolean[] stFps = { cfg.debugShowFps };
        addDrawableChild(ButtonWidget.builder(
                Text.literal("FPS : " + (stFps[0] ? "§aON" : "§cOFF")),
                btn -> { stFps[0]=!stFps[0]; cfg.debugShowFps=stFps[0]; btn.setMessage(Text.literal("FPS : "+(stFps[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, 18).build());
        by += 22;

        boolean[] stCoords = { cfg.debugShowCoords };
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Coordonnées : " + (stCoords[0] ? "§aON" : "§cOFF")),
                btn -> { stCoords[0]=!stCoords[0]; cfg.debugShowCoords=stCoords[0]; btn.setMessage(Text.literal("Coordonnées : "+(stCoords[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, 18).build());
        by += 22;

        boolean[] stRam = { cfg.debugShowRam };
        addDrawableChild(ButtonWidget.builder(
                Text.literal("RAM : " + (stRam[0] ? "§aON" : "§cOFF")),
                btn -> { stRam[0]=!stRam[0]; cfg.debugShowRam=stRam[0]; btn.setMessage(Text.literal("RAM : "+(stRam[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, 18).build());
    }

    private void initBrightPanel() {
        int bx = brightX + 10, bw = BRIGHT_W - 20;
        int by = brightY + PANEL_HEADER_H + 10 + 14;

        boolean[] stFB = { cfg.fullBrightEnabled };
        addDrawableChild(ButtonWidget.builder(
                Text.literal("FullBright : " + (stFB[0] ? "§aON" : "§cOFF")),
                btn -> { stFB[0]=!stFB[0]; cfg.fullBrightEnabled=stFB[0]; btn.setMessage(Text.literal("FullBright : "+(stFB[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, 18).build());
        by += 26;

        by += 14;
        int btnW = 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("<<"),
                btn -> { cfg.fullBrightLevel = Math.max(0f, cfg.fullBrightLevel - 1f); saveAllPositions(); clearAndInit(); }
        ).dimensions(bx, by, btnW, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("<"),
                btn -> { cfg.fullBrightLevel = Math.max(0f, cfg.fullBrightLevel - 0.1f); saveAllPositions(); clearAndInit(); }
        ).dimensions(bx+btnW+2, by, btnW, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"),
                btn -> { cfg.fullBrightLevel = Math.min(15f, cfg.fullBrightLevel + 0.1f); saveAllPositions(); clearAndInit(); }
        ).dimensions(bx+bw-btnW*2-2, by, btnW, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">>"),
                btn -> { cfg.fullBrightLevel = Math.min(15f, cfg.fullBrightLevel + 1f); saveAllPositions(); clearAndInit(); }
        ).dimensions(bx+bw-btnW, by, btnW, 16).build());
    }

    private void initToolPanel() {
        int bx = toolX + 10, bw = TOOL_W - 20;
        int by = toolY + PANEL_HEADER_H + 10 + 14;

        boolean[] stEnabled = { cfg.autoToolEnabled };
        addDrawableChild(ButtonWidget.builder(
                Text.literal("AutoTool : " + (stEnabled[0] ? "§aON" : "§cOFF")),
                btn -> { stEnabled[0]=!stEnabled[0]; cfg.autoToolEnabled=stEnabled[0]; btn.setMessage(Text.literal("AutoTool : "+(stEnabled[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, 18).build());
        by += 24;

        boolean[] stDura = { cfg.autoToolSkipLowDurability };
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Skip durabilité ≤10 : " + (stDura[0] ? "§aON" : "§cOFF")),
                btn -> { stDura[0]=!stDura[0]; cfg.autoToolSkipLowDurability=stDura[0]; btn.setMessage(Text.literal("Skip durabilité ≤10 : "+(stDura[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, 18).build());
        by += 28;

        by += 14;

        int hw = (bw - 4) / 2;
        addToolToggle(bx,      by, hw, "⛏ Pioche",   cfg.autoToolUsePioche,   v -> cfg.autoToolUsePioche   = v);
        addToolToggle(bx+hw+4, by, hw, "🪣 Pelle",    cfg.autoToolUsePelle,    v -> cfg.autoToolUsePelle    = v); by += 20;
        addToolToggle(bx,      by, hw, "🪓 Hache",    cfg.autoToolUseHache,    v -> cfg.autoToolUseHache    = v);
        addToolToggle(bx+hw+4, by, hw, "🌾 Houe",     cfg.autoToolUseHoue,     v -> cfg.autoToolUseHoue     = v); by += 20;
        addToolToggle(bx,      by, hw, "✂ Cisaille",  cfg.autoToolUseCisaille, v -> cfg.autoToolUseCisaille = v);
        addToolToggle(bx+hw+4, by, hw, "⚔ Épée",     cfg.autoToolUseEpee,     v -> cfg.autoToolUseEpee     = v);
    }

    private void addToolToggle(int x, int y, int w, String label, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        boolean[] state = { initial };
        addDrawableChild(ButtonWidget.builder(
                Text.literal((state[0] ? "§a" : "§c") + label),
                btn -> { state[0]=!state[0]; onChange.accept(state[0]); btn.setMessage(Text.literal((state[0]?"§a":"§c")+label)); }
        ).dimensions(x, y, w, 16).build());
    }

    private void addToggleButton(int x, int y, int w, String label, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        boolean[] state = { initial };
        addDrawableChild(ButtonWidget.builder(
                Text.literal(label + " : " + (state[0] ? "§aON" : "§cOFF")),
                btn -> { state[0]=!state[0]; onChange.accept(state[0]); btn.setMessage(Text.literal(label+" : "+(state[0]?"§aON":"§cOFF"))); }
        ).dimensions(x, y, w, 18).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        hud.render(context, delta);

        drawPanel(context, leftX,  leftY,  LEFT_W,  computeLeftH(),  "§e◆ Paramètres");
        drawPanel(context, rightX, rightY, RIGHT_W, computeRightH(), "§d◆ Blocs custom");

        int ty = leftY + PANEL_HEADER_H + 10;
        context.drawText(textRenderer, "§7Sections à afficher :", leftX+10, ty, 0xFFB0BEC5, false);

        ty = rightY + PANEL_HEADER_H + 10;
        context.drawText(textRenderer, "§7Blocs trackés :", rightX+10, ty, 0xFFB0BEC5, false);
        ty += 14;

        int rx = rightX + 10, rw = RIGHT_W - 20;
        for (ModConfig.CustomBlockEntry e : cfg.customBlocks) {

            try {
                Item item = Registries.ITEM.get(new Identifier(e.blockId));
                context.drawItem(new ItemStack(item), rx+86, ty);
            } catch (Exception ignored) {}

            String name = formatBlockName(e.blockId);
            context.drawText(textRenderer, "§7"+name, rx+104, ty+4, 0xFFB0BEC5, false);
            ty += 20;
        }

        ty += 18 + 26;

        if (addingMode) {
            ty += 18 + 24;

            int listX = rightX + 10;
            int listW = RIGHT_W - 20;
            int listY = ty + 20;

            context.fill(listX, listY, listX+listW, listY+BLOCK_LIST_HEIGHT, 0xFF0A0A0A);
            context.fill(listX, listY, listX+listW, listY+1, 0xFF333333);
            context.fill(listX, listY+BLOCK_LIST_HEIGHT-1, listX+listW, listY+BLOCK_LIST_HEIGHT, 0xFF333333);
            context.fill(listX, listY, listX+1, listY+BLOCK_LIST_HEIGHT, 0xFF333333);
            context.fill(listX+listW-1, listY, listX+listW, listY+BLOCK_LIST_HEIGHT, 0xFF333333);

            for (int i = 0; i < BLOCK_LIST_VISIBLE; i++) {
                int idx = i + blockScrollOffset;
                if (idx >= filteredBlocks.size()) break;
                String blockId = filteredBlocks.get(idx);
                int rowY = listY + i * BLOCK_ROW_H;

                if (mouseX >= listX && mouseX <= listX+listW-9 && mouseY >= rowY && mouseY < rowY+BLOCK_ROW_H)
                    context.fill(listX+1, rowY, listX+listW-9, rowY+BLOCK_ROW_H, 0xFF151515);

                try {
                    Item item = Registries.ITEM.get(new Identifier(blockId));
                    context.drawItem(new ItemStack(item), listX+2, rowY+1);
                } catch (Exception ignored) {}

                String dn = blockId.contains(":") ? blockId.split(":")[1] : blockId;
                context.drawText(textRenderer, dn, listX+22, rowY+5, 0xFFB0BEC5, false);

                if (i < BLOCK_LIST_VISIBLE-1)
                    context.fill(listX+1, rowY+BLOCK_ROW_H-1, listX+listW-9, rowY+BLOCK_ROW_H, 0xFF111111);
            }

            if (filteredBlocks.size() > BLOCK_LIST_VISIBLE) {
                int maxScroll = filteredBlocks.size() - BLOCK_LIST_VISIBLE;
                int sbH = Math.max(20, BLOCK_LIST_HEIGHT * BLOCK_LIST_VISIBLE / filteredBlocks.size());
                int sbTrack = BLOCK_LIST_HEIGHT - sbH;
                int sbY = listY + (maxScroll > 0 ? blockScrollOffset * sbTrack / maxScroll : 0);
                context.fill(listX+listW-9, listY, listX+listW-1, listY+BLOCK_LIST_HEIGHT, 0xFF1A1A1A);
                context.fill(listX+listW-9, sbY, listX+listW-1, sbY+sbH, 0xFF4FC3F7);
            }
        }

        drawPanel(context, debugX, debugY, DEBUG_W, computeDebugH(), "§c◆ Débug");
        int dty = debugY + PANEL_HEADER_H + 10;
        context.drawText(textRenderer, "§7Overlays de débug :", debugX+10, dty, 0xFFB0BEC5, false);
        dty += 14;

        drawPanel(context, brightX, brightY, BRIGHT_W, computeBrightH(), "§b◆ FullBright");
        int bty = brightY + PANEL_HEADER_H + 10;
        context.drawText(textRenderer, "§7Luminosité du jeu :", brightX+10, bty, 0xFFB0BEC5, false);
        bty += 14 + 18 + 26 + 14;

        int pct = Math.round(cfg.fullBrightLevel * 100f);
        String pctStr = pct + "%";
        int pctX = brightX + BRIGHT_W/2 - textRenderer.getWidth(pctStr)/2;
        context.drawTextWithShadow(textRenderer, "§f" + pctStr, pctX, bty + 3, 0xFFFFFFFF);

        int barX = brightX + 10 + 24*2 + 4, barW = (BRIGHT_W-20) - 24*4 - 8;
        int barY = bty + 2;
        context.fill(barX, barY, barX+barW, barY+12, 0xFF111111);
        int filled = (int)(barW * (cfg.fullBrightLevel / 15f));
        context.fill(barX, barY, barX+filled, barY+12, 0xFFFFD54F);

        drawPanel(context, toolX, toolY, TOOL_W, computeToolH(), "§6◆ AutoTool");
        int tty = toolY + PANEL_HEADER_H + 10;
        context.drawText(textRenderer, "§7Sélection automatique d'outil", toolX+10, tty, 0xFFB0BEC5, false);
        tty += 14 + 18 + 24 + 18 + 28;
        context.drawText(textRenderer, "§7Outils à utiliser :", toolX+10, tty, 0xFFB0BEC5, false);

        context.drawText(textRenderer, "§7[Échap] Sauvegarder", leftX+10, leftY+computeLeftH()-14, 0xFF546E7A, false);

        int noteY = leftY + computeLeftH() + 6;
        context.drawText(textRenderer, "§f⚠ Les totaux sont calculés à la connexion uniquement.", leftX+10, noteY, 0xFFFFFFFF, false);
        context.drawText(textRenderer, "§7Pour recalculer, utilisez le bouton Actualiser.", leftX+10, noteY+10, 0xFFB0BEC5, false);

        drawPanel(context, arX, arY, AR_W, computeArH(), "§a◆ Bind Touche");
        int arDesc = arY + PANEL_HEADER_H + 10 + 18 + 26 + 18 + 4;
        String arStatus = OBFUtilities.config.attackRemapEnabled
                ? "§aActif — touche : §f" + getGlfwKeyName(OBFUtilities.config.attackRemapKey)
                : "§7Inactif — clic gauche par défaut";
        context.drawText(textRenderer, arStatus, arX+10, arDesc, 0xFFB0BEC5, false);

        drawPanel(context, wpX, wpY, WP_W, computeWpH(), "§6◆ Waypoints");
        int wty = wpY + WP_HEADER_H + 10 + 18 + 24 + 18 + 26;
        java.util.List<com.hamtabot.obfutilities.waypoint.Waypoint> wpAll = com.hamtabot.obfutilities.waypoint.WaypointManager.getAll();
        int wpListH = WP_VISIBLE * WP_ROW_H;

        if (wpAll.isEmpty()) {
            context.drawText(textRenderer, "§7Aucun waypoint. Appuyez sur + Créer.", wpX+10, wty+10, 0xFFB0BEC5, false);
        }

        for (int i = 0; i < WP_VISIBLE && i + wpScrollOffset < wpAll.size(); i++) {
            int idx = i + wpScrollOffset;
            com.hamtabot.obfutilities.waypoint.Waypoint wp = wpAll.get(idx);
            int rowY = wty + i * WP_ROW_H;
            boolean cur = com.hamtabot.obfutilities.waypoint.WaypointManager.getCurrentDimension().equals(wp.dimension);
            boolean hovered = mouseX >= wpX+10 && mouseX <= wpX+WP_W-14 && mouseY >= rowY && mouseY < rowY+WP_ROW_H-2;

            context.fill(wpX+10, rowY, wpX+WP_W-14, rowY+WP_ROW_H-2, hovered ? 0x441A3A6A : (cur ? 0x221A3A4A : 0x11FFFFFF));
            context.fill(wpX+14, rowY+6, wpX+24, rowY+16, wp.color);

            String enabledMark = wp.enabled ? "§a●" : "§8●";
            context.drawText(textRenderer, enabledMark, wpX+28, rowY+4, 0xFFFFFFFF, false);
            context.drawTextWithShadow(textRenderer, "§f" + wp.name, wpX+40, rowY+4, wp.color);
            context.drawText(textRenderer, "§7" + wp.x+", "+wp.y+", "+wp.z+"  §8["+wp.getDimShort()+"]", wpX+40, rowY+15, 0xFF888888, false);

            if (hovered) {
                int btnY = rowY + WP_ROW_H - 18;
                int btnW2 = 64;
                int gap2 = 4;
                // Toggle
                drawHoverBtn(context, textRenderer, wpX+14, btnY, btnW2, 14, wp.enabled ? "§aVisible" : "§8Masqué");
                // Partager
                // Supprimer / Confirmer
                if (wpConfirmDelete == idx) {
                    drawHoverBtn(context, textRenderer, wpX+14+(btnW2+gap2)*2, btnY, btnW2+20, 14, "§cConfirmer ?");
                } else {
                    drawHoverBtn(context, textRenderer, wpX+14+(btnW2+gap2)*2, btnY, btnW2, 14, "§c✗ Suppr.");
                }
            }
        }

        // Scrollbar toujours visible
        context.fill(wpX+WP_W-12, wty, wpX+WP_W-2, wty+wpListH, 0xFF1A1A1A);
        if (!wpAll.isEmpty()) {
            int total = Math.max(wpAll.size(), WP_VISIBLE);
            int sbH = Math.max(20, wpListH * WP_VISIBLE / total);
            int maxSc = Math.max(0, wpAll.size() - WP_VISIBLE);
            int sbY = wty + (maxSc > 0 ? wpScrollOffset * (wpListH-sbH) / maxSc : 0);
            context.fill(wpX+WP_W-12, sbY, wpX+WP_W-2, sbY+sbH, 0xFF4FC3F7);
        }

        super.render(context, mouseX, mouseY, delta);

        com.hamtabot.obfutilities.debug.DebugOverlay.render(context, delta);
    }

    private void drawPanel(DrawContext ctx, int x, int y, int w, int h, String title) {
        ctx.fill(x, y, x+w, y+h, 0xEE0D0D0D);
        ctx.fill(x, y, x+w, y+1, 0xFFFFD54F);
        ctx.fill(x, y+h-1, x+w, y+h, 0xFFFFD54F);
        ctx.fill(x, y, x+1, y+h, 0xFFFFD54F);
        ctx.fill(x+w-1, y, x+w, y+h, 0xFFFFD54F);
        ctx.fill(x+1, y+1, x+w-1, y+PANEL_HEADER_H, 0xFF1A1A1A);
        ctx.drawTextWithShadow(textRenderer, title + "  §7— glisser pour déplacer", x+8, y+6, 0xFFFFD54F);
    }

    private int getRightListY() {
        return rightY + PANEL_HEADER_H + 10 + 14
                + cfg.customBlocks.size() * 20
                + 18 + 26 + 18 + 24 + 20;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {

            if (mouseX >= leftX && mouseX <= leftX+LEFT_W && mouseY >= leftY && mouseY <= leftY+PANEL_HEADER_H) {
                draggingLeft = true; dragLeftOffX=(int)mouseX-leftX; dragLeftOffY=(int)mouseY-leftY; return true;
            }

            if (mouseX >= rightX && mouseX <= rightX+RIGHT_W && mouseY >= rightY && mouseY <= rightY+PANEL_HEADER_H) {
                draggingRight = true; dragRightOffX=(int)mouseX-rightX; dragRightOffY=(int)mouseY-rightY; return true;
            }

            if (mouseX >= debugX && mouseX <= debugX+DEBUG_W && mouseY >= debugY && mouseY <= debugY+PANEL_HEADER_H) {
                draggingDebug = true; dragDebugOffX=(int)mouseX-debugX; dragDebugOffY=(int)mouseY-debugY; return true;
            }

            if (mouseX >= brightX && mouseX <= brightX+BRIGHT_W && mouseY >= brightY && mouseY <= brightY+PANEL_HEADER_H) {
                draggingBright = true; dragBrightOffX=(int)mouseX-brightX; dragBrightOffY=(int)mouseY-brightY; return true;
            }

            if (mouseX >= toolX && mouseX <= toolX+TOOL_W && mouseY >= toolY && mouseY <= toolY+PANEL_HEADER_H) {
                draggingTool = true; dragToolOffX=(int)mouseX-toolX; dragToolOffY=(int)mouseY-toolY; return true;
            }

            if (mouseX >= wpX && mouseX <= wpX+WP_W && mouseY >= wpY && mouseY <= wpY+WP_HEADER_H) {
                draggingWp = true; dragWpOffX=(int)mouseX-wpX; dragWpOffY=(int)mouseY-wpY; return true;
            }
            if (mouseX >= arX && mouseX <= arX+AR_W && mouseY >= arY && mouseY <= arY+PANEL_HEADER_H) {
                draggingAr = true; dragArOffX=(int)mouseX-arX; dragArOffY=(int)mouseY-arY; return true;
            }
        }

        if (addingMode) {
            int listX = rightX + 10, listW = RIGHT_W - 20;
            int listY = getRightListY();

            if (mouseX >= listX+listW-9 && mouseX <= listX+listW && mouseY >= listY && mouseY < listY+BLOCK_LIST_HEIGHT) {
                draggingScrollbar = true; scrollbarDragStartY=(int)mouseY; scrollbarDragStartOffset=blockScrollOffset; return true;
            }
            if (mouseX >= listX && mouseX <= listX+listW-9 && mouseY >= listY && mouseY < listY+BLOCK_LIST_HEIGHT) {
                int row = ((int)mouseY - listY) / BLOCK_ROW_H + blockScrollOffset;
                if (row >= 0 && row < filteredBlocks.size()) {
                    cfg.customBlocks.add(new ModConfig.CustomBlockEntry(filteredBlocks.get(row), pendingMode.equals("posé")));
                    addingMode = false;
                    saveAllPositions(); clearAndInit();
                    return true;
                }
            }
        }

        if (button == 0) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player != null) {
                if (cfg.debugShowFps) {
                    int[] b = com.hamtabot.obfutilities.debug.DebugOverlay.getFpsBounds(textRenderer, mc);
                    if (mouseX>=b[0] && mouseX<=b[0]+b[2] && mouseY>=b[1] && mouseY<=b[1]+b[3]) {
                        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingFps = true;
                        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragFpsOffX = (int)mouseX-b[0];
                        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragFpsOffY = (int)mouseY-b[1];
                        return true;
                    }
                }
                if (cfg.debugShowCoords) {
                    int[] b = com.hamtabot.obfutilities.debug.DebugOverlay.getCoordsBounds(textRenderer, mc);
                    if (mouseX>=b[0] && mouseX<=b[0]+b[2] && mouseY>=b[1] && mouseY<=b[1]+b[3]) {
                        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingCoords = true;
                        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragCoordsOffX = (int)mouseX-b[0];
                        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragCoordsOffY = (int)mouseY-b[1];
                        return true;
                    }
                }
                if (cfg.debugShowRam) {
                    int[] b = com.hamtabot.obfutilities.debug.DebugOverlay.getRamBounds(textRenderer);
                    if (mouseX>=b[0] && mouseX<=b[0]+b[2] && mouseY>=b[1] && mouseY<=b[1]+b[3]) {
                        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingRam = true;
                        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragRamOffX = (int)mouseX-b[0];
                        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragRamOffY = (int)mouseY-b[1];
                        return true;
                    }
                }
            }
        }

        int hx = hud.getPosX(), hy = hud.getPosY();
        if (button == 0 && mouseX >= hx && mouseX <= hx+HUD_W && mouseY >= hy && mouseY <= hy+300) {
            boolean inLeft  = mouseX >= leftX  && mouseX <= leftX+LEFT_W   && mouseY >= leftY  && mouseY <= leftY+computeLeftH();
            boolean inRight = mouseX >= rightX && mouseX <= rightX+RIGHT_W && mouseY >= rightY && mouseY <= rightY+computeRightH();
            if (!inLeft && !inRight) { draggingHud=true; dragHudOffX=(int)mouseX-hx; dragHudOffY=(int)mouseY-hy; return true; }
        }

        // Clics sur les boutons hover des waypoints
        if (button == 0) {
            int wty2 = wpY + WP_HEADER_H + 10 + 18 + 24 + 18 + 26;
            int wlH = WP_VISIBLE * WP_ROW_H;
            java.util.List<com.hamtabot.obfutilities.waypoint.Waypoint> wAll = com.hamtabot.obfutilities.waypoint.WaypointManager.getAll();

            // Scrollbar
            if (wAll.size() > WP_VISIBLE && mouseX >= wpX+WP_W-12 && mouseX <= wpX+WP_W-2 && mouseY >= wty2 && mouseY < wty2+wlH) {
                wpScrollOffset = Math.max(0, Math.min((int)(((mouseY-wty2)/(double)wlH)*wAll.size()), wAll.size()-WP_VISIBLE));
                return true;
            }

            int btnW2 = 64, gap2 = 4;
            for (int i = 0; i < WP_VISIBLE && i + wpScrollOffset < wAll.size(); i++) {
                int idx = i + wpScrollOffset;
                com.hamtabot.obfutilities.waypoint.Waypoint wp = wAll.get(idx);
                int rowY = wty2 + i * WP_ROW_H;
                boolean hovered = mouseX >= wpX+10 && mouseX <= wpX+WP_W-14 && mouseY >= rowY && mouseY < rowY+WP_ROW_H-2;
                if (!hovered) continue;
                int btnY = rowY + WP_ROW_H - 18;
                if (mouseX >= wpX+14 && mouseX <= wpX+14+btnW2 && mouseY >= btnY && mouseY < btnY+14) {
                    wp.enabled = !wp.enabled; com.hamtabot.obfutilities.waypoint.WaypointManager.save(); return true;
                }
                if (mouseX >= wpX+14+btnW2+gap2 && mouseY >= btnY && mouseY < btnY+14) {
                    if (wpConfirmDelete == idx) {
                        com.hamtabot.obfutilities.waypoint.WaypointManager.remove(idx);
                        wpConfirmDelete = -1; wpScrollOffset = Math.max(0, wpScrollOffset-1);
                    } else { wpConfirmDelete = idx; }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingScrollbar && button == 0) {
            int max = Math.max(0, filteredBlocks.size()-BLOCK_LIST_VISIBLE);
            if (max > 0) {
                int delta = (int)((mouseY-scrollbarDragStartY) / ((double)BLOCK_LIST_HEIGHT/filteredBlocks.size()));
                blockScrollOffset = Math.max(0, Math.min(scrollbarDragStartOffset+delta, max));
            }
            return true;
        }

        // Scrollbar wp
        if (com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingFps && button == 0) {
            cfg.debugFpsX = (int)mouseX - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragFpsOffX;
            cfg.debugFpsY = (int)mouseY - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragFpsOffY;
            return true;
        }
        if (com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingCoords && button == 0) {
            cfg.debugCoordsX = (int)mouseX - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragCoordsOffX;
            cfg.debugCoordsY = (int)mouseY - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragCoordsOffY;
            return true;
        }
        if (com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingRam && button == 0) {
            cfg.debugRamX = (int)mouseX - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragRamOffX;
            cfg.debugRamY = (int)mouseY - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragRamOffY;
            return true;
        }
        // Capture touche d'attaque
        if (listeningForKey && button == 0) {
            // Clic ignoré pendant l'écoute
            listeningForKey = false; saveAllPositions(); clearAndInit(); return true;
        }

        if (draggingWp && button == 0) {
            wpX = Math.max(0, Math.min((int)mouseX-dragWpOffX, this.width-WP_W));
            wpY = Math.max(0, Math.min((int)mouseY-dragWpOffY, this.height-computeWpH()));
            OBFUtilities.config.cfgWpX = wpX; OBFUtilities.config.cfgWpY = wpY;
            OBFUtilities.config.cfgAttackRemapX = arX; OBFUtilities.config.cfgAttackRemapY = arY;
            saveAllPositions(); clearAndInit(); return true;
        }
        if (draggingAr && button == 0) {
            arX = Math.max(0, Math.min((int)mouseX-dragArOffX, this.width-AR_W));
            arY = Math.max(0, Math.min((int)mouseY-dragArOffY, this.height-computeArH()));
            OBFUtilities.config.cfgAttackRemapX = arX; OBFUtilities.config.cfgAttackRemapY = arY;
            saveAllPositions(); clearAndInit(); return true;
        }
        if (draggingDebug && button == 0) {
            debugX = Math.max(0, Math.min((int)mouseX-dragDebugOffX, this.width-DEBUG_W));
            debugY = Math.max(0, Math.min((int)mouseY-dragDebugOffY, this.height-computeDebugH()));
            OBFUtilities.config.cfgDebugX = debugX; OBFUtilities.config.cfgDebugY = debugY;
            OBFUtilities.config.cfgWpX = wpX; OBFUtilities.config.cfgWpY = wpY;
            OBFUtilities.config.cfgAttackRemapX = arX; OBFUtilities.config.cfgAttackRemapY = arY;
            saveAllPositions(); clearAndInit(); return true;
        }
        if (draggingBright && button == 0) {
            brightX = Math.max(0, Math.min((int)mouseX-dragBrightOffX, this.width-BRIGHT_W));
            brightY = Math.max(0, Math.min((int)mouseY-dragBrightOffY, this.height-computeBrightH()));
            OBFUtilities.config.cfgBrightX = brightX; OBFUtilities.config.cfgBrightY = brightY;
            saveAllPositions(); clearAndInit(); return true;
        }
        if (draggingTool && button == 0) {
            toolX = Math.max(0, Math.min((int)mouseX-dragToolOffX, this.width-TOOL_W));
            toolY = Math.max(0, Math.min((int)mouseY-dragToolOffY, this.height-computeToolH()));
            OBFUtilities.config.cfgToolX = toolX; OBFUtilities.config.cfgToolY = toolY;
            saveAllPositions(); clearAndInit(); return true;
        }
        if (draggingLeft && button == 0) {
            leftX = Math.max(0, Math.min((int)mouseX-dragLeftOffX, this.width-LEFT_W));
            leftY = Math.max(0, Math.min((int)mouseY-dragLeftOffY, this.height-computeLeftH()));
            OBFUtilities.config.cfgLeftX = leftX; OBFUtilities.config.cfgLeftY = leftY;
            saveAllPositions(); clearAndInit(); return true;
        }
        if (draggingRight && button == 0) {
            rightX = Math.max(0, Math.min((int)mouseX-dragRightOffX, this.width-RIGHT_W));
            rightY = Math.max(0, Math.min((int)mouseY-dragRightOffY, this.height-computeRightH()));
            OBFUtilities.config.cfgRightX = rightX; OBFUtilities.config.cfgRightY = rightY;
            saveAllPositions(); clearAndInit(); return true;
        }
        if (draggingHud && button == 0) { hud.setPos((int)mouseX-dragHudOffX, (int)mouseY-dragHudOffY); return true; }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingLeft = draggingRight = draggingHud = draggingScrollbar = draggingTool = draggingBright = draggingDebug = draggingWp = draggingAr = false;
        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingFps    = false;
        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingCoords = false;
        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingRam    = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (addingMode) {
            int listX = rightX+10, listW = RIGHT_W-20, listY = getRightListY();
            if (mouseX >= listX && mouseX <= listX+listW && mouseY >= listY && mouseY < listY+BLOCK_LIST_HEIGHT) {
                blockScrollOffset = Math.max(0, Math.min(blockScrollOffset-(int)amount, Math.max(0, filteredBlocks.size()-BLOCK_LIST_VISIBLE)));
                return true;
            }
        }
        // Scroll liste waypoints
        int wtyS = wpY + WP_HEADER_H + 10 + 18 + 24 + 18 + 26;
        int wlHS = WP_VISIBLE * WP_ROW_H;
        if (mouseX >= wpX && mouseX <= wpX+WP_W && mouseY >= wtyS && mouseY < wtyS+wlHS) {
            int maxScroll = Math.max(0, com.hamtabot.obfutilities.waypoint.WaypointManager.getAll().size() - WP_VISIBLE);
            wpScrollOffset = Math.max(0, Math.min(wpScrollOffset - (int)amount, maxScroll));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningForKey) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                listeningForKey = false; saveAllPositions(); clearAndInit(); return true;
            }
            OBFUtilities.config.attackRemapKey = keyCode;
            OBFUtilities.config.save();
            if (OBFUtilities.config.attackRemapEnabled) {
                MinecraftClient.getInstance().options.attackKey.setBoundKey(
                        net.minecraft.client.util.InputUtil.fromKeyCode(keyCode, 0));
                net.minecraft.client.option.KeyBinding.updateKeysByCode();
            }
            listeningForKey = false; saveAllPositions(); clearAndInit(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        // Sauvegarder toutes les positions courantes dans la config
        OBFUtilities.config.cfgLeftX = leftX;   OBFUtilities.config.cfgLeftY = leftY;
        OBFUtilities.config.cfgRightX = rightX;  OBFUtilities.config.cfgRightY = rightY;
        OBFUtilities.config.cfgToolX = toolX;    OBFUtilities.config.cfgToolY = toolY;
        OBFUtilities.config.cfgBrightX = brightX; OBFUtilities.config.cfgBrightY = brightY;
        OBFUtilities.config.cfgDebugX = debugX;  OBFUtilities.config.cfgDebugY = debugY;
        OBFUtilities.config.cfgWpX = wpX;        OBFUtilities.config.cfgWpY = wpY;
        // Reset TOUS les panneaux pour forcer un rechargement depuis la config
        leftX = -1; rightX = -1; toolX = -1; brightX = -1; debugX = -1; wpX = -1; arX = -1;
        super.resize(client, width, height);
    }

    private void saveAllPositions() {
        if (leftX != -1)  { OBFUtilities.config.cfgLeftX = leftX;   OBFUtilities.config.cfgLeftY = leftY; }
        if (rightX != -1) { OBFUtilities.config.cfgRightX = rightX; OBFUtilities.config.cfgRightY = rightY; }
        if (toolX != -1)  { OBFUtilities.config.cfgToolX = toolX;   OBFUtilities.config.cfgToolY = toolY; }
        if (brightX != -1){ OBFUtilities.config.cfgBrightX = brightX; OBFUtilities.config.cfgBrightY = brightY; }
        if (debugX != -1) { OBFUtilities.config.cfgDebugX = debugX; OBFUtilities.config.cfgDebugY = debugY; }
        if (wpX != -1)    { OBFUtilities.config.cfgWpX = wpX;       OBFUtilities.config.cfgWpY = wpY; }
        if (arX != -1)    { OBFUtilities.config.cfgAttackRemapX = arX; OBFUtilities.config.cfgAttackRemapY = arY; }
    }

    private int refreshTick = 0;

    @Override
    public void tick() {
        super.tick();

        if (!OBFUtilities.canRefreshStats()) {
            refreshTick++;
            if (refreshTick >= 20) {
                refreshTick = 0;
                // Ne pas recharger si le joueur est en train d'écrire dans un champ
                boolean fieldFocused = searchField != null && searchField.isFocused();
                if (!fieldFocused && !listeningForKey) {
                    saveAllPositions(); clearAndInit();
                }
            }
        } else {
            refreshTick = 0;
        }
    }

    @Override public void close() {
        com.hamtabot.obfutilities.debug.DebugOverlay.inConfigScreen = false;

        OBFUtilities.config.cfgLeftX = leftX; OBFUtilities.config.cfgLeftY = leftY;
        OBFUtilities.config.cfgRightX = rightX; OBFUtilities.config.cfgRightY = rightY;
        OBFUtilities.config.cfgToolX = toolX; OBFUtilities.config.cfgToolY = toolY;
        OBFUtilities.config.cfgBrightX = brightX; OBFUtilities.config.cfgBrightY = brightY;
        OBFUtilities.config.cfgDebugX = debugX; OBFUtilities.config.cfgDebugY = debugY;
        OBFUtilities.config.cfgWpX = wpX; OBFUtilities.config.cfgWpY = wpY;
        OBFUtilities.config.cfgAttackRemapX = arX; OBFUtilities.config.cfgAttackRemapY = arY;
        cfg.save(); hud.savePosition(); super.close();
    }
    @Override public boolean shouldPause() { return false; }

    private void clampPanels() {
        if (toolX == -1) toolX = 10;
        toolX  = Math.max(0, Math.min(toolX,  this.width-TOOL_W));
        toolY  = Math.max(0, Math.min(toolY,  this.height-computeToolH()));
        if (brightX == -1) { brightX = 10; brightY = 20; }
        brightX = Math.max(0, Math.min(brightX, this.width-BRIGHT_W));
        brightY = Math.max(0, Math.min(brightY, this.height-computeBrightH()));
        if (wpX == -1) { wpX = 10; wpY = 20; }
        wpX = Math.max(0, Math.min(wpX, this.width-WP_W));
        wpY = Math.max(0, Math.min(wpY, this.height-computeWpH()));
        if (arX == -1) { arX = WP_W + 18; arY = 20; }
        arX = Math.max(0, Math.min(arX, this.width-AR_W));
        arY = Math.max(0, Math.min(arY, this.height-computeArH()));
        if (debugX == -1) { debugX = 10; debugY = 20; }
        debugX = Math.max(0, Math.min(debugX, this.width-DEBUG_W));
        debugY = Math.max(0, Math.min(debugY, this.height-computeDebugH()));
        leftX  = Math.max(0, Math.min(leftX,  this.width-LEFT_W));
        leftY  = Math.max(0, Math.min(leftY,  this.height-computeLeftH()));
        rightX = Math.max(0, Math.min(rightX, this.width-RIGHT_W));
        rightY = Math.max(0, Math.min(rightY, this.height-computeRightH()));
    }

    private void rebuildBlockList() {
        boolean placed = pendingMode.equals("posé");
        allBlocks = (placed ? Registries.ITEM.getIds() : Registries.BLOCK.getIds()).stream()
                .map(Identifier::toString).filter(id -> !id.equals("minecraft:air"))
                .sorted().collect(Collectors.toList());
        filteredBlocks = searchField != null && !searchField.getText().isEmpty()
                ? allBlocks.stream().filter(b -> b.contains(searchField.getText().toLowerCase())).collect(Collectors.toList())
                : new ArrayList<>(allBlocks);
        blockScrollOffset = 0;
    }

    private String formatBlockName(String id) {
        String n = id.contains(":") ? id.split(":")[1] : id;
        StringBuilder sb = new StringBuilder();
        for (String p : n.split("_")) if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        return sb.toString().trim();
    }

    private int computeDebugH() {
        return PANEL_HEADER_H + 10 + 14 + 18 + 22 + 18 + 22 + 18 + 16 + 10;
    }

    private int computeBrightH() {
        return PANEL_HEADER_H + 10 + 14 + 18 + 26 + 14 + 16 + 12;
    }

    private int computeToolH() {

        return PANEL_HEADER_H + 10 + 14 + 18 + 24 + 18 + 28 + 14 + 20 + 20 + 16 + 14;
    }

    private static final int WP_ROW_H = 36;
    private static final int WP_VISIBLE = 4;
    private static final int WP_HEADER_H = 20;

    private void initWpPanel() {
        int bx = wpX + 10, bw = WP_W - 20;
        int by = wpY + WP_HEADER_H + 10;

        boolean[] st = { OBFUtilities.config.waypointsEnabled };
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Waypoints : " + (st[0] ? "§aON" : "§cOFF")),
                btn -> { st[0]=!st[0]; OBFUtilities.config.waypointsEnabled=st[0]; btn.setMessage(Text.literal("Waypoints : "+(st[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, 18).build());
        by += 24;

        String keyName = OBFUtilities.keyAddWaypoint != null ? OBFUtilities.keyAddWaypoint.getBoundKeyLocalizedText().getString() : "N";
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§a+ Créer un waypoint  [" + keyName + "]"),
                btn -> MinecraftClient.getInstance().setScreen(new com.hamtabot.obfutilities.waypoint.WaypointCreateScreen(() -> MinecraftClient.getInstance().setScreen(this)))
        ).dimensions(bx, by, bw, 18).build());
        by += 26 + WP_VISIBLE * WP_ROW_H + 14;

        addDrawableChild(ButtonWidget.builder(Text.literal("§7📁 Ouvrir le dossier des waypoints"),
                btn -> {
                    try {
                        java.io.File configDir = new java.io.File("config").getAbsoluteFile();
                        String os = System.getProperty("os.name").toLowerCase();
                        if (os.contains("win")) {
                            new ProcessBuilder("explorer.exe", configDir.getAbsolutePath()).start();
                        } else if (os.contains("mac")) {
                            Runtime.getRuntime().exec(new String[]{"open", configDir.getAbsolutePath()});
                        } else {
                            Runtime.getRuntime().exec(new String[]{"xdg-open", configDir.getAbsolutePath()});
                        }
                    } catch (Exception e) { OBFUtilities.LOGGER.error("[OBF] Impossible d'ouvrir le dossier: " + e.getMessage()); }
                }).dimensions(bx, by, bw, 18).build());
        by += 24;


    }



    private static net.minecraft.client.util.InputUtil.Key originalAttackKey = null;

    private void initArPanel() {
        int bx = arX + 10, bw = AR_W - 20;
        int by = arY + PANEL_HEADER_H + 10;

        boolean[] st = { OBFUtilities.config.attackRemapEnabled };
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Bind touche : " + (st[0] ? "§aON" : "§cOFF")),
                btn -> {
                    st[0] = !st[0];
                    OBFUtilities.config.attackRemapEnabled = st[0];
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (st[0]) {
                        // Sauvegarder la touche originale si pas encore fait
                        if (originalAttackKey == null) {
                            originalAttackKey = client.options.attackKey.getBoundKeyTranslationKey().equals("key.mouse.left")
                                    ? net.minecraft.client.util.InputUtil.Type.MOUSE.createFromCode(0)
                                    : net.minecraft.client.util.InputUtil.fromTranslationKey(client.options.attackKey.getBoundKeyTranslationKey());
                        }
                        // Assigner la nouvelle touche directement dans Minecraft
                        client.options.attackKey.setBoundKey(
                                net.minecraft.client.util.InputUtil.fromKeyCode(OBFUtilities.config.attackRemapKey, 0));
                        net.minecraft.client.option.KeyBinding.updateKeysByCode();
                    } else {
                        // Restaurer la touche d'origine (clic gauche)
                        if (originalAttackKey != null) {
                            client.options.attackKey.setBoundKey(originalAttackKey);
                            net.minecraft.client.option.KeyBinding.updateKeysByCode();
                            originalAttackKey = null;
                        }
                    }
                    OBFUtilities.config.save();
                    btn.setMessage(Text.literal("Bind touche : " + (st[0] ? "§aON" : "§cOFF")));
                }
        ).dimensions(bx, by, bw, 18).build());
        by += 26;

        String keyName = getGlfwKeyName(OBFUtilities.config.attackRemapKey);
        addDrawableChild(ButtonWidget.builder(
                Text.literal(listeningForKey ? "§eAppuyez sur une touche..." : "§7Touche : §f" + keyName),
                btn -> { listeningForKey = true; saveAllPositions(); clearAndInit(); }
        ).dimensions(bx, by, bw, 18).build());
    }

    private String getGlfwKeyName(int key) {
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase();
        return switch (key) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE -> "ESPACE";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT -> "SHIFT G";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT -> "SHIFT D";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL -> "CTRL G";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL -> "CTRL D";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_TAB -> "TAB";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER -> "ENTREE";
            default -> "GLFW#" + key;
        };
    }

    private int computeArH() {
        return PANEL_HEADER_H + 10 + 18 + 26 + 18 + 16;
    }

    private void drawHoverBtn(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label) {
        ctx.fill(x, y, x+w, y+h, 0xBB1A1A1A);
        ctx.fill(x, y, x+w, y+1, 0xFF546E7A);
        ctx.fill(x, y+h-1, x+w, y+h, 0xFF546E7A);
        ctx.fill(x, y, x+1, y+h, 0xFF546E7A);
        ctx.fill(x+w-1, y, x+w, y+h, 0xFF546E7A);
        int tw = tr.getWidth(label);
        ctx.drawText(tr, label, x + w/2 - tw/2, y+2, 0xFFFFFFFF, false);
    }

    private int computeWpH() {
        return WP_HEADER_H + 10 + 18 + 24 + 18 + 26 + WP_VISIBLE * WP_ROW_H + 14 + 18 + 24 + 14;
    }

    private int computeLeftH() {
        return PANEL_HEADER_H + 10 + 14 + 22 + 22*4 + 26 + 18 + 24 + 18 + 18;
    }

    private int computeRightH() {
        int h = PANEL_HEADER_H + 10 + 14 + cfg.customBlocks.size()*20 + 18 + 26;
        if (addingMode) h += 18 + 24 + 20 + BLOCK_LIST_HEIGHT + 6;
        return h + 6;
    }
}