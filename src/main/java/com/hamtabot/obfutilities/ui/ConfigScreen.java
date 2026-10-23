package com.hamtabot.obfutilities.ui;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.blockshade.BlockShade;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.input.KeyRemap;
import com.hamtabot.obfutilities.input.Keybinds;
import com.hamtabot.obfutilities.stats.AdTimer;
import com.hamtabot.obfutilities.stats.ServerStats;
import com.hamtabot.obfutilities.stats.SessionStats;
import com.hamtabot.obfutilities.stats.SessionStore;
import com.hamtabot.obfutilities.timer.TimerEditScreen;
import com.hamtabot.obfutilities.timer.TimerEntry;
import com.hamtabot.obfutilities.timer.Timers;
import com.hamtabot.obfutilities.ui.component.*;
import com.hamtabot.obfutilities.util.Format;
import com.hamtabot.obfutilities.util.OsUtil;
import com.hamtabot.obfutilities.waypoint.Waypoint;
import com.hamtabot.obfutilities.waypoint.WaypointCreateScreen;
import com.hamtabot.obfutilities.waypoint.WaypointManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigScreen extends Screen {

    public enum Page {
        GENERAL  ("◆", "Général",      "Statistiques affichées dans le HUD"),
        ADS      ("✉", "Publicité",    "Enregistre ta pub et envoie-la en un clic"),
        TIMERS   ("◷", "Timers",       "Chronos et comptes à rebours, affichables à l'écran"),
        BLOCKS   ("▣", "Blocs suivis", "Compte des blocs précis en plus des stats globales"),
        AUTOTOOL ("⛏", "AutoTool",     "Prend automatiquement le bon outil de la hotbar"),
        VISUALS  ("☀", "Visuel",       "Luminosité et overlays d'informations"),
        SHADES   ("◐", "Blocs sombres", "Applique une texture sombre aux blocs de ton choix"),
        CONTROLS ("⌨", "Contrôles",    "Drop sécurisé et touches de remplacement"),
        WAYPOINTS("⚑", "Waypoints",    "Points de repère affichés dans le monde");

        final String icon, title, subtitle;
        Page(String icon, String title, String subtitle) { this.icon = icon; this.title = title; this.subtitle = subtitle; }
    }

    private static final int MAX_W = 480, MAX_H = 340;
    private static final int HEADER_H = 24, NAV_ITEM_H = 20;
    private static final int LOGO_SIZE = 18, TITLE_X = 10 + LOGO_SIZE + 6;
    private static final String VERSION = "v" + FabricLoader.getInstance().getModContainer(OBFUtilities.MOD_ID)
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");

    private static Page lastPage = Page.GENERAL;

    private final Screen parent;
    private final ModConfig cfg = OBFUtilities.config;
    private final ScrollList list = new ScrollList();
    private Page page;
    private long adSentAt;

    private int winX, winY, winW, winH, navW, contentX;
    private boolean compactNav;

    public ConfigScreen(Screen parent) { this(parent, lastPage); }

    public ConfigScreen(Screen parent, Page page) {
        super(Text.literal("OBF Utilities"));
        this.parent = parent;
        this.page = page;
    }

    @Override
    protected void init() {
        int margin = width < 400 || height < 260 ? 6 : 16;
        winW = Math.min(width - margin * 2, MAX_W);
        winH = Math.min(height - margin * 2, MAX_H);
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        compactNav = winW < 340;
        navW = compactNav ? 28 : 108;

        contentX = winX + navW + 10;
        int listY = winY + HEADER_H + 32;
        list.setBounds(contentX, listY, winX + winW - 8 - contentX, winY + winH - 8 - listY);
        refresh();
    }

    private void showPage(Page p) {
        if (p == page) return;
        page = p;
        lastPage = p;
        list.setComponents(buildPage(p), false);
    }

    private void refresh() { list.setComponents(buildPage(page), true); }

    private List<Component> buildPage(Page p) {
        List<Component> c = new ArrayList<>();
        switch (p) {
            case GENERAL   -> buildGeneral(c);
            case ADS       -> buildAds(c);
            case TIMERS    -> buildTimers(c);
            case BLOCKS    -> buildBlocks(c);
            case AUTOTOOL  -> buildAutoTool(c);
            case VISUALS   -> buildVisuals(c);
            case SHADES    -> buildShades(c);
            case CONTROLS  -> buildControls(c);
            case WAYPOINTS -> buildWaypoints(c);
        }
        return c;
    }

    private void buildGeneral(List<Component> c) {
        c.add(new SectionTitle("HUD"));
        c.add(new ToggleRow("HUD visible", "Raccourci : [" + Format.keyName(Keybinds.toggleHud) + "]",
                () -> OBFUtilities.getHud().isVisible(), v -> OBFUtilities.getHud().toggleVisible()));
        c.add(new ButtonRow("Position et taille", "Déplace le HUD à la souris, et agrandis-le avec le coin en bas à droite.",
                "Modifier", Draw.ButtonStyle.PRIMARY, this::openHudEditor));

        c.add(new SectionTitle("Sections affichées"));
        c.add(new ToggleRow("Blocs posés", "Blocs posés pendant la session et au total.",
                () -> cfg.showBlocksPlaced, v -> cfg.showBlocksPlaced = v));
        c.add(new ToggleRow("Blocs minés", "Blocs cassés pendant la session et au total.",
                () -> cfg.showBlocksMined, v -> cfg.showBlocksMined = v));
        c.add(new ToggleRow("Mobs tués", "Mobs tués pendant la session et au total, stacks compris.",
                () -> cfg.showMobsKilled, v -> cfg.showMobsKilled = v));
        c.add(new ToggleRow("Minuteur de pub", "Temps restant avant de pouvoir renvoyer une pub ([i] ou [item] dans le chat).",
                () -> cfg.showAdTimer, v -> cfg.showAdTimer = v));

        c.add(new SectionTitle("Statistiques"));
        c.add(new ToggleRow("Garder la session après un redémarrage", "Les compteurs de session et le chrono reprennent où ils en étaient au prochain lancement du jeu.",
                () -> cfg.keepSession, v -> { cfg.keepSession = v; SessionStore.save(); }));
        c.add(new ButtonRow("Réinitialiser la session", "Remet à zéro les compteurs de session et le chrono.",
                "Réinitialiser", Draw.ButtonStyle.DANGER, SessionStats::reset));
        c.add(new ButtonRow("Actualiser les totaux", "Redemande les totaux au serveur. Possible une fois toutes les 15 minutes.",
                () -> ServerStats.canRefresh() ? "Actualiser" : Format.countdown(ServerStats.getRefreshCooldownRemaining()),
                Draw.ButtonStyle.SECONDARY, ServerStats::canRefresh, ServerStats::refresh));
        c.add(new InfoBox("Les totaux sont récupérés automatiquement à chaque connexion au serveur.", Theme.ACCENT));
    }

    private void buildAds(List<Component> c) {
        c.add(new SectionTitle("Ma publicité"));
        c.add(new TextAreaRow("Écris ta pub ici...", 256, cfg.adMessage, v -> cfg.adMessage = v));
        c.add(new ButtonRow("Envoyer la pub", "Envoie le message ci-dessus dans le chat.",
                () -> System.currentTimeMillis() - adSentAt < 1500 ? "Envoyée !"
                        : AdTimer.isAvailable() ? "Envoyer" : Format.countdown(AdTimer.getRemainingMs()),
                Draw.ButtonStyle.PRIMARY, AdTimer::canSendSaved,
                () -> { AdTimer.sendSaved(); adSentAt = System.currentTimeMillis(); }));
        c.add(new InfoBox("Le minuteur démarre quand le message contient [i] ou [item].", Theme.ACCENT));

        c.add(new SectionTitle("Notifications"));
        c.add(new ToggleRow("Message à l'écran", "Affiche « Pub disponible » au milieu de l'écran quand tu peux renvoyer ta pub.",
                () -> cfg.adTitleEnabled, v -> cfg.adTitleEnabled = v));
    }

    private void buildTimers(List<Component> c) {
        List<TimerEntry> timers = Timers.all();
        c.add(new ButtonRow("Nouveau timer", "Compte à rebours (title à la fin) ou chronomètre. " + Timers.MAX + " timers au maximum.",
                () -> "+ Créer", Draw.ButtonStyle.PRIMARY, () -> timers.size() < Timers.MAX,
                () -> client.setScreen(new TimerEditScreen(this, null))));
        c.add(new ButtonRow("Placer les timers", "Active l'interrupteur d'un timer pour l'afficher, puis place-le et règle sa taille.",
                "Modifier", Draw.ButtonStyle.SECONDARY, this::openHudEditor));

        c.add(new SectionTitle("Mes timers (" + timers.size() + ")"));
        if (timers.isEmpty()) {
            c.add(new InfoBox("Aucun timer. Exemple : un compte à rebours \"Vote\" de 24 h, à relancer avec ↻ après chaque vote.", Theme.TEXT_MUTED));
        }
        for (int i = 0; i < timers.size(); i++) {
            TimerEntry t = timers.get(i);
            c.add(new TimerRow(i, t, () -> client.setScreen(new TimerEditScreen(this, t)),
                    () -> { timers.remove(t); cfg.save(); refresh(); }));
        }
        if (!timers.isEmpty()) {
            c.add(new InfoBox("▶ démarrer / pause  ·  ↻ relancer depuis le début  ·  ✎ modifier  ·  interrupteur : afficher à l'écran", Theme.ACCENT));
        }
    }

    private void buildBlocks(List<Component> c) {
        c.add(new ButtonRow("Ajouter un bloc", "Choisis un bloc et s'il faut compter les blocs posés ou minés.",
                "+ Ajouter", Draw.ButtonStyle.PRIMARY, () -> client.setScreen(new BlockPickerScreen(this))));

        c.add(new SectionTitle("Blocs suivis (" + cfg.customBlocks.size() + ")"));
        if (cfg.customBlocks.isEmpty()) {
            c.add(new InfoBox("Aucun bloc suivi pour l'instant. Ajoute-en un pour voir son compteur dans le HUD.", Theme.TEXT_MUTED));
        }
        for (ModConfig.CustomBlockEntry e : cfg.customBlocks) {
            c.add(new CustomBlockRow(e,
                    () -> SessionStats.resetCustom(e),
                    () -> { cfg.customBlocks.remove(e); SessionStats.resetCustom(e); cfg.save(); refresh(); }));
        }
    }

    private void buildAutoTool(List<Component> c) {
        c.add(new ToggleRow("Activer AutoTool", "Prend le meilleur outil en visant un bloc, et l'arme en frappant un mob.",
                () -> cfg.autoToolEnabled, v -> cfg.autoToolEnabled = v));
        c.add(new ToggleRow("Protéger les outils usés", "Ignore les outils à 10 de durabilité ou moins pour éviter de les casser.",
                () -> cfg.autoToolSkipLowDurability, v -> cfg.autoToolSkipLowDurability = v));
        c.add(new ChipsRow("Outils autorisés")
                .add("Pioche",    () -> cfg.autoToolUsePioche,   v -> cfg.autoToolUsePioche   = v)
                .add("Pelle",     () -> cfg.autoToolUsePelle,    v -> cfg.autoToolUsePelle    = v)
                .add("Hache",     () -> cfg.autoToolUseHache,    v -> cfg.autoToolUseHache    = v)
                .add("Houe",      () -> cfg.autoToolUseHoue,     v -> cfg.autoToolUseHoue     = v)
                .add("Cisailles", () -> cfg.autoToolUseCisaille, v -> cfg.autoToolUseCisaille = v)
                .add("Épée",      () -> cfg.autoToolUseEpee,     v -> cfg.autoToolUseEpee     = v));
    }

    private void buildVisuals(List<Component> c) {
        c.add(new SectionTitle("Luminosité"));
        c.add(new ToggleRow("FullBright", "Applique Vision nocturne en local pour tout voir dans le noir.",
                () -> cfg.fullBrightEnabled, v -> cfg.fullBrightEnabled = v));

        c.add(new SectionTitle("Overlays"));
        c.add(new ToggleRow("FPS", "Images par seconde, en couleur selon les performances.",
                () -> cfg.debugShowFps, v -> cfg.debugShowFps = v));
        c.add(new ToggleRow("Coordonnées", "Position X / Y / Z et direction regardée.",
                () -> cfg.debugShowCoords, v -> cfg.debugShowCoords = v));
        c.add(new ToggleRow("Mémoire", "RAM utilisée par le jeu, avec une jauge.",
                () -> cfg.debugShowRam, v -> cfg.debugShowRam = v));
        c.add(new ButtonRow("Placer les overlays", "Glisse chaque overlay où tu veux sur l'écran.",
                "Modifier", Draw.ButtonStyle.PRIMARY, this::openHudEditor));
    }

    private void buildShades(List<Component> c) {
        c.add(new ToggleRow("Activer", "Remplace la texture des blocs choisis par une texture sombre, plus reposante pour les yeux.",
                () -> cfg.blockShadeEnabled, v -> cfg.blockShadeEnabled = v));
        c.add(new ButtonRow("Ajouter des blocs", "Coche autant de blocs que tu veux, puis valide.",
                "+ Ajouter", Draw.ButtonStyle.PRIMARY, this::openShadePicker));

        c.add(new SectionTitle("Blocs assombris (" + cfg.blockShades.size() + ")"));
        if (cfg.blockShades.isEmpty()) {
            c.add(new InfoBox("Aucun bloc pour l'instant. Clique sur \"+ Ajouter\" et coche les blocs à assombrir.", Theme.TEXT_MUTED));
        } else {
            c.add(new ButtonRow("Tout retirer", "Rend leur texture d'origine à tous les blocs de la liste.",
                    "Tout retirer", Draw.ButtonStyle.DANGER,
                    () -> { cfg.blockShades.clear(); cfg.save(); refresh(); }));
        }
        for (ModConfig.BlockShadeEntry e : cfg.blockShades) {
            c.add(new ShadeBlockRow(e, () -> { cfg.blockShades.remove(e); cfg.save(); refresh(); }));
        }
        c.add(new InfoBox("La texture change partout où elle apparaît, icônes de l'inventaire comprises.", Theme.ACCENT));
    }

    private void openShadePicker() {
        Set<String> added = new HashSet<>();
        for (ModConfig.BlockShadeEntry e : cfg.blockShades) added.add(e.blockId);
        client.setScreen(new BlockPickerScreen(this, "Blocs à assombrir", added, BlockShade::isEligible, ids -> {
            for (String id : ids) {
                if (!added.contains(id)) cfg.blockShades.add(new ModConfig.BlockShadeEntry(id));
            }
            cfg.save();
        }));
    }

    private void buildControls(List<Component> c) {
        c.add(new SectionTitle("Inventaire"));
        c.add(new ToggleRow("Double appui pour jeter", "Il faut appuyer deux fois sur la touche de drop pour jeter un objet. Évite les drops accidentels.",
                () -> cfg.doubleDropEnabled, v -> cfg.doubleDropEnabled = v));

        c.add(new SectionTitle("Clic gauche"));
        c.add(new ToggleRow("Remplacer le clic gauche", "Attaque avec une touche du clavier au lieu de la souris.",
                () -> KeyRemap.isEnabled(KeyRemap.Target.ATTACK), v -> KeyRemap.setEnabled(KeyRemap.Target.ATTACK, v)));
        c.add(new KeyBindRow("Touche d'attaque", "Clique puis appuie sur une touche. Échap pour annuler.",
                () -> KeyRemap.getKey(KeyRemap.Target.ATTACK), k -> KeyRemap.setKey(KeyRemap.Target.ATTACK, k)));

        c.add(new SectionTitle("Clic droit"));
        c.add(new ToggleRow("Remplacer le clic droit", "Utilise et pose avec une touche du clavier au lieu de la souris.",
                () -> KeyRemap.isEnabled(KeyRemap.Target.USE), v -> KeyRemap.setEnabled(KeyRemap.Target.USE, v)));
        c.add(new KeyBindRow("Touche d'utilisation", "Clique puis appuie sur une touche. Échap pour annuler.",
                () -> KeyRemap.getKey(KeyRemap.Target.USE), k -> KeyRemap.setKey(KeyRemap.Target.USE, k)));

        c.add(new InfoBox("Les raccourcis du mod (HUD, config, waypoints) se changent dans Options › Contrôles › Touches, catégorie OBF Utilities.", Theme.ACCENT));
    }

    private void buildWaypoints(List<Component> c) {
        c.add(new ToggleRow("Afficher les waypoints", "Affiche les waypoints de la dimension actuelle jusqu'à 1000 blocs.",
                () -> cfg.waypointsEnabled, v -> cfg.waypointsEnabled = v));
        c.add(new ButtonRow("Nouveau waypoint", "Crée un waypoint à ta position. Raccourci : [" + Format.keyName(Keybinds.addWaypoint) + "]",
                "+ Créer", Draw.ButtonStyle.PRIMARY, () -> client.setScreen(new WaypointCreateScreen(this))));
        c.add(new ButtonRow("Dossier des waypoints", "Ouvre le dossier où ils sont sauvegardés.",
                "Ouvrir", Draw.ButtonStyle.SECONDARY, () -> OsUtil.openFolder(new File("config"))));

        List<Waypoint> all = WaypointManager.getAll();
        c.add(new SectionTitle("Mes waypoints (" + all.size() + ")"));
        if (all.isEmpty()) {
            c.add(new InfoBox("Aucun waypoint. \"Partager\" copie un waypoint : colle-le dans le chat pour que les autres puissent l'importer en un clic.", Theme.TEXT_MUTED));
        }
        for (Waypoint wp : new ArrayList<>(all)) {
            c.add(new WaypointRow(wp, () -> { WaypointManager.remove(wp); refresh(); }));
        }
    }

    private void openHudEditor() { client.setScreen(new HudEditScreen(this)); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Draw.window(ctx, width, height, winX, winY, winW, winH, HEADER_H);
        renderHeader(ctx, mouseX, mouseY);
        renderNav(ctx, mouseX, mouseY);

        int contentW = winX + winW - 8 - contentX;
        int ty = winY + HEADER_H + 8;
        Draw.round(ctx, contentX, ty - 1, 2, 10, Theme.ACCENT);
        ctx.drawText(textRenderer, page.title, contentX + 6, ty, Theme.TEXT, true);
        ctx.drawText(textRenderer, Draw.ellipsize(textRenderer, page.subtitle, contentW), contentX, ty + 11, Theme.TEXT_MUTED, false);

        list.render(ctx, mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);

        if (compactNav) {
            Page hovered = navAt(mouseX, mouseY);
            if (hovered != null) ctx.drawTooltip(textRenderer, Text.literal(hovered.title), mouseX, mouseY);
        }
    }

    private void renderHeader(DrawContext ctx, int mouseX, int mouseY) {
        int titleEnd = Draw.brand(ctx, textRenderer, winX + 10, winY, HEADER_H, LOGO_SIZE, TITLE_X - 10 - LOGO_SIZE);
        ctx.drawText(textRenderer, VERSION, titleEnd + 5, winY + (HEADER_H - 8) / 2 + 1, Theme.TEXT_DIM, false);

        int[] c = closeBounds();
        Draw.closeButton(ctx, c[0], c[1], Theme.CLOSE_SIZE, Draw.inside(mouseX, mouseY, c[0], c[1], Theme.CLOSE_SIZE, Theme.CLOSE_SIZE));

        int[] e = hudButtonBounds();
        if (e != null) {
            Draw.button(ctx, textRenderer, e[0], e[1], e[2], e[3], "Éditer le HUD", Draw.ButtonStyle.SECONDARY,
                    Draw.inside(mouseX, mouseY, e[0], e[1], e[2], e[3]), true);
        }
    }

    private void renderNav(DrawContext ctx, int mouseX, int mouseY) {
        int nx = winX + 1, ny = winY + HEADER_H + 1;
        ctx.fill(nx, ny, nx + navW - 1, winY + winH - 2, Theme.SIDEBAR);
        ctx.fill(nx + navW - 1, ny, nx + navW, winY + winH - 1, Theme.DIVIDER);

        for (Page p : Page.values()) {
            int[] b = navBounds(p);
            boolean selected = p == page;
            boolean hov = Draw.inside(mouseX, mouseY, b[0], b[1], b[2], b[3]);
            if (selected) {
                Draw.round(ctx, b[0], b[1], b[2], b[3], Theme.ACCENT_SURFACE);
                ctx.fill(b[0], b[1] + 4, b[0] + 2, b[1] + b[3] - 4, Theme.ACCENT);
            } else if (hov) {
                Draw.round(ctx, b[0], b[1], b[2], b[3], Theme.SURFACE_HOVER);
            }
            int iconColor = selected ? Theme.ACCENT : hov ? Theme.TEXT : Theme.TEXT_MUTED;
            int textY = b[1] + (b[3] - 8) / 2 + 1;
            if (compactNav) {
                ctx.drawText(textRenderer, p.icon, b[0] + (b[2] - textRenderer.getWidth(p.icon)) / 2, textY, iconColor, false);
            } else {
                ctx.drawText(textRenderer, p.icon, b[0] + 8, textY, iconColor, false);
                ctx.drawText(textRenderer, Draw.ellipsize(textRenderer, p.title, b[2] - 26), b[0] + 22, textY,
                        selected || hov ? Theme.TEXT : Theme.TEXT_MUTED, false);
            }
        }

        if (!compactNav) {
            String hint = "Échap pour fermer";
            int hy = winY + winH - 14;
            if (hy > navBounds(Page.values()[Page.values().length - 1])[1] + NAV_ITEM_H + 4) {
                ctx.drawText(textRenderer, Draw.ellipsize(textRenderer, hint, navW - 14), winX + 8, hy, Theme.TEXT_DIM, false);
            }
        }
    }

    private int[] navBounds(Page p) {
        int x = winX + 4, w = navW - 8;
        int y = winY + HEADER_H + 6 + p.ordinal() * (NAV_ITEM_H + 2);
        return new int[]{ x, y, w, NAV_ITEM_H };
    }

    private Page navAt(double mx, double my) {
        for (Page p : Page.values()) {
            int[] b = navBounds(p);
            if (Draw.inside(mx, my, b[0], b[1], b[2], b[3])) return p;
        }
        return null;
    }

    private int[] closeBounds() {
        return new int[]{ winX + winW - Theme.CLOSE_SIZE - 5, winY + (HEADER_H - Theme.CLOSE_SIZE) / 2 };
    }

    private int[] hudButtonBounds() {
        int w = textRenderer.getWidth("Éditer le HUD") + 14;
        int x = closeBounds()[0] - 6 - w;
        int titleEnd = winX + TITLE_X + textRenderer.getWidth("OBF Utilities") + 5 + textRenderer.getWidth(VERSION);
        if (x < titleEnd + 8) return null;
        return new int[]{ x, winY + (HEADER_H - Theme.BUTTON_H) / 2, w, Theme.BUTTON_H };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int[] c = closeBounds();
            if (Draw.inside(mouseX, mouseY, c[0], c[1], Theme.CLOSE_SIZE, Theme.CLOSE_SIZE)) { close(); return true; }
            int[] e = hudButtonBounds();
            if (e != null && Draw.inside(mouseX, mouseY, e[0], e[1], e[2], e[3])) { openHudEditor(); return true; }
            Page p = navAt(mouseX, mouseY);
            if (p != null) { showPage(p); return true; }
        }
        if (list.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (list.mouseDragged(mouseX, mouseY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        list.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (list.mouseScrolled(mouseX, mouseY, amount)) return true;
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (list.isCapturingKeys()) return list.keyPressed(keyCode);
        if (Keybinds.openConfig.matchesKey(keyCode, scanCode)) { close(); return true; }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            Page[] pages = Page.values();
            int dir = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1;
            showPage(pages[(page.ordinal() + dir + pages.length) % pages.length]);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (list.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void removed() {
        cfg.save();
    }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    public boolean shouldPause() { return false; }
}
