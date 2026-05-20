package com.hamtabot.obfutilities.hud;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import net.minecraft.client.MinecraftClient;
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

    private static final int RESIZE_GRIP = 8;

    private static final int BASE_LEFT_W   = 220;
    private static final int BASE_RIGHT_W  = 260;
    private static final int BASE_TOOL_W   = 200;
    private static final int BASE_DEBUG_W  = 200;
    private static final int BASE_BRIGHT_W = 210;
    private static final int BASE_WP_W     = 300;
    private static final int BASE_AR_W     = 230;

    private int s(int v, float sc) { return Math.round(v * sc); }

    private int LEFT_W()   { return s(BASE_LEFT_W,   cfg.scaleLeft);   }
    private int RIGHT_W()  { return s(BASE_RIGHT_W,  cfg.scaleRight);  }
    private int TOOL_W()   { return s(BASE_TOOL_W,   cfg.scaleTool);   }
    private int DEBUG_W()  { return s(BASE_DEBUG_W,  cfg.scaleDebug);  }
    private int BRIGHT_W() { return s(BASE_BRIGHT_W, cfg.scaleBright); }
    private int WP_W()     { return s(BASE_WP_W,     cfg.scaleWp);     }
    private int AR_W()     { return s(BASE_AR_W,      cfg.scaleAr);     }

    private int PH(float sc)  { return s(20, sc); }
    private int ROW(float sc) { return s(22, sc); }
    private int BTN(float sc) { return s(18, sc); }
    private int PAD(float sc) { return s(10, sc); }

    private static int leftX = -1, leftY = 20;
    private boolean draggingLeft = false;
    private int dragLeftOffX, dragLeftOffY;
    private boolean resizingLeft = false;
    private int resizeLeftStartX;
    private float resizeLeftStartScale;

    private static int rightX = -1, rightY = 20;
    private boolean draggingRight = false;
    private int dragRightOffX, dragRightOffY;
    private boolean resizingRight = false;
    private int resizeRightStartX;
    private float resizeRightStartScale;

    private static int toolX = -1, toolY = 20;
    private boolean draggingTool = false;
    private int dragToolOffX, dragToolOffY;
    private boolean resizingTool = false;
    private int resizeToolStartX;
    private float resizeToolStartScale;

    private static int debugX = -1, debugY = 20;
    private boolean draggingDebug = false;
    private int dragDebugOffX, dragDebugOffY;
    private boolean resizingDebug = false;
    private int resizeDebugStartX;
    private float resizeDebugStartScale;

    private static int brightX = -1, brightY = 20;
    private boolean draggingBright = false;
    private int dragBrightOffX, dragBrightOffY;
    private boolean resizingBright = false;
    private int resizeBrightStartX;
    private float resizeBrightStartScale;

    private static int wpX = -1, wpY = 20;
    private boolean draggingWp = false;
    private int dragWpOffX, dragWpOffY;
    private boolean resizingWp = false;
    private int resizeWpStartX;
    private float resizeWpStartScale;

    private static int arX = -1, arY = 20;
    private boolean draggingAr = false;
    private int dragArOffX, dragArOffY;
    private boolean resizingAr = false;
    private int resizeArStartX;
    private float resizeArStartScale;

    private boolean resizingHud = false;
    private int resizeHudStartX;
    private float resizeHudStartScale;

    private static boolean listeningForKey = false;
    private static int wpScrollOffset = 0;
    private static int wpConfirmDelete = -1;
    private static boolean addingMode = false;
    private static String  pendingMode = "posé";

    private TextFieldWidget searchField;
    private List<String> allBlocks      = new ArrayList<>();
    private List<String> filteredBlocks = new ArrayList<>();
    private int blockScrollOffset = 0;
    private boolean draggingScrollbar = false;
    private int scrollbarDragStartY = 0, scrollbarDragStartOffset = 0;

    private boolean draggingHud = false;
    private int dragHudOffX, dragHudOffY;

    private static final int BLOCK_LIST_VISIBLE = 6;
    private static final int WP_VISIBLE         = 4;

    public OBFConfigScreen(OBFHud hud) {
        super(Text.literal("OBF Utilitaire - Configuration"));
        this.hud = hud;
        this.cfg = OBFUtilities.config;
    }

    private boolean inResizeGrip(double mx, double my, int px, int py, int pw, int ph) {
        return mx >= px+pw-RESIZE_GRIP && mx <= px+pw
                && my >= py+ph-RESIZE_GRIP && my <= py+ph;
    }

    private void drawResizeGrip(DrawContext ctx, int px, int py, int pw, int ph) {
        int gx = px+pw-RESIZE_GRIP, gy = py+ph-RESIZE_GRIP;
        ctx.fill(gx, gy, px+pw, py+ph, 0x55FFD54F);
        ctx.fill(gx+2, gy+6, gx+4, gy+8, 0xAAFFD54F);
        ctx.fill(gx+4, gy+4, gx+6, gy+6, 0xAAFFD54F);
        ctx.fill(gx+6, gy+2, gx+8, gy+4, 0xAAFFD54F);
    }

    /** Retourne la position [x, y] de l'icône ? pour un panneau donné */
    private int[] getHelpIconPos(int px, int py, int pw, float sc) {
        int size = s(10, sc);
        int ix = px + pw - size - s(3, sc);
        int iy = py + (PH(sc) - size) / 2;
        return new int[]{ ix, iy };
    }

    private boolean isHelpIconHovered(double mx, double my, int px, int py, int pw, float sc) {
        int[] pos = getHelpIconPos(px, py, pw, sc);
        int size = s(10, sc);
        return mx >= pos[0] && mx <= pos[0]+size && my >= pos[1] && my <= pos[1]+size;
    }

    @Override
    protected void init() {
        if (leftX == -1) {
            if (cfg.cfgLeftX != -1) {
                leftX   = cfg.cfgLeftX;   leftY   = cfg.cfgLeftY;
                rightX  = cfg.cfgRightX;  rightY  = cfg.cfgRightY;
                toolX   = cfg.cfgToolX;   toolY   = cfg.cfgToolY;
                brightX = cfg.cfgBrightX; brightY = cfg.cfgBrightY;
                debugX  = cfg.cfgDebugX;  debugY  = cfg.cfgDebugY;
                wpX     = cfg.cfgWpX != -1 ? cfg.cfgWpX : -1; wpY = cfg.cfgWpY;
                arX     = cfg.cfgAttackRemapX != -1 ? cfg.cfgAttackRemapX : -1; arY = cfg.cfgAttackRemapY;
            } else {
                int gap = 8;
                leftX  = 10;                              leftY  = 10;
                rightX = leftX + LEFT_W() + gap;          rightY = 10;
                toolX  = rightX + RIGHT_W() + gap;        toolY  = 10;
                int row2Y = 10 + Math.max(computeLeftH(), Math.max(computeRightH(), computeToolH())) + gap;
                brightX = 10;                             brightY = row2Y;
                debugX  = brightX + BRIGHT_W() + gap;    debugY  = row2Y;
                int row3Y = row2Y + computeBrightH() + gap;
                wpX = 10; wpY = row3Y;
                arX = WP_W() + 18; arY = row3Y;
            }
        }
        com.hamtabot.obfutilities.debug.DebugOverlay.inConfigScreen = true;
        clampPanels();
        rebuildBlockList();
        initLeftPanel();
        initRightPanel();
        initToolPanel();
        initBrightPanel();
        initDebugPanel();
        initWpPanel();
        initArPanel();
    }

    private void initLeftPanel() {
        float sc = cfg.scaleLeft;
        int bx = leftX + PAD(sc), bw = LEFT_W() - PAD(sc)*2;
        int by = leftY + PH(sc) + PAD(sc) + s(14, sc);

        addToggleButton(bx, by, bw, sc, "Blocs posés", cfg.showBlocksPlaced, v -> cfg.showBlocksPlaced = v); by += ROW(sc);
        addToggleButton(bx, by, bw, sc, "Blocs minés", cfg.showBlocksMined,  v -> cfg.showBlocksMined  = v); by += ROW(sc);
        addToggleButton(bx, by, bw, sc, "Mobs tués",   cfg.showMobsKilled,   v -> cfg.showMobsKilled   = v); by += ROW(sc);
        addToggleButton(bx, by, bw, sc, "Pub timer",   cfg.showAdTimer,      v -> cfg.showAdTimer      = v); by += ROW(sc);
        by += s(4, sc);

        addDrawableChild(ButtonWidget.builder(Text.literal("Réinitialiser la session"),
                btn -> OBFUtilities.resetSession()).dimensions(bx, by, bw, BTN(sc)).build());
        by += BTN(sc) + s(6, sc);

        if (OBFUtilities.canRefreshStats()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("§bActualiser les stats globales"),
                    btn -> {
                        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("utilitiesstats");
                            OBFUtilities.onStatsRefreshed();
                            saveAllPositions(); clearAndInit();
                        }
                    }).dimensions(bx, by, bw, BTN(sc)).build());
        } else {
            long rem = OBFUtilities.getStatsRefreshCooldownRemaining();
            long m = rem / 60000, sv = (rem / 1000) % 60;
            ButtonWidget btn = ButtonWidget.builder(Text.literal("§7Attendez " + String.format("%dm %02ds", m, sv) + " avant d'actualiser"), b -> {}).dimensions(bx, by, bw, BTN(sc)).build();
            btn.active = false;
            addDrawableChild(btn);
        }
    }

    private void initRightPanel() {
        float sc = cfg.scaleRight;
        int rx = rightX + PAD(sc), rw = RIGHT_W() - PAD(sc)*2;
        int ry = rightY + PH(sc) + PAD(sc) + s(14, sc);

        for (int i = 0; i < cfg.customBlocks.size(); i++) {
            final int idx = i;
            ModConfig.CustomBlockEntry e = cfg.customBlocks.get(i);
            boolean[] st = { e.enabled };
            addDrawableChild(ButtonWidget.builder(Text.literal(st[0] ? "§aON" : "§cOFF"),
                    btn -> { st[0]=!st[0]; e.enabled=st[0]; btn.setMessage(Text.literal(st[0]?"§aON":"§cOFF")); }
            ).dimensions(rx, ry, s(36, sc), s(16, sc)).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(e.trackPlaced ? "Posé" : "Miné"),
                    btn -> { e.trackPlaced=!e.trackPlaced; btn.setMessage(Text.literal(e.trackPlaced?"Posé":"Miné")); }
            ).dimensions(rx+s(40, sc), ry, s(44, sc), s(16, sc)).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("§cX"),
                    btn -> { cfg.customBlocks.remove(idx); saveAllPositions(); clearAndInit(); }
            ).dimensions(rx+rw-s(20, sc), ry, s(20, sc), s(16, sc)).build());
            ry += s(20, sc);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("§a+ Ajouter un bloc"),
                btn -> { addingMode = !addingMode; saveAllPositions(); clearAndInit(); }
        ).dimensions(rx, ry, rw, BTN(sc)).build());
        ry += BTN(sc) + s(8, sc);

        if (addingMode) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Mode : " + pendingMode),
                    btn -> { pendingMode = pendingMode.equals("posé") ? "miné" : "posé"; btn.setMessage(Text.literal("Mode : "+pendingMode)); rebuildBlockList(); }
            ).dimensions(rx, ry, rw, BTN(sc)).build());
            ry += BTN(sc) + s(6, sc);

            searchField = new TextFieldWidget(textRenderer, rx, ry, rw, s(16, sc), Text.literal(""));
            searchField.setPlaceholder(Text.literal("Chercher un bloc..."));
            searchField.setChangedListener(str -> {
                filteredBlocks = allBlocks.stream().filter(b -> b.contains(str.toLowerCase())).collect(Collectors.toList());
                blockScrollOffset = 0;
            });
            addDrawableChild(searchField);
            searchField.setEditable(true);
            setFocused(searchField);
        }
    }

    private void initDebugPanel() {
        float sc = cfg.scaleDebug;
        int bx = debugX + PAD(sc), bw = DEBUG_W() - PAD(sc)*2;
        int by = debugY + PH(sc) + PAD(sc) + s(14, sc);

        boolean[] stFps = { cfg.debugShowFps };
        addDrawableChild(ButtonWidget.builder(Text.literal("FPS : " + (stFps[0] ? "§aON" : "§cOFF")),
                btn -> { stFps[0]=!stFps[0]; cfg.debugShowFps=stFps[0]; btn.setMessage(Text.literal("FPS : "+(stFps[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, BTN(sc)).build()); by += ROW(sc);

        boolean[] stCoords = { cfg.debugShowCoords };
        addDrawableChild(ButtonWidget.builder(Text.literal("Coordonnées : " + (stCoords[0] ? "§aON" : "§cOFF")),
                btn -> { stCoords[0]=!stCoords[0]; cfg.debugShowCoords=stCoords[0]; btn.setMessage(Text.literal("Coordonnées : "+(stCoords[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, BTN(sc)).build()); by += ROW(sc);

        boolean[] stRam = { cfg.debugShowRam };
        addDrawableChild(ButtonWidget.builder(Text.literal("RAM : " + (stRam[0] ? "§aON" : "§cOFF")),
                btn -> { stRam[0]=!stRam[0]; cfg.debugShowRam=stRam[0]; btn.setMessage(Text.literal("RAM : "+(stRam[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, BTN(sc)).build());
    }

    private void initBrightPanel() {
        float sc = cfg.scaleBright;
        int bx = brightX + PAD(sc), bw = BRIGHT_W() - PAD(sc)*2;
        int by = brightY + PH(sc) + PAD(sc) + s(14, sc);

        boolean[] stFB = { cfg.fullBrightEnabled };
        addDrawableChild(ButtonWidget.builder(Text.literal("Night Vision : " + (stFB[0] ? "§aON" : "§cOFF")),
                btn -> { stFB[0]=!stFB[0]; cfg.fullBrightEnabled=stFB[0]; btn.setMessage(Text.literal("Night Vision : "+(stFB[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, BTN(sc)).build());
    }

    private void initToolPanel() {
        float sc = cfg.scaleTool;
        int bx = toolX + PAD(sc), bw = TOOL_W() - PAD(sc)*2;
        int by = toolY + PH(sc) + PAD(sc) + s(14, sc);

        boolean[] stEnabled = { cfg.autoToolEnabled };
        addDrawableChild(ButtonWidget.builder(Text.literal("AutoTool : " + (stEnabled[0] ? "§aON" : "§cOFF")),
                btn -> { stEnabled[0]=!stEnabled[0]; cfg.autoToolEnabled=stEnabled[0]; btn.setMessage(Text.literal("AutoTool : "+(stEnabled[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, BTN(sc)).build()); by += BTN(sc) + s(6, sc);

        boolean[] stDura = { cfg.autoToolSkipLowDurability };
        addDrawableChild(ButtonWidget.builder(Text.literal("Skip durabilité ≤10 : " + (stDura[0] ? "§aON" : "§cOFF")),
                btn -> { stDura[0]=!stDura[0]; cfg.autoToolSkipLowDurability=stDura[0]; btn.setMessage(Text.literal("Skip durabilité ≤10 : "+(stDura[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, BTN(sc)).build()); by += BTN(sc) + s(10, sc) + s(14, sc);

        int hw = (bw - s(4, sc)) / 2;
        addToolToggle(bx,            by, hw, sc, "⛏ Pioche",   cfg.autoToolUsePioche,   v -> cfg.autoToolUsePioche   = v);
        addToolToggle(bx+hw+s(4,sc), by, hw, sc, "🪣 Pelle",   cfg.autoToolUsePelle,    v -> cfg.autoToolUsePelle    = v); by += s(20, sc);
        addToolToggle(bx,            by, hw, sc, "🪓 Hache",    cfg.autoToolUseHache,    v -> cfg.autoToolUseHache    = v);
        addToolToggle(bx+hw+s(4,sc), by, hw, sc, "🌾 Houe",    cfg.autoToolUseHoue,     v -> cfg.autoToolUseHoue     = v); by += s(20, sc);
        addToolToggle(bx,            by, hw, sc, "✂ Cisaille", cfg.autoToolUseCisaille, v -> cfg.autoToolUseCisaille = v);
        addToolToggle(bx+hw+s(4,sc), by, hw, sc, "⚔ Épée",    cfg.autoToolUseEpee,     v -> cfg.autoToolUseEpee     = v);
    }

    private void initWpPanel() {
        float sc = cfg.scaleWp;
        int bx = wpX + PAD(sc), bw = WP_W() - PAD(sc)*2;
        int by = wpY + PH(sc) + PAD(sc);

        boolean[] st = { cfg.waypointsEnabled };
        addDrawableChild(ButtonWidget.builder(Text.literal("Waypoints : " + (st[0] ? "§aON" : "§cOFF")),
                btn -> { st[0]=!st[0]; cfg.waypointsEnabled=st[0]; btn.setMessage(Text.literal("Waypoints : "+(st[0]?"§aON":"§cOFF"))); }
        ).dimensions(bx, by, bw, BTN(sc)).build()); by += BTN(sc) + s(6, sc);

        String keyName = OBFUtilities.keyAddWaypoint != null ? OBFUtilities.keyAddWaypoint.getBoundKeyLocalizedText().getString() : "N";
        addDrawableChild(ButtonWidget.builder(Text.literal("§a+ Créer un waypoint  [" + keyName + "]"),
                btn -> MinecraftClient.getInstance().setScreen(new com.hamtabot.obfutilities.waypoint.WaypointCreateScreen(() -> MinecraftClient.getInstance().setScreen(this)))
        ).dimensions(bx, by, bw, BTN(sc)).build());
        by += BTN(sc) + s(8, sc) + WP_VISIBLE * s(36, sc) + s(14, sc);

        addDrawableChild(ButtonWidget.builder(Text.literal("§7📁 Ouvrir le dossier des waypoints"),
                btn -> {
                    try {
                        java.io.File configDir = new java.io.File("config").getAbsoluteFile();
                        String os = System.getProperty("os.name").toLowerCase();
                        if (os.contains("win")) new ProcessBuilder("explorer.exe", configDir.getAbsolutePath()).start();
                        else if (os.contains("mac")) Runtime.getRuntime().exec(new String[]{"open", configDir.getAbsolutePath()});
                        else Runtime.getRuntime().exec(new String[]{"xdg-open", configDir.getAbsolutePath()});
                    } catch (Exception e) { OBFUtilities.LOGGER.error("[OBF] " + e.getMessage()); }
                }).dimensions(bx, by, bw, BTN(sc)).build());
    }

    private static net.minecraft.client.util.InputUtil.Key originalAttackKey = null;

    private void initArPanel() {
        float sc = cfg.scaleAr;
        int bx = arX + PAD(sc), bw = AR_W() - PAD(sc)*2;
        int by = arY + PH(sc) + PAD(sc);

        boolean[] st = { cfg.attackRemapEnabled };
        addDrawableChild(ButtonWidget.builder(Text.literal("Bind touche : " + (st[0] ? "§aON" : "§cOFF")),
                btn -> {
                    st[0] = !st[0]; cfg.attackRemapEnabled = st[0];
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (st[0]) {
                        if (originalAttackKey == null)
                            originalAttackKey = client.options.attackKey.getBoundKeyTranslationKey().equals("key.mouse.left")
                                    ? net.minecraft.client.util.InputUtil.Type.MOUSE.createFromCode(0)
                                    : net.minecraft.client.util.InputUtil.fromTranslationKey(client.options.attackKey.getBoundKeyTranslationKey());
                        client.options.attackKey.setBoundKey(net.minecraft.client.util.InputUtil.fromKeyCode(cfg.attackRemapKey, 0));
                        net.minecraft.client.option.KeyBinding.updateKeysByCode();
                    } else {
                        if (originalAttackKey != null) {
                            client.options.attackKey.setBoundKey(originalAttackKey);
                            net.minecraft.client.option.KeyBinding.updateKeysByCode();
                            originalAttackKey = null;
                        }
                    }
                    cfg.save(); btn.setMessage(Text.literal("Bind touche : " + (st[0] ? "§aON" : "§cOFF")));
                }
        ).dimensions(bx, by, bw, BTN(sc)).build()); by += BTN(sc) + s(8, sc);

        addDrawableChild(ButtonWidget.builder(
                Text.literal(listeningForKey ? "§eAppuyez sur une touche..." : "§7Touche : §f" + getGlfwKeyName(cfg.attackRemapKey)),
                btn -> { listeningForKey = true; saveAllPositions(); clearAndInit(); }
        ).dimensions(bx, by, bw, BTN(sc)).build());
    }

    private void addToolToggle(int x, int y, int w, float sc, String label, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        boolean[] state = { initial };
        addDrawableChild(ButtonWidget.builder(Text.literal((state[0] ? "§a" : "§c") + label),
                btn -> { state[0]=!state[0]; onChange.accept(state[0]); btn.setMessage(Text.literal((state[0]?"§a":"§c")+label)); }
        ).dimensions(x, y, w, s(16, sc)).build());
    }

    private void addToggleButton(int x, int y, int w, float sc, String label, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        boolean[] state = { initial };
        addDrawableChild(ButtonWidget.builder(Text.literal(label + " : " + (state[0] ? "§aON" : "§cOFF")),
                btn -> { state[0]=!state[0]; onChange.accept(state[0]); btn.setMessage(Text.literal(label+" : "+(state[0]?"§aON":"§cOFF"))); }
        ).dimensions(x, y, w, BTN(sc)).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        hud.render(context, delta);

        renderLeftPanel(context, mouseX, mouseY);
        renderRightPanel(context, mouseX, mouseY);
        renderDebugPanel(context);
        renderBrightPanel(context);
        renderToolPanel(context);
        renderArPanel(context);
        renderWpPanel(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
        com.hamtabot.obfutilities.debug.DebugOverlay.render(context, delta);

        // Tooltips des ? — rendu après super.render pour passer par-dessus les boutons
        renderHelpTooltipIfHovered(context, mouseX, mouseY, leftX,   leftY,   LEFT_W(),   cfg.scaleLeft);
        renderHelpTooltipIfHovered(context, mouseX, mouseY, rightX,  rightY,  RIGHT_W(),  cfg.scaleRight);
        renderHelpTooltipIfHovered(context, mouseX, mouseY, toolX,   toolY,   TOOL_W(),   cfg.scaleTool);
        renderHelpTooltipIfHovered(context, mouseX, mouseY, brightX, brightY, BRIGHT_W(), cfg.scaleBright);
        renderHelpTooltipIfHovered(context, mouseX, mouseY, debugX,  debugY,  DEBUG_W(),  cfg.scaleDebug);
        renderHelpTooltipIfHovered(context, mouseX, mouseY, wpX,     wpY,     WP_W(),     cfg.scaleWp);
        renderHelpTooltipIfHovered(context, mouseX, mouseY, arX,     arY,     AR_W(),     cfg.scaleAr);
    }

    private void renderHelpTooltipIfHovered(DrawContext context, int mouseX, int mouseY,
                                            int px, int py, int pw, float sc) {
        if (isHelpIconHovered(mouseX, mouseY, px, py, pw, sc)) {
            List<Text> lines = java.util.Arrays.asList(
                    Text.literal("§eGlisser §7— maintenir clic gauche sur l'en-tête"),
                    Text.literal("§e↘ Redimensionner §7— glisser le coin bas-droit")
            );
            context.drawTooltip(textRenderer, lines, mouseX, mouseY);
        }
    }

    private void renderLeftPanel(DrawContext ctx, int mouseX, int mouseY) {
        float sc = cfg.scaleLeft;
        int h = computeLeftH();
        drawPanel(ctx, leftX, leftY, LEFT_W(), h, sc, "§e◆ Paramètres");
        ctx.drawText(textRenderer, "§7Sections à afficher :", leftX+PAD(sc), leftY+PH(sc)+PAD(sc), 0xFFB0BEC5, false);
        ctx.drawText(textRenderer, "§7[Échap] Sauvegarder", leftX+PAD(sc), leftY+h-s(14, sc), 0xFF546E7A, false);
        int noteY = leftY + h + s(6, sc);
        ctx.drawText(textRenderer, "§f⚠ Les totaux sont calculés à la connexion.", leftX+PAD(sc), noteY, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, "§7Pour recalculer, utilisez le bouton Actualiser.", leftX+PAD(sc), noteY+s(10, sc), 0xFFB0BEC5, false);
    }

    private void renderRightPanel(DrawContext ctx, int mouseX, int mouseY) {
        float sc = cfg.scaleRight;
        drawPanel(ctx, rightX, rightY, RIGHT_W(), computeRightH(), sc, "§d◆ Blocs custom");
        int ty = rightY + PH(sc) + PAD(sc);
        ctx.drawText(textRenderer, "§7Blocs trackés :", rightX+PAD(sc), ty, 0xFFB0BEC5, false);
        ty += s(14, sc);
        int rx = rightX + PAD(sc);
        for (ModConfig.CustomBlockEntry e : cfg.customBlocks) {
            try { ctx.drawItem(new ItemStack(Registries.ITEM.get(new Identifier(e.blockId))), rx+s(86, sc), ty); } catch (Exception ignored) {}
            ctx.drawText(textRenderer, "§7"+formatBlockName(e.blockId), rx+s(104, sc), ty+s(4, sc), 0xFFB0BEC5, false);
            ty += s(20, sc);
        }
        ty += BTN(sc) + s(8, sc);
        if (addingMode) {
            ty += BTN(sc) + s(6, sc) + BTN(sc) + s(6, sc);
            int listX = rightX + PAD(sc), listW = RIGHT_W() - PAD(sc)*2;
            int listY = getRightListY();
            int rowH = s(18, sc), listH = BLOCK_LIST_VISIBLE * rowH;
            ctx.fill(listX, listY, listX+listW, listY+listH, 0xFF0A0A0A);
            ctx.fill(listX, listY, listX+listW, listY+1, 0xFF333333);
            ctx.fill(listX, listY+listH-1, listX+listW, listY+listH, 0xFF333333);
            ctx.fill(listX, listY, listX+1, listY+listH, 0xFF333333);
            ctx.fill(listX+listW-1, listY, listX+listW, listY+listH, 0xFF333333);
            for (int i = 0; i < BLOCK_LIST_VISIBLE; i++) {
                int idx = i + blockScrollOffset;
                if (idx >= filteredBlocks.size()) break;
                String blockId = filteredBlocks.get(idx);
                int rowY = listY + i * rowH;
                if (mouseX >= listX && mouseX <= listX+listW-s(9, sc) && mouseY >= rowY && mouseY < rowY+rowH)
                    ctx.fill(listX+1, rowY, listX+listW-s(9, sc), rowY+rowH, 0xFF151515);
                try { ctx.drawItem(new ItemStack(Registries.ITEM.get(new Identifier(blockId))), listX+s(2, sc), rowY+1); } catch (Exception ignored) {}
                String dn = blockId.contains(":") ? blockId.split(":")[1] : blockId;
                ctx.drawText(textRenderer, dn, listX+s(22, sc), rowY+s(5, sc), 0xFFB0BEC5, false);
                if (i < BLOCK_LIST_VISIBLE-1) ctx.fill(listX+1, rowY+rowH-1, listX+listW-s(9, sc), rowY+rowH, 0xFF111111);
            }
            if (filteredBlocks.size() > BLOCK_LIST_VISIBLE) {
                int max = filteredBlocks.size() - BLOCK_LIST_VISIBLE;
                int sbH = Math.max(s(20, sc), listH * BLOCK_LIST_VISIBLE / filteredBlocks.size());
                int sbY = listY + (max > 0 ? blockScrollOffset * (listH-sbH) / max : 0);
                ctx.fill(listX+listW-s(9, sc), listY, listX+listW-1, listY+listH, 0xFF1A1A1A);
                ctx.fill(listX+listW-s(9, sc), sbY, listX+listW-1, sbY+sbH, 0xFF4FC3F7);
            }
        }
    }

    private void renderDebugPanel(DrawContext ctx) {
        float sc = cfg.scaleDebug;
        drawPanel(ctx, debugX, debugY, DEBUG_W(), computeDebugH(), sc, "§c◆ Débug");
        ctx.drawText(textRenderer, "§7Overlays de débug :", debugX+PAD(sc), debugY+PH(sc)+PAD(sc), 0xFFB0BEC5, false);
    }

    private void renderBrightPanel(DrawContext ctx) {
        float sc = cfg.scaleBright;
        drawPanel(ctx, brightX, brightY, BRIGHT_W(), computeBrightH(), sc, "§b◆ FullBright");
        ctx.drawText(textRenderer, "§7Active le Night Vision localement.", brightX+PAD(sc), brightY+PH(sc)+PAD(sc), 0xFFB0BEC5, false);
    }

    private void renderToolPanel(DrawContext ctx) {
        float sc = cfg.scaleTool;
        drawPanel(ctx, toolX, toolY, TOOL_W(), computeToolH(), sc, "§6◆ AutoTool");
        int tty = toolY + PH(sc) + PAD(sc);
        ctx.drawText(textRenderer, "§7Sélection automatique d'outil", toolX+PAD(sc), tty, 0xFFB0BEC5, false);
        tty += s(14, sc) + BTN(sc) + s(6, sc) + BTN(sc) + s(10, sc) + s(14, sc);
        ctx.drawText(textRenderer, "§7Outils à utiliser :", toolX+PAD(sc), tty, 0xFFB0BEC5, false);
    }

    private void renderArPanel(DrawContext ctx) {
        float sc = cfg.scaleAr;
        drawPanel(ctx, arX, arY, AR_W(), computeArH(), sc, "§a◆ Bind Touche");
        int arDesc = arY + PH(sc) + PAD(sc) + BTN(sc) + s(8, sc) + BTN(sc) + s(4, sc);
        String arStatus = cfg.attackRemapEnabled ? "§aActif — touche : §f"+getGlfwKeyName(cfg.attackRemapKey) : "§7Inactif — clic gauche par défaut";
        ctx.drawText(textRenderer, arStatus, arX+PAD(sc), arDesc, 0xFFB0BEC5, false);
    }

    private void renderWpPanel(DrawContext ctx, int mouseX, int mouseY) {
        float sc = cfg.scaleWp;
        drawPanel(ctx, wpX, wpY, WP_W(), computeWpH(), sc, "§6◆ Waypoints");
        int wty = wpY + PH(sc) + PAD(sc) + BTN(sc) + s(6, sc) + BTN(sc) + s(8, sc);
        java.util.List<com.hamtabot.obfutilities.waypoint.Waypoint> wpAll = com.hamtabot.obfutilities.waypoint.WaypointManager.getAll();
        int rowH = s(36, sc), wpListH = WP_VISIBLE * rowH;

        if (wpAll.isEmpty()) ctx.drawText(textRenderer, "§7Aucun waypoint. Appuyez sur + Créer.", wpX+PAD(sc), wty+PAD(sc), 0xFFB0BEC5, false);

        for (int i = 0; i < WP_VISIBLE && i + wpScrollOffset < wpAll.size(); i++) {
            int idx = i + wpScrollOffset;
            com.hamtabot.obfutilities.waypoint.Waypoint wp = wpAll.get(idx);
            int rowY = wty + i * rowH;
            boolean cur = com.hamtabot.obfutilities.waypoint.WaypointManager.getCurrentDimension().equals(wp.dimension);
            boolean hovered = mouseX >= wpX+PAD(sc) && mouseX <= wpX+WP_W()-s(14, sc) && mouseY >= rowY && mouseY < rowY+rowH-s(2, sc);
            ctx.fill(wpX+PAD(sc), rowY, wpX+WP_W()-s(14, sc), rowY+rowH-s(2, sc), hovered ? 0x441A3A6A : (cur ? 0x221A3A4A : 0x11FFFFFF));
            ctx.fill(wpX+s(14, sc), rowY+s(6, sc), wpX+s(24, sc), rowY+s(16, sc), wp.color);
            ctx.drawText(textRenderer, wp.enabled ? "§a●" : "§8●", wpX+s(28, sc), rowY+s(4, sc), 0xFFFFFFFF, false);
            ctx.drawTextWithShadow(textRenderer, "§f"+wp.name, wpX+s(40, sc), rowY+s(4, sc), wp.color);
            ctx.drawText(textRenderer, "§7"+wp.x+", "+wp.y+", "+wp.z+"  §8["+wp.getDimShort()+"]", wpX+s(40, sc), rowY+s(15, sc), 0xFF888888, false);
            if (hovered) {
                int btnY = rowY + rowH - s(18, sc);
                int btnW2 = s(64, sc), gap2 = s(4, sc);
                drawHoverBtn(ctx, textRenderer, wpX+s(14, sc), btnY, btnW2, s(14, sc), wp.enabled ? "§aVisible" : "§8Masqué");
                if (wpConfirmDelete == idx) drawHoverBtn(ctx, textRenderer, wpX+s(14, sc)+(btnW2+gap2)*2, btnY, btnW2+s(20, sc), s(14, sc), "§cConfirmer ?");
                else drawHoverBtn(ctx, textRenderer, wpX+s(14, sc)+(btnW2+gap2)*2, btnY, btnW2, s(14, sc), "§c✗ Suppr.");
            }
        }
        ctx.fill(wpX+WP_W()-s(12, sc), wty, wpX+WP_W()-s(2, sc), wty+wpListH, 0xFF1A1A1A);
        if (!wpAll.isEmpty()) {
            int total = Math.max(wpAll.size(), WP_VISIBLE);
            int sbH = Math.max(s(20, sc), wpListH * WP_VISIBLE / total);
            int maxSc2 = Math.max(0, wpAll.size() - WP_VISIBLE);
            int sbY = wty + (maxSc2 > 0 ? wpScrollOffset * (wpListH-sbH) / maxSc2 : 0);
            ctx.fill(wpX+WP_W()-s(12, sc), sbY, wpX+WP_W()-s(2, sc), sbY+sbH, 0xFF4FC3F7);
        }
    }

    private void drawPanel(DrawContext ctx, int x, int y, int w, int h, float sc, String title) {
        ctx.fill(x, y, x+w, y+h, 0xEE0D0D0D);
        ctx.fill(x, y, x+w, y+1, 0xFFFFD54F);
        ctx.fill(x, y+h-1, x+w, y+h, 0xFFFFD54F);
        ctx.fill(x, y, x+1, y+h, 0xFFFFD54F);
        ctx.fill(x+w-1, y, x+w, y+h, 0xFFFFD54F);
        ctx.fill(x+1, y+1, x+w-1, y+PH(sc), 0xFF1A1A1A);
        ctx.drawTextWithShadow(textRenderer, title, x+s(8, sc), y+s(6, sc), 0xFFFFD54F);
        int size = s(10, sc);
        int[] pos = getHelpIconPos(x, y, w, sc);
        int ix = pos[0], iy = pos[1];
        ctx.fill(ix, iy, ix+size, iy+size, 0xFF6B1010);
        ctx.fill(ix,        iy,        ix+size, iy+1,      0xFFCC2222);
        ctx.fill(ix,        iy+size-1, ix+size, iy+size,   0xFFCC2222);
        ctx.fill(ix,        iy,        ix+1,    iy+size,   0xFFCC2222);
        ctx.fill(ix+size-1, iy,        ix+size, iy+size,   0xFFCC2222);
        ctx.drawText(textRenderer, "§c?", ix + Math.max(1, (size - textRenderer.getWidth("?")) / 2), iy+s(1, sc), 0xFFFF4444, false);
        drawResizeGrip(ctx, x, y, w, h);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inResizeGrip(mouseX, mouseY, leftX,   leftY,   LEFT_W(),   computeLeftH()))   { resizingLeft   = true; resizeLeftStartX   = (int)mouseX; resizeLeftStartScale   = cfg.scaleLeft;   return true; }
            if (inResizeGrip(mouseX, mouseY, rightX,  rightY,  RIGHT_W(),  computeRightH()))  { resizingRight  = true; resizeRightStartX  = (int)mouseX; resizeRightStartScale  = cfg.scaleRight;  return true; }
            if (inResizeGrip(mouseX, mouseY, toolX,   toolY,   TOOL_W(),   computeToolH()))   { resizingTool   = true; resizeToolStartX   = (int)mouseX; resizeToolStartScale   = cfg.scaleTool;   return true; }
            if (inResizeGrip(mouseX, mouseY, debugX,  debugY,  DEBUG_W(),  computeDebugH()))  { resizingDebug  = true; resizeDebugStartX  = (int)mouseX; resizeDebugStartScale  = cfg.scaleDebug;  return true; }
            if (inResizeGrip(mouseX, mouseY, brightX, brightY, BRIGHT_W(), computeBrightH())) { resizingBright = true; resizeBrightStartX = (int)mouseX; resizeBrightStartScale = cfg.scaleBright; return true; }
            if (inResizeGrip(mouseX, mouseY, wpX,     wpY,     WP_W(),     computeWpH()))     { resizingWp     = true; resizeWpStartX     = (int)mouseX; resizeWpStartScale     = cfg.scaleWp;     return true; }
            if (inResizeGrip(mouseX, mouseY, arX,     arY,     AR_W(),     computeArH()))     { resizingAr     = true; resizeArStartX     = (int)mouseX; resizeArStartScale     = cfg.scaleAr;     return true; }

            if (mouseX >= leftX  && mouseX <= leftX+LEFT_W()   && mouseY >= leftY   && mouseY <= leftY+PH(cfg.scaleLeft))    { draggingLeft   = true; dragLeftOffX  =(int)mouseX-leftX;   dragLeftOffY  =(int)mouseY-leftY;   return true; }
            if (mouseX >= rightX && mouseX <= rightX+RIGHT_W() && mouseY >= rightY  && mouseY <= rightY+PH(cfg.scaleRight))  { draggingRight  = true; dragRightOffX =(int)mouseX-rightX;  dragRightOffY =(int)mouseY-rightY;  return true; }
            if (mouseX >= debugX && mouseX <= debugX+DEBUG_W() && mouseY >= debugY  && mouseY <= debugY+PH(cfg.scaleDebug))  { draggingDebug  = true; dragDebugOffX =(int)mouseX-debugX;  dragDebugOffY =(int)mouseY-debugY;  return true; }
            if (mouseX >= brightX && mouseX <= brightX+BRIGHT_W() && mouseY >= brightY && mouseY <= brightY+PH(cfg.scaleBright)) { draggingBright = true; dragBrightOffX=(int)mouseX-brightX; dragBrightOffY=(int)mouseY-brightY; return true; }
            if (mouseX >= toolX  && mouseX <= toolX+TOOL_W()   && mouseY >= toolY   && mouseY <= toolY+PH(cfg.scaleTool))    { draggingTool   = true; dragToolOffX  =(int)mouseX-toolX;   dragToolOffY  =(int)mouseY-toolY;   return true; }
            if (mouseX >= wpX    && mouseX <= wpX+WP_W()       && mouseY >= wpY     && mouseY <= wpY+PH(cfg.scaleWp))        { draggingWp     = true; dragWpOffX    =(int)mouseX-wpX;     dragWpOffY    =(int)mouseY-wpY;     return true; }
            if (mouseX >= arX    && mouseX <= arX+AR_W()       && mouseY >= arY     && mouseY <= arY+PH(cfg.scaleAr))        { draggingAr     = true; dragArOffX    =(int)mouseX-arX;     dragArOffY    =(int)mouseY-arY;     return true; }
        }

        if (addingMode) {
            float sc = cfg.scaleRight;
            int listX = rightX + PAD(sc), listW = RIGHT_W() - PAD(sc)*2;
            int rowH = s(18, sc), listH = BLOCK_LIST_VISIBLE * rowH;
            int listY = getRightListY();
            if (mouseX >= listX+listW-s(9, sc) && mouseX <= listX+listW && mouseY >= listY && mouseY < listY+listH) {
                draggingScrollbar = true; scrollbarDragStartY=(int)mouseY; scrollbarDragStartOffset=blockScrollOffset; return true;
            }
            if (mouseX >= listX && mouseX <= listX+listW-s(9, sc) && mouseY >= listY && mouseY < listY+listH) {
                int row = ((int)mouseY - listY) / rowH + blockScrollOffset;
                if (row >= 0 && row < filteredBlocks.size()) {
                    cfg.customBlocks.add(new ModConfig.CustomBlockEntry(filteredBlocks.get(row), pendingMode.equals("posé")));
                    addingMode = false; saveAllPositions(); clearAndInit(); return true;
                }
            }
        }

        if (button == 0) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                if (cfg.debugShowFps)    { int[] b = com.hamtabot.obfutilities.debug.DebugOverlay.getFpsBounds(textRenderer, mc);    if (mouseX>=b[0]&&mouseX<=b[0]+b[2]&&mouseY>=b[1]&&mouseY<=b[1]+b[3]) { com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingFps=true;    com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragFpsOffX=(int)mouseX-b[0];    com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragFpsOffY=(int)mouseY-b[1];    return true; } }
                if (cfg.debugShowCoords) { int[] b = com.hamtabot.obfutilities.debug.DebugOverlay.getCoordsBounds(textRenderer, mc); if (mouseX>=b[0]&&mouseX<=b[0]+b[2]&&mouseY>=b[1]&&mouseY<=b[1]+b[3]) { com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingCoords=true; com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragCoordsOffX=(int)mouseX-b[0]; com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragCoordsOffY=(int)mouseY-b[1]; return true; } }
                if (cfg.debugShowRam)    { int[] b = com.hamtabot.obfutilities.debug.DebugOverlay.getRamBounds(textRenderer);         if (mouseX>=b[0]&&mouseX<=b[0]+b[2]&&mouseY>=b[1]&&mouseY<=b[1]+b[3]) { com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingRam=true;    com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragRamOffX=(int)mouseX-b[0];    com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragRamOffY=(int)mouseY-b[1];    return true; } }
            }
        }

        if (button == 0) {
            int hx = hud.getPosX(), hy = hud.getPosY();
            int hudW = s(220, cfg.scaleHud);
            int hudH = hud.getComputedPanelHeight();
            if (inResizeGrip(mouseX, mouseY, hx, hy, hudW, hudH)) {
                resizingHud = true; resizeHudStartX = (int)mouseX; resizeHudStartScale = cfg.scaleHud; return true;
            }
            if (mouseX >= hx && mouseX <= hx+hudW && mouseY >= hy && mouseY <= hy+hudH) {
                boolean inLeft  = mouseX>=leftX && mouseX<=leftX+LEFT_W()   && mouseY>=leftY  && mouseY<=leftY+computeLeftH();
                boolean inRight = mouseX>=rightX && mouseX<=rightX+RIGHT_W() && mouseY>=rightY && mouseY<=rightY+computeRightH();
                if (!inLeft && !inRight) { draggingHud=true; dragHudOffX=(int)mouseX-hx; dragHudOffY=(int)mouseY-hy; return true; }
            }
        }

        if (button == 0) {
            float sc = cfg.scaleWp;
            int wty2 = wpY + PH(sc) + PAD(sc) + BTN(sc) + s(6, sc) + BTN(sc) + s(8, sc);
            int rowH = s(36, sc), wlH = WP_VISIBLE * rowH;
            java.util.List<com.hamtabot.obfutilities.waypoint.Waypoint> wAll = com.hamtabot.obfutilities.waypoint.WaypointManager.getAll();
            if (wAll.size() > WP_VISIBLE && mouseX >= wpX+WP_W()-s(12, sc) && mouseX <= wpX+WP_W()-s(2, sc) && mouseY >= wty2 && mouseY < wty2+wlH) {
                wpScrollOffset = Math.max(0, Math.min((int)(((mouseY-wty2)/(double)wlH)*wAll.size()), wAll.size()-WP_VISIBLE)); return true;
            }
            int btnW2 = s(64, sc), gap2 = s(4, sc);
            for (int i = 0; i < WP_VISIBLE && i+wpScrollOffset < wAll.size(); i++) {
                int idx = i+wpScrollOffset;
                com.hamtabot.obfutilities.waypoint.Waypoint wp = wAll.get(idx);
                int rowY = wty2 + i*rowH;
                boolean hovered = mouseX>=wpX+PAD(sc) && mouseX<=wpX+WP_W()-s(14, sc) && mouseY>=rowY && mouseY<rowY+rowH-s(2, sc);
                if (!hovered) continue;
                int btnY = rowY + rowH - s(18, sc);
                if (mouseX>=wpX+s(14, sc) && mouseX<=wpX+s(14, sc)+btnW2 && mouseY>=btnY && mouseY<btnY+s(14, sc)) { wp.enabled=!wp.enabled; com.hamtabot.obfutilities.waypoint.WaypointManager.save(); return true; }
                if (mouseX>=wpX+s(14, sc)+btnW2+gap2 && mouseY>=btnY && mouseY<btnY+s(14, sc)) {
                    if (wpConfirmDelete==idx) { com.hamtabot.obfutilities.waypoint.WaypointManager.remove(idx); wpConfirmDelete=-1; wpScrollOffset=Math.max(0, wpScrollOffset-1); }
                    else wpConfirmDelete=idx;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button != 0) return super.mouseDragged(mouseX, mouseY, button, dx, dy);

        if (resizingLeft)   { cfg.scaleLeft   = ModConfig.clampScale(resizeLeftStartScale   + ((int)mouseX - resizeLeftStartX)   / 200f); saveAllPositions(); clearAndInit(); return true; }
        if (resizingRight)  { cfg.scaleRight  = ModConfig.clampScale(resizeRightStartScale  + ((int)mouseX - resizeRightStartX)  / 200f); saveAllPositions(); clearAndInit(); return true; }
        if (resizingTool)   { cfg.scaleTool   = ModConfig.clampScale(resizeToolStartScale   + ((int)mouseX - resizeToolStartX)   / 200f); saveAllPositions(); clearAndInit(); return true; }
        if (resizingDebug)  { cfg.scaleDebug  = ModConfig.clampScale(resizeDebugStartScale  + ((int)mouseX - resizeDebugStartX)  / 200f); saveAllPositions(); clearAndInit(); return true; }
        if (resizingBright) { cfg.scaleBright = ModConfig.clampScale(resizeBrightStartScale + ((int)mouseX - resizeBrightStartX) / 200f); saveAllPositions(); clearAndInit(); return true; }
        if (resizingWp)     { cfg.scaleWp     = ModConfig.clampScale(resizeWpStartScale     + ((int)mouseX - resizeWpStartX)     / 200f); saveAllPositions(); clearAndInit(); return true; }
        if (resizingAr)     { cfg.scaleAr     = ModConfig.clampScale(resizeArStartScale     + ((int)mouseX - resizeArStartX)     / 200f); saveAllPositions(); clearAndInit(); return true; }
        if (resizingHud)    { cfg.scaleHud    = ModConfig.clampScale(resizeHudStartScale    + ((int)mouseX - resizeHudStartX)    / 200f); return true; }

        if (draggingScrollbar) {
            float sc = cfg.scaleRight;
            int rowH = s(18, sc), listH = BLOCK_LIST_VISIBLE * rowH;
            int max = Math.max(0, filteredBlocks.size()-BLOCK_LIST_VISIBLE);
            if (max > 0) blockScrollOffset = Math.max(0, Math.min(scrollbarDragStartOffset + (int)((mouseY-scrollbarDragStartY)/((double)listH/filteredBlocks.size())), max));
            return true;
        }

        if (com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingFps)    { cfg.debugFpsX    = (int)mouseX - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragFpsOffX;    cfg.debugFpsY    = (int)mouseY - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragFpsOffY;    return true; }
        if (com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingCoords) { cfg.debugCoordsX = (int)mouseX - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragCoordsOffX; cfg.debugCoordsY = (int)mouseY - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragCoordsOffY; return true; }
        if (com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingRam)    { cfg.debugRamX    = (int)mouseX - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragRamOffX;    cfg.debugRamY    = (int)mouseY - com.hamtabot.obfutilities.debug.DebugOverlay.cfgDragRamOffY;    return true; }

        if (listeningForKey) { listeningForKey = false; saveAllPositions(); clearAndInit(); return true; }
        if (draggingWp)     { wpX = Math.max(0, Math.min((int)mouseX-dragWpOffX, this.width-WP_W())); wpY = Math.max(0, Math.min((int)mouseY-dragWpOffY, this.height-computeWpH())); cfg.cfgWpX=wpX; cfg.cfgWpY=wpY; saveAllPositions(); clearAndInit(); return true; }
        if (draggingAr)     { arX = Math.max(0, Math.min((int)mouseX-dragArOffX, this.width-AR_W())); arY = Math.max(0, Math.min((int)mouseY-dragArOffY, this.height-computeArH())); cfg.cfgAttackRemapX=arX; cfg.cfgAttackRemapY=arY; saveAllPositions(); clearAndInit(); return true; }
        if (draggingDebug)  { debugX = Math.max(0, Math.min((int)mouseX-dragDebugOffX, this.width-DEBUG_W())); debugY = Math.max(0, Math.min((int)mouseY-dragDebugOffY, this.height-computeDebugH())); cfg.cfgDebugX=debugX; cfg.cfgDebugY=debugY; saveAllPositions(); clearAndInit(); return true; }
        if (draggingBright) { brightX = Math.max(0, Math.min((int)mouseX-dragBrightOffX, this.width-BRIGHT_W())); brightY = Math.max(0, Math.min((int)mouseY-dragBrightOffY, this.height-computeBrightH())); cfg.cfgBrightX=brightX; cfg.cfgBrightY=brightY; saveAllPositions(); clearAndInit(); return true; }
        if (draggingTool)   { toolX = Math.max(0, Math.min((int)mouseX-dragToolOffX, this.width-TOOL_W())); toolY = Math.max(0, Math.min((int)mouseY-dragToolOffY, this.height-computeToolH())); cfg.cfgToolX=toolX; cfg.cfgToolY=toolY; saveAllPositions(); clearAndInit(); return true; }
        if (draggingLeft)   { leftX = Math.max(0, Math.min((int)mouseX-dragLeftOffX, this.width-LEFT_W())); leftY = Math.max(0, Math.min((int)mouseY-dragLeftOffY, this.height-computeLeftH())); cfg.cfgLeftX=leftX; cfg.cfgLeftY=leftY; saveAllPositions(); clearAndInit(); return true; }
        if (draggingRight)  { rightX = Math.max(0, Math.min((int)mouseX-dragRightOffX, this.width-RIGHT_W())); rightY = Math.max(0, Math.min((int)mouseY-dragRightOffY, this.height-computeRightH())); cfg.cfgRightX=rightX; cfg.cfgRightY=rightY; saveAllPositions(); clearAndInit(); return true; }
        if (draggingHud)    { hud.setPos((int)mouseX-dragHudOffX, (int)mouseY-dragHudOffY); return true; }

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (resizingLeft || resizingRight || resizingTool || resizingDebug || resizingBright || resizingWp || resizingAr || resizingHud) cfg.save();
        resizingLeft = resizingRight = resizingTool = resizingDebug = resizingBright = resizingWp = resizingAr = resizingHud = false;
        draggingLeft = draggingRight = draggingHud = draggingScrollbar = draggingTool = draggingBright = draggingDebug = draggingWp = draggingAr = false;
        com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingFps = com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingCoords = com.hamtabot.obfutilities.debug.DebugOverlay.cfgDraggingRam = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (addingMode) {
            float sc = cfg.scaleRight;
            int listX = rightX+PAD(sc), listW = RIGHT_W()-PAD(sc)*2, listY = getRightListY(), listH = BLOCK_LIST_VISIBLE*s(18, sc);
            if (mouseX>=listX && mouseX<=listX+listW && mouseY>=listY && mouseY<listY+listH) {
                blockScrollOffset = Math.max(0, Math.min(blockScrollOffset-(int)amount, Math.max(0, filteredBlocks.size()-BLOCK_LIST_VISIBLE))); return true;
            }
        }
        float sc = cfg.scaleWp;
        int wtyS = wpY + PH(sc) + PAD(sc) + BTN(sc) + s(6, sc) + BTN(sc) + s(8, sc);
        if (mouseX>=wpX && mouseX<=wpX+WP_W() && mouseY>=wtyS && mouseY<wtyS+WP_VISIBLE*s(36, sc)) {
            wpScrollOffset = Math.max(0, Math.min(wpScrollOffset-(int)amount, Math.max(0, com.hamtabot.obfutilities.waypoint.WaypointManager.getAll().size()-WP_VISIBLE))); return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningForKey) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { listeningForKey = false; saveAllPositions(); clearAndInit(); return true; }
            cfg.attackRemapKey = keyCode; cfg.save();
            if (cfg.attackRemapEnabled) { MinecraftClient.getInstance().options.attackKey.setBoundKey(net.minecraft.client.util.InputUtil.fromKeyCode(keyCode, 0)); net.minecraft.client.option.KeyBinding.updateKeysByCode(); }
            listeningForKey = false; saveAllPositions(); clearAndInit(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        cfg.cfgLeftX=leftX; cfg.cfgLeftY=leftY; cfg.cfgRightX=rightX; cfg.cfgRightY=rightY;
        cfg.cfgToolX=toolX; cfg.cfgToolY=toolY; cfg.cfgBrightX=brightX; cfg.cfgBrightY=brightY;
        cfg.cfgDebugX=debugX; cfg.cfgDebugY=debugY; cfg.cfgWpX=wpX; cfg.cfgWpY=wpY;
        leftX=-1; rightX=-1; toolX=-1; brightX=-1; debugX=-1; wpX=-1; arX=-1;
        super.resize(client, width, height);
    }

    private void saveAllPositions() {
        if (leftX != -1)  { cfg.cfgLeftX=leftX;    cfg.cfgLeftY=leftY; }
        if (rightX != -1) { cfg.cfgRightX=rightX;   cfg.cfgRightY=rightY; }
        if (toolX != -1)  { cfg.cfgToolX=toolX;     cfg.cfgToolY=toolY; }
        if (brightX != -1){ cfg.cfgBrightX=brightX; cfg.cfgBrightY=brightY; }
        if (debugX != -1) { cfg.cfgDebugX=debugX;   cfg.cfgDebugY=debugY; }
        if (wpX != -1)    { cfg.cfgWpX=wpX;         cfg.cfgWpY=wpY; }
        if (arX != -1)    { cfg.cfgAttackRemapX=arX; cfg.cfgAttackRemapY=arY; }
    }

    private int refreshTick = 0;

    @Override
    public void tick() {
        super.tick();
        if (!OBFUtilities.canRefreshStats()) {
            refreshTick++;
            if (refreshTick >= 20) {
                refreshTick = 0;
                boolean fieldFocused = searchField != null && searchField.isFocused();
                if (!fieldFocused && !listeningForKey) { saveAllPositions(); clearAndInit(); }
            }
        } else { refreshTick = 0; }
    }

    @Override public void close() {
        com.hamtabot.obfutilities.debug.DebugOverlay.inConfigScreen = false;
        cfg.cfgLeftX=leftX; cfg.cfgLeftY=leftY; cfg.cfgRightX=rightX; cfg.cfgRightY=rightY;
        cfg.cfgToolX=toolX; cfg.cfgToolY=toolY; cfg.cfgBrightX=brightX; cfg.cfgBrightY=brightY;
        cfg.cfgDebugX=debugX; cfg.cfgDebugY=debugY; cfg.cfgWpX=wpX; cfg.cfgWpY=wpY;
        cfg.cfgAttackRemapX=arX; cfg.cfgAttackRemapY=arY;
        cfg.save(); hud.savePosition(); super.close();
    }
    @Override public boolean shouldPause() { return false; }

    private void clampPanels() {
        if (toolX==-1) toolX=10;     toolX=Math.max(0,Math.min(toolX,  this.width-TOOL_W()));   toolY=Math.max(0,Math.min(toolY,  this.height-computeToolH()));
        if (brightX==-1){brightX=10;brightY=20;} brightX=Math.max(0,Math.min(brightX,this.width-BRIGHT_W())); brightY=Math.max(0,Math.min(brightY,this.height-computeBrightH()));
        if (wpX==-1){wpX=10;wpY=20;} wpX=Math.max(0,Math.min(wpX,this.width-WP_W())); wpY=Math.max(0,Math.min(wpY,this.height-computeWpH()));
        if (arX==-1){arX=WP_W()+18;arY=20;} arX=Math.max(0,Math.min(arX,this.width-AR_W())); arY=Math.max(0,Math.min(arY,this.height-computeArH()));
        if (debugX==-1){debugX=10;debugY=20;} debugX=Math.max(0,Math.min(debugX,this.width-DEBUG_W())); debugY=Math.max(0,Math.min(debugY,this.height-computeDebugH()));
        leftX=Math.max(0,Math.min(leftX,this.width-LEFT_W()));   leftY=Math.max(0,Math.min(leftY,this.height-computeLeftH()));
        rightX=Math.max(0,Math.min(rightX,this.width-RIGHT_W())); rightY=Math.max(0,Math.min(rightY,this.height-computeRightH()));
    }

    private void rebuildBlockList() {
        boolean placed = pendingMode.equals("posé");
        allBlocks = (placed ? Registries.ITEM.getIds() : Registries.BLOCK.getIds()).stream().map(Identifier::toString).filter(id -> !id.equals("minecraft:air")).sorted().collect(Collectors.toList());
        filteredBlocks = searchField != null && !searchField.getText().isEmpty()
                ? allBlocks.stream().filter(b -> b.contains(searchField.getText().toLowerCase())).collect(Collectors.toList())
                : new ArrayList<>(allBlocks);
        blockScrollOffset = 0;
    }

    private int getRightListY() {
        float sc = cfg.scaleRight;
        int ty = rightY + PH(sc) + PAD(sc) + s(14, sc);
        ty += cfg.customBlocks.size() * s(20, sc);
        ty += BTN(sc) + s(8, sc);
        ty += BTN(sc) + s(6, sc);
        ty += BTN(sc) + s(6, sc);
        ty += s(16, sc) + s(4, sc);
        return ty;
    }

    private String formatBlockName(String id) {
        String n = id.contains(":") ? id.split(":")[1] : id;
        StringBuilder sb = new StringBuilder();
        for (String p : n.split("_")) if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        return sb.toString().trim();
    }

    private int computeLeftH()  { float sc=cfg.scaleLeft;   return PH(sc)+PAD(sc)+s(14,sc)+ROW(sc)*4+s(4,sc)+BTN(sc)+s(6,sc)+BTN(sc)+s(18,sc); }
    private int computeRightH() { float sc=cfg.scaleRight;  int h=PH(sc)+PAD(sc)+s(14,sc)+cfg.customBlocks.size()*s(20,sc)+BTN(sc)+s(8,sc); if(addingMode) h+=BTN(sc)+s(6,sc)+s(16,sc)+s(6,sc)+BLOCK_LIST_VISIBLE*s(18,sc)+s(6,sc); return h+s(6,sc); }
    private int computeToolH()  { float sc=cfg.scaleTool;   return PH(sc)+PAD(sc)+s(14,sc)+BTN(sc)+s(6,sc)+BTN(sc)+s(10,sc)+s(14,sc)+s(20,sc)+s(20,sc)+s(16,sc)+s(14,sc); }
    private int computeBrightH(){ float sc=cfg.scaleBright; return PH(sc)+PAD(sc)+s(14,sc)+BTN(sc)+PAD(sc); }
    private int computeDebugH() { float sc=cfg.scaleDebug;  return PH(sc)+PAD(sc)+s(14,sc)+BTN(sc)+ROW(sc)+BTN(sc)+ROW(sc)+BTN(sc)+s(16,sc)+PAD(sc); }
    private int computeWpH()    { float sc=cfg.scaleWp;     return PH(sc)+PAD(sc)+BTN(sc)+s(6,sc)+BTN(sc)+s(8,sc)+WP_VISIBLE*s(36,sc)+s(14,sc)+BTN(sc)+s(24,sc)+s(14,sc); }
    private int computeArH()    { float sc=cfg.scaleAr;     return PH(sc)+PAD(sc)+BTN(sc)+s(8,sc)+BTN(sc)+s(16,sc); }

    private void drawHoverBtn(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label) {
        ctx.fill(x,y,x+w,y+h,0xBB1A1A1A); ctx.fill(x,y,x+w,y+1,0xFF546E7A); ctx.fill(x,y+h-1,x+w,y+h,0xFF546E7A); ctx.fill(x,y,x+1,y+h,0xFF546E7A); ctx.fill(x+w-1,y,x+w,y+h,0xFF546E7A);
        ctx.drawText(tr, label, x+w/2-tr.getWidth(label)/2, y+2, 0xFFFFFFFF, false);
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

    protected void clearAndInit() { this.clearChildren(); this.init(); }
}