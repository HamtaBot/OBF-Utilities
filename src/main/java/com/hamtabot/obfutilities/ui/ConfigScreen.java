package com.hamtabot.obfutilities.ui;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.blockshade.BlockShade;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.input.KeyRemap;
import com.hamtabot.obfutilities.input.Keybinds;
import com.hamtabot.obfutilities.stats.AdTimer;
import com.hamtabot.obfutilities.stats.AfkTimer;
import com.hamtabot.obfutilities.stats.ServerStats;
import com.hamtabot.obfutilities.stats.SessionStats;
import com.hamtabot.obfutilities.stats.SessionStore;
import com.hamtabot.obfutilities.timer.TimerEditScreen;
import com.hamtabot.obfutilities.timer.TimerEntry;
import com.hamtabot.obfutilities.timer.Timers;
import com.hamtabot.obfutilities.ui.component.*;
import com.hamtabot.obfutilities.util.Format;
import com.hamtabot.obfutilities.util.OsUtil;
import com.hamtabot.obfutilities.warp.WarpMacros;
import com.hamtabot.obfutilities.waypoint.Waypoint;
import com.hamtabot.obfutilities.waypoint.WaypointCreateScreen;
import com.hamtabot.obfutilities.waypoint.WaypointManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigScreen extends Screen {

    public enum Page {
        GENERAL  (Items.BOOK,            "Général",       "Statistiques affichées dans le HUD"),
        ADS      (Items.PAPER,           "Publicité",     "Enregistre ta pub et envoie-la en un clic"),
        AFK      (Items.RED_BED,         "AFK Timer",     "Temps passé dans ta zone AFK"),
        TIMERS   (Items.CLOCK,           "Timers",        "Chronos et comptes à rebours, affichables à l'écran"),
        BLOCKS   (Items.GRASS_BLOCK,     "Blocs suivis",  "Compte des blocs précis en plus des stats globales"),
        AUTOTOOL (Items.DIAMOND_PICKAXE, "AutoTool",      "Prend automatiquement le bon outil de la hotbar"),
        VISUALS  (Items.TORCH,           "Visuel",        "Luminosité et overlays d'informations"),
        OPTIMIZE (Items.FEATHER,         "Optimisations", "Allège le rendu pour gagner des FPS"),
        SHADES   (Items.COAL_BLOCK,      "Blocs sombres", "Applique une texture sombre aux blocs de ton choix"),
        CONTROLS (Items.LEVER,           "Contrôles",     "Drop sécurisé et touches de remplacement"),
        WAYPOINTS(Items.MAP,             "Waypoints",     "Points de repère affichés dans le monde"),
        WARPS    (Items.ENDER_PEARL,     "Macro Warps",   "Une touche du clavier pour chaque /warp du serveur"),
        STORAGE  (Items.HOPPER,          "Unité stockage", "Déposer et prendre en un clic dans les unités de stockage");

        final ItemStack icon;
        final String title, subtitle;
        Page(Item icon, String title, String subtitle) { this.icon = new ItemStack(icon); this.title = title; this.subtitle = subtitle; }
    }

    private static final int MAX_W = 480, MAX_H = 340;
    private static final int HEADER_H = 24, NAV_ITEM_H = 20, NAV_ICON = 12, NAV_BTN_H = 16, NAV_ARROW = 10;
    private static final int LOGO_SIZE = 18, TITLE_X = 10 + LOGO_SIZE + 6;
    private static final String VERSION = "v" + FabricLoader.getInstance().getModContainer(OBFUtilities.MOD_ID)
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");

    private static Page lastPage = Page.GENERAL;

    private final Screen parent;
    private final ModConfig cfg = OBFUtilities.config;
    private final ScrollList list = new ScrollList();
    private Page page;
    private final ScrollList navList = new ScrollList();
    private final List<NavItem> navItems = new ArrayList<>();
    private Page hoveredNav;
    private boolean organizing;
    private int warpsVersion = WarpMacros.version();
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
        navW = compactNav ? 28 : 118;
        layoutNav();

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
        navList.reveal(navItemFor(p));
    }

    private void refresh() { list.setComponents(buildPage(page), true); }

    private List<Component> buildPage(Page p) {
        List<Component> c = new ArrayList<>();
        switch (p) {
            case GENERAL   -> buildGeneral(c);
            case ADS       -> buildAds(c);
            case AFK       -> buildAfk(c);
            case TIMERS    -> buildTimers(c);
            case BLOCKS    -> buildBlocks(c);
            case AUTOTOOL  -> buildAutoTool(c);
            case VISUALS   -> buildVisuals(c);
            case OPTIMIZE  -> buildOptimizations(c);
            case SHADES    -> buildShades(c);
            case CONTROLS  -> buildControls(c);
            case WAYPOINTS -> buildWaypoints(c);
            case WARPS     -> buildWarps(c);
            case STORAGE   -> buildStorage(c);
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

    private void buildAfk(List<Component> c) {
        c.add(new ToggleRow("Afficher dans le HUD", "Temps passé dans la zone AFK. Il tourne quand tu y entres et s'arrête quand tu en sors.",
                () -> cfg.showAfkTimer, v -> cfg.showAfkTimer = v));
        c.add(new ToggleRow("Garder après un redémarrage", "Le temps AFK reprend où il en était au prochain lancement du jeu.",
                () -> cfg.keepAfkTimer, v -> { cfg.keepAfkTimer = v; AfkTimer.save(); }));
        c.add(new ButtonRow("Réinitialiser le timer", "Remet le temps AFK à zéro.",
                "Réinitialiser", Draw.ButtonStyle.DANGER, AfkTimer::reset));

        c.add(new SectionTitle("Zone AFK"));
        c.add(new ButtonRow("Coin 1", afkCorner(cfg.afkCorner1),
                () -> "Ma position", Draw.ButtonStyle.PRIMARY, () -> client.player != null,
                () -> { AfkTimer.setCorner(client, true); refresh(); }));
        c.add(new ButtonRow("Coin 2", afkCorner(cfg.afkCorner2),
                () -> "Ma position", Draw.ButtonStyle.PRIMARY, () -> client.player != null,
                () -> { AfkTimer.setCorner(client, false); refresh(); }));
        boolean hasZone = AfkTimer.hasZone();
        if (hasZone) {
            c.add(new ButtonRow("Effacer la zone", "Supprime les deux coins. Le timer ne tourne plus tant qu'une nouvelle zone n'est pas définie.",
                    "Effacer", Draw.ButtonStyle.DANGER, () -> { AfkTimer.clearZone(); refresh(); }));
        }
        c.add(new InfoBox("Place-toi sur un coin de la zone AFK et clique sur « Ma position », puis fais pareil au coin opposé. "
                + "Les deux blocs sont inclus dans la zone.", hasZone ? Theme.ACCENT : Theme.WARNING));
    }

    private String afkCorner(int[] corner) {
        if (!AfkTimer.isCorner(corner)) return "Non défini";
        return "X " + corner[0] + ", Y " + corner[1] + ", Z " + corner[2] + "  ·  " + cfg.afkDimension.replace("minecraft:", "");
    }

    private void buildTimers(List<Component> c) {
        List<TimerEntry> timers = Timers.all();
        c.add(new ButtonRow("Nouveau timer", "Compte à rebours (title à la fin) ou chronomètre. " + Timers.MAX + " timers au maximum.",
                () -> "+ Créer", Draw.ButtonStyle.PRIMARY, () -> timers.size() < Timers.MAX,
                () -> client.setScreen(new TimerEditScreen(this, null))));
        c.add(new ButtonRow("Placer les timers", "Active un timer pour l'afficher, puis place-le et règle sa taille.",
                "Modifier", Draw.ButtonStyle.SECONDARY, this::openHudEditor));

        c.add(new SectionTitle("Mes timers (" + timers.size() + ")"));
        if (timers.isEmpty()) {
            c.add(new InfoBox("Aucun timer pour l'instant. Exemple : un compte à rebours « Vote » de 24 h, que tu relances après chaque vote.", Theme.TEXT_MUTED));
        }
        for (int i = 0; i < timers.size(); i++) {
            TimerEntry t = timers.get(i);
            c.add(new TimerRow(i, t, () -> client.setScreen(new TimerEditScreen(this, t)),
                    () -> { timers.remove(t); cfg.save(); refresh(); }));
        }
        if (!timers.isEmpty()) {
            c.add(new SectionTitle("Légende"));
            c.add(new TimerLegend());
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

    private void buildOptimizations(List<Component> c) {
        c.add(new SectionTitle("Entités"));
        c.add(new ToggleRow("Cacher l'équipement des joueurs", "Cache l'armure, les objets en main et en seconde main, et ce qu'ils portent sur la tête (papier, têtes...). Le tien reste visible.",
                () -> cfg.hideOtherArmor, v -> cfg.hideOtherArmor = v));
        c.add(new ToggleRow("Cacher les cadres", "Les cadres deviennent invisibles, mais l'objet ou la carte qu'ils contiennent reste affiché.",
                () -> cfg.hideItemFrames, v -> cfg.hideItemFrames = v));

        c.add(new SectionTitle("Effets"));
        c.add(new ToggleRow("Désactiver les particules", "Supprime toutes les particules du jeu : explosions, potions, blocs cassés, fumée...",
                () -> cfg.disableParticles, v -> cfg.disableParticles = v));
        c.add(new ToggleRow("Pas d'animation des coffres", "Les coffres, coffres de l'Ender et shulker boxes s'ouvrent sans animer leur couvercle.",
                () -> cfg.disableChestAnimation, v -> cfg.disableChestAnimation = v));
        c.add(new ToggleRow("Cacher le mob des spawners", "Le mob qui tourne dans les spawners n'est plus affiché, la cage reste visible.",
                () -> cfg.disableSpawnerAnimation, v -> cfg.disableSpawnerAnimation = v));
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

    private void buildWarps(List<Component> c) {
        c.add(new ButtonRow("Actualiser la liste", "Récupère les warps du serveur avec /warp. C'est fait automatiquement à chaque connexion.",
                () -> WarpMacros.isSyncing() ? "..." : "Actualiser", Draw.ButtonStyle.SECONDARY,
                () -> client.player != null && !WarpMacros.isSyncing(), WarpMacros::refresh));

        List<ModConfig.WarpBind> warps = cfg.warps;
        c.add(new SectionTitle("Warps (" + warps.size() + ")"));
        if (warps.isEmpty()) {
            c.add(new InfoBox("Aucun warp trouvé. Connecte-toi au serveur, la liste se remplit toute seule.", Theme.TEXT_MUTED));
            return;
        }
        for (ModConfig.WarpBind w : warps) {
            c.add(new KeyBindRow(w.name, "/warp " + w.name, () -> w.key, k -> WarpMacros.setKey(w, k), true));
        }
        c.add(new InfoBox("Clique sur le bouton d'un warp puis appuie sur une touche. Retour arrière pour retirer la touche, Échap pour annuler. "
                + "Les touches marchent en jeu, pas dans le chat ni dans un menu.", Theme.ACCENT));
    }

    private void buildStorage(List<Component> c) {
        c.add(new ToggleRow("Bouton « Tout déposer »", "Dépose dans l'unité tous les objets de ton inventaire identiques au bloc qu'elle contient.",
                () -> cfg.storageDepositEnabled, v -> cfg.storageDepositEnabled = v));
        c.add(new ToggleRow("Bouton « Tout prendre »", "Prend des stacks dans l'unité jusqu'à ce que ton inventaire soit plein.",
                () -> cfg.storageWithdrawEnabled, v -> cfg.storageWithdrawEnabled = v));
        c.add(new InfoBox("Les boutons apparaissent à côté de l'unité quand tu l'ouvres. Clique sur « Arrêter » pour interrompre.", Theme.ACCENT));
    }

    @Override
    public void tick() {
        if (warpsVersion != WarpMacros.version() && !list.isCapturingKeys()) {
            warpsVersion = WarpMacros.version();
            if (page == Page.WARPS) refresh();
        }
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
            c.add(new InfoBox("Aucun waypoint. \"Partager\" copie un waypoint : colle-le dans en MP pour que les autres puissent l'importer en un clic.", Theme.TEXT_MUTED));
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

        if (compactNav && hoveredNav != null) ctx.drawTooltip(textRenderer, Text.literal(hoveredNav.title), mouseX, mouseY);
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

    private List<Page> orderedPages() {
        List<Page> pages = new ArrayList<>();
        for (String name : cfg.pageOrder) {
            for (Page p : Page.values()) {
                if (p.name().equals(name) && !pages.contains(p)) pages.add(p);
            }
        }
        for (Page p : Page.values()) {
            if (!pages.contains(p)) pages.add(p);
        }
        return pages;
    }

    private void movePage(Page p, int dir) {
        List<Page> pages = orderedPages();
        int i = pages.indexOf(p), j = i + dir;
        if (j < 0 || j >= pages.size()) return;
        pages.set(i, pages.get(j));
        pages.set(j, p);
        cfg.pageOrder = new ArrayList<>(pages.stream().map(Page::name).toList());
        cfg.save();
        layoutNav();
        navList.reveal(navItemFor(p));
    }

    private void resetPageOrder() {
        cfg.pageOrder.clear();
        cfg.save();
        layoutNav();
    }

    private NavItem navItemFor(Page p) {
        for (NavItem item : navItems) if (item.target == p) return item;
        return navItems.get(0);
    }

    private int navFooterH() {
        if (compactNav) return 0;
        return organizing ? 2 * (NAV_BTN_H + 4) + 2 : NAV_BTN_H + 6;
    }

    private void layoutNav() {
        navItems.clear();
        for (Page p : orderedPages()) navItems.add(new NavItem(p));
        int top = winY + HEADER_H + 6, available = winH - HEADER_H - 12;
        navList.setBounds(winX + 4, top, navW - 4, available - navFooterH());
        navList.setComponents(new ArrayList<>(navItems), true);
        navList.reveal(navItemFor(page));
    }

    private int[][] navFooterButtons() {
        if (compactNav) return new int[0][];
        int x = winX + 6, w = navW - 12, bottom = winY + winH - 6;
        if (!organizing) return new int[][]{ { x, bottom - NAV_BTN_H, w, NAV_BTN_H } };
        return new int[][]{
                { x, bottom - 2 * NAV_BTN_H - 4, w, NAV_BTN_H },
                { x, bottom - NAV_BTN_H, w, NAV_BTN_H }
        };
    }

    private void renderNav(DrawContext ctx, int mouseX, int mouseY) {
        int nx = winX + 1, ny = winY + HEADER_H + 1;
        ctx.fill(nx, ny, nx + navW - 1, winY + winH - 2, Theme.SIDEBAR);
        ctx.fill(nx + navW - 1, ny, nx + navW, winY + winH - 1, Theme.DIVIDER);

        hoveredNav = null;
        navList.render(ctx, mouseX, mouseY);

        int[][] buttons = navFooterButtons();
        for (int i = 0; i < buttons.length; i++) {
            int[] b = buttons[i];
            boolean hov = Draw.inside(mouseX, mouseY, b[0], b[1], b[2], b[3]);
            if (!organizing) {
                Draw.button(ctx, textRenderer, b[0], b[1], b[2], b[3], "Organiser", Draw.ButtonStyle.SECONDARY, hov, true);
            } else if (i == 0) {
                Draw.button(ctx, textRenderer, b[0], b[1], b[2], b[3], "Ordre par défaut", Draw.ButtonStyle.SECONDARY, hov, !cfg.pageOrder.isEmpty());
            } else {
                Draw.button(ctx, textRenderer, b[0], b[1], b[2], b[3], "Terminé", Draw.ButtonStyle.PRIMARY, hov, true);
            }
        }
    }

    private class NavItem extends Component {

        private final Page target;

        NavItem(Page target) { this.target = target; }

        @Override public int getHeight() { return NAV_ITEM_H; }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY) {
            boolean selected = target == page;
            boolean hov = isHovered(mouseX, mouseY);
            if (hov) hoveredNav = target;
            if (selected) {
                Draw.round(ctx, x, y, width, NAV_ITEM_H, Theme.ACCENT_SURFACE);
                ctx.fill(x, y + 4, x + 2, y + NAV_ITEM_H - 4, Theme.ACCENT);
            } else if (hov) {
                Draw.round(ctx, x, y, width, NAV_ITEM_H, Theme.SURFACE_HOVER);
            }
            int iconY = y + (NAV_ITEM_H - NAV_ICON) / 2;
            if (compactNav) {
                Draw.item(ctx, target.icon, x + (width - NAV_ICON) / 2, iconY, NAV_ICON);
                return;
            }
            Draw.item(ctx, target.icon, x + 6, iconY, NAV_ICON);
            int titleW = width - 26 - (organizing ? 2 * NAV_ARROW + 4 : 0);
            ctx.drawText(tr(), Draw.ellipsize(tr(), target.title, titleW), x + 22, y + (NAV_ITEM_H - 8) / 2 + 1,
                    selected || hov ? Theme.TEXT : Theme.TEXT_MUTED, false);
            if (organizing) {
                renderArrow(ctx, -1, mouseX, mouseY);
                renderArrow(ctx, 1, mouseX, mouseY);
            }
        }

        private int arrowX(int dir) { return x + width - 2 - (dir < 0 ? 2 : 1) * NAV_ARROW; }

        private int arrowY() { return y + (NAV_ITEM_H - NAV_ARROW) / 2; }

        private boolean canMove(int dir) {
            int i = navItems.indexOf(this) + dir;
            return i >= 0 && i < navItems.size();
        }

        private void renderArrow(DrawContext ctx, int dir, int mouseX, int mouseY) {
            int ax = arrowX(dir), ay = arrowY();
            boolean enabled = canMove(dir);
            boolean hov = enabled && Draw.inside(mouseX, mouseY, ax, ay, NAV_ARROW, NAV_ARROW);
            if (hov) Draw.round(ctx, ax, ay, NAV_ARROW, NAV_ARROW, Theme.ACCENT_SURFACE);
            Draw.arrowIcon(ctx, ax + NAV_ARROW / 2, ay + NAV_ARROW / 2, dir < 0,
                    !enabled ? Theme.TEXT_DIM : hov ? Theme.ACCENT : Theme.TEXT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !isHovered(mouseX, mouseY)) return false;
            if (organizing) {
                for (int dir : new int[]{ -1, 1 }) {
                    if (canMove(dir) && Draw.inside(mouseX, mouseY, arrowX(dir), arrowY(), NAV_ARROW, NAV_ARROW)) {
                        Draw.playClick();
                        movePage(target, dir);
                        return true;
                    }
                }
            }
            showPage(target);
            return true;
        }
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
            int[][] buttons = navFooterButtons();
            for (int i = 0; i < buttons.length; i++) {
                int[] b = buttons[i];
                if (!Draw.inside(mouseX, mouseY, b[0], b[1], b[2], b[3])) continue;
                if (organizing && i == 0) {
                    if (cfg.pageOrder.isEmpty()) return true;
                    resetPageOrder();
                } else {
                    organizing = !organizing;
                    layoutNav();
                }
                Draw.playClick();
                return true;
            }
        }
        if (navList.mouseClicked(mouseX, mouseY, button)) return true;
        if (list.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (navList.mouseDragged(mouseX, mouseY)) return true;
        if (list.mouseDragged(mouseX, mouseY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        navList.mouseReleased();
        list.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (navList.mouseScrolled(mouseX, mouseY, amount)) return true;
        if (list.mouseScrolled(mouseX, mouseY, amount)) return true;
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (list.isCapturingKeys()) return list.keyPressed(keyCode);
        if (Keybinds.openConfig.matchesKey(keyCode, scanCode)) { close(); return true; }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            List<Page> pages = orderedPages();
            int dir = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1;
            showPage(pages.get((pages.indexOf(page) + dir + pages.size()) % pages.size()));
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
