package com.hamtabot.obfutilities.bossbar;

import com.hamtabot.obfutilities.ui.Anim;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import com.hamtabot.obfutilities.util.Format;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** One job, skill or quest boss bar, with every display string prepared when the server updates it. */
public final class ProgressBar {

    public enum Type { JOB, SKILL, QUEST }

    private record Look(String[] keys, int color, Item icon) {
        Look(int color, Item icon, String... keys) { this(keys, color, icon); }
    }

    private static final List<Look> JOB_LOOKS = List.of(
            new Look(Theme.DANGER,  Items.IRON_SWORD,  "chasseur"),
            new Look(Theme.SUCCESS, Items.WHEAT,       "farm"),
            new Look(Theme.PURPLE,  Items.BRICKS,      "poseur"),
            // Survie has Mineur instead of PoseurBlocs, so it can share the purple
            new Look(Theme.PURPLE,  Items.IRON_PICKAXE, "mineur"),
            new Look(Theme.WARNING, Items.IRON_AXE,    "bucheron"),
            new Look(Theme.ACCENT,  Items.FISHING_ROD, "pecheur"));

    // skill names come from the bar, in French or English depending on the plugin's language file
    private static final List<Look> SKILL_LOOKS = List.of(
            new Look(Theme.SUCCESS, Items.WHEAT,          "agri", "farm"),
            new Look(Theme.WARNING, Items.IRON_AXE,       "forest", "bucheron"),
            new Look(Theme.ACCENT,  Items.IRON_PICKAXE,   "minage", "mining", "mineur"),
            new Look(Theme.ACCENT,  Items.FISHING_ROD,    "peche", "fishing"),
            new Look(Theme.WARNING, Items.IRON_SHOVEL,    "excav"),
            new Look(Theme.SUCCESS, Items.BOW,            "tir", "arc", "archer"),
            new Look(Theme.ACCENT,  Items.IRON_CHESTPLATE, "defense"),
            new Look(Theme.DANGER,  Items.IRON_SWORD,     "combat", "fight"),
            new Look(Theme.SUCCESS, Items.IRON_BOOTS,     "endurance"),
            new Look(Theme.ACCENT,  Items.FEATHER,        "agil"),
            new Look(Theme.DANGER,  Items.GOLDEN_APPLE,   "soin", "guerison", "heal", "regen"),
            new Look(Theme.TEXT_MUTED, Items.ANVIL,       "forg"),
            new Look(Theme.PURPLE,  Items.BREWING_STAND,  "alchim"),
            new Look(Theme.PURPLE,  Items.ENCHANTED_BOOK, "enchant"),
            new Look(Theme.PURPLE,  Items.BLAZE_ROD,      "sorcel", "sorcery"));

    private static final Look DEFAULT_LOOK = new Look(Theme.ACCENT, Items.EXPERIENCE_BOTTLE);
    private static final Look QUEST_LOOK = new Look(Theme.WARNING, Items.WRITABLE_BOOK);

    static final int GRADIENT_STEPS = 16;
    private static final long LEVEL_UP_MS = 2500;

    public final Type type;
    private final String name;
    public final int color;
    public final ItemStack icon;
    public final int[] gradient = new int[GRADIENT_STEPS];
    public final Anim fill = new Anim(0f);

    private int level;
    public boolean maxed;
    float barPercent;
    private float progress;
    private boolean percentFromBar;
    /** {@code levelText} is empty for quests, which have no level. */
    public String levelText = "", progressText = "", gain = "";
    /** Width of the card these strings need, measured by the HUD; -1 once they change. */
    int cardWidth = -1;
    private long levelUpAt;
    private float percentFor = -1f;
    private String percentText;

    ProgressBar(Type type, String name) {
        this.type = type;
        this.name = name;
        Look look = switch (type) {
            case JOB -> look(JOB_LOOKS, name);
            case SKILL -> look(SKILL_LOOKS, name);
            case QUEST -> QUEST_LOOK;
        };
        color = look.color();
        icon = new ItemStack(look.icon());
        int dark = Draw.lerpColor(color, 0xFF000000, 0.45f);
        for (int i = 0; i < GRADIENT_STEPS; i++) gradient[i] = Draw.lerpColor(dark, color, i / (float) (GRADIENT_STEPS - 1));
    }

    private static Look look(List<Look> looks, String name) {
        String n = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        for (Look look : looks) {
            for (String key : look.keys()) if (n.contains(key)) return look;
        }
        return DEFAULT_LOOK;
    }

    /**
     * {@code level} is -1 for quests. The server's max level is a placeholder for "no limit" on jobs, so it is
     * never kept. {@code unit} follows the numbers ("XP", or what the quest counts).
     */
    void update(int level, double current, double needed, String gain, String unit) {
        boolean levelUp = level > 0 && this.level > 0 && level > this.level;
        this.level = level;
        maxed = false;
        percentFromBar = needed <= 0;
        // without a max to divide by, target() reads the server's own bar percent instead
        progress = percentFromBar ? 0f : (float) Math.max(0, Math.min(1, current / needed));
        levelText = level >= 0 ? String.valueOf(level) : "";
        progressText = Format.number((long) current) + " / " + Format.number((long) needed) + (unit.isEmpty() ? "" : " " + unit);
        this.gain = gain == null || gain.isBlank() ? "" : gain.startsWith("+") || gain.startsWith("-") ? gain : "+" + gain;
        cardWidth = -1;
        if (levelUp) {
            levelUpAt = System.currentTimeMillis();
            // start the new level from an empty bar instead of sliding back across it
            fill.set(0f);
        }
    }

    void updateMaxed(int level) {
        this.level = level;
        maxed = true;
        percentFromBar = false;
        progress = 1f;
        levelText = String.valueOf(level);
        progressText = "Niveau maximum";
        gain = "";
        cardWidth = -1;
    }

    public String name() { return name; }

    public float target() { return percentFromBar ? barPercent : progress; }

    public String percentText() {
        if (maxed) return "MAX";
        float t = target();
        if (t != percentFor || percentText == null) {
            percentFor = t;
            percentText = String.format(Locale.ROOT, "%.1f %%", t * 100f).replace('.', ',');
        }
        return percentText;
    }

    public boolean justLeveledUp() { return System.currentTimeMillis() - levelUpAt < LEVEL_UP_MS; }

    static ProgressBar preview(Type type) {
        ProgressBar bar = new ProgressBar(type, switch (type) {
            case JOB -> "Chasseur";
            case SKILL -> "Minage";
            case QUEST -> "Cassez 2500 Bûches d'acacia";
        });
        switch (type) {
            case JOB -> bar.update(874, 872956.10, 3064244, "12.5", "XP");
            case SKILL -> bar.update(24, 1830, 4200, "8.4", "XP");
            case QUEST -> bar.update(-1, 1240, 2500, "", "Bûches d'acacia");
        }
        bar.fill.set(bar.target());
        return bar;
    }
}
