package com.hamtabot.obfutilities.bossbar;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.mixin.BitmapGlyphAccessor;
import com.hamtabot.obfutilities.mixin.BossBarHudAccessor;
import com.hamtabot.obfutilities.mixin.GlyphRendererAccessor;
import com.hamtabot.obfutilities.mixin.TextRendererAccessor;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the server boss bars and sorts them into job (Jobs Reborn), skill (AureliumSkills), info
 * (CustomNameplates) and quest bars.
 * The server rewrites them every tick, so a bar is only re-parsed when its name object changes.
 */
public final class BossBars {

    public enum Kind { OTHER, JOB, SKILL, QUEST, INFO }

    // %jobfullname% [%joblevel%/9999] %jobxp%/%jobmaxxp% (%gain%)
    private static final Pattern JOB = Pattern.compile(
            "^\\s*(.+?)\\s*\\[(\\d+)\\s*/\\s*(\\d+)]\\s*([\\d.,]+)\\s*/\\s*([\\d.,]+)(?:\\s*\\(\\s*([^)]*?)\\s*\\))?");
    // Skill {skill} Niveau {level}: ({current_xp}/{level_xp}) (+{xp_gained})
    // the skill plugin shortens big amounts ("12k"), hence the optional suffix on its numbers
    private static final Pattern SKILL = Pattern.compile(
            "^\\s*Skill\\s+(.+?)\\s+Niveau\\s+([0-9IVXLCDM]+)\\s*:\\s*\\(\\s*([\\d.,]+[kKmM]?)\\s*/\\s*([\\d.,]+[kKmM]?)\\s*\\)(?:\\s*\\(\\s*([^)]*?)\\s*\\))?");
    // {skill} {level} (MAX)
    private static final Pattern SKILL_MAX = Pattern.compile("^\\s*(.+?)\\s+([0-9IVXLCDM]+)\\s*\\(MAX\\)\\s*$");
    // any other bar holding a progress "x/y": a quest, named by the text before it
    private static final Pattern PROGRESS = Pattern.compile("(\\d[\\d.,]*(?:[kKmM](?!\\p{L}))?)\\s*/\\s*(\\d[\\d.,]*(?:[kKmM](?!\\p{L}))?)");
    private static final Pattern NAME_END = Pattern.compile("[\\s\\-–:|(\\[]+$");
    private static final Pattern UNIT_EDGES = Pattern.compile("^[\\s)\\]]+|[\\s\\-–:|(\\[]+$");
    private static final Pattern LEGACY_CODE = Pattern.compile("§.");
    private static final int MAX_ICON_W = 16, WORD_GAP = 4;

    /** {@code dy} recentres a resource pack icon whose glyph sits higher or lower than the text line. */
    public record Piece(OrderedText text, int width, int gapBefore, float dy) {}

    /** {@code key} names the info across updates (its label, or its icon when it has none) so it can be hidden. */
    public record Chip(String key, List<Piece> pieces, int width, boolean loading) {}

    private static final class Entry {
        Text name;
        Kind kind = Kind.OTHER;
        ProgressBar bar;
        List<Chip> chips;
    }

    private static final Map<UUID, Entry> ENTRIES = new LinkedHashMap<>();
    private static final Map<ProgressBar.Type, List<ProgressBar>> PROGRESS_BARS = new EnumMap<>(ProgressBar.Type.class);
    static {
        for (ProgressBar.Type t : ProgressBar.Type.values()) PROGRESS_BARS.put(t, new ArrayList<>());
    }
    private static final List<List<Chip>> INFOS = new ArrayList<>();
    // every info seen since launch, with its latest value, for the list of the config screen
    private static final Map<String, Chip> KNOWN = new LinkedHashMap<>();
    private static final Map<String, Boolean> COLOURED = new HashMap<>();
    // resource pack URL of the current server, without its query string (which often carries a version hash)
    private static volatile String serverPack = "";
    // the HUD asks for the bars several times per frame; they are read once per frame (see newFrame)
    private static boolean synced;
    // chip keys hidden on the current server, rebuilt when the config list or the server changes
    private static final java.util.Set<String> HIDDEN = new java.util.HashSet<>();
    private static int hiddenVersion = -1;
    private static String hiddenPack;

    private BossBars() {}

    private static boolean active() {
        ModConfig cfg = OBFUtilities.config;
        return cfg.jobBarsEnabled || cfg.skillBarsEnabled || cfg.questBarsEnabled || cfg.infoBarsEnabled;
    }

    public static List<ProgressBar> bars(ProgressBar.Type type) {
        sync();
        return PROGRESS_BARS.get(type);
    }

    public static List<List<Chip>> infos() {
        sync();
        return INFOS;
    }

    public static Collection<Chip> knownInfos() {
        sync();
        return KNOWN.values();
    }

    /** The raw JSON of every server bar, to see how a new server builds its bars. */
    public static String rawBars() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return "";
        StringBuilder sb = new StringBuilder();
        for (ClientBossBar bar : ((BossBarHudAccessor) client.inGameHud.getBossBarHud()).obf$getBossBars().values()) {
            sb.append(Text.Serializer.toJson(bar.getName())).append('\n');
        }
        return sb.toString();
    }

    public static Chip knownInfo(String key) { return KNOWN.get(key); }

    /**
     * Each server's pack maps the same characters to different icons, so a hidden info is remembered for the
     * server it was hidden on.
     */
    private static String hiddenKey(Chip chip) { return serverPack + "|" + chip.key(); }

    public static boolean isInfoHidden(Chip chip) {
        List<String> list = OBFUtilities.config.infoBarsHidden;
        if (hiddenVersion != list.size() || !serverPack.equals(hiddenPack)) {
            HIDDEN.clear();
            String prefix = serverPack + "|";
            for (String k : list) if (k.startsWith(prefix)) HIDDEN.add(k.substring(prefix.length()));
            hiddenVersion = list.size();
            hiddenPack = serverPack;
        }
        return HIDDEN.contains(chip.key());
    }

    public static void setInfoHidden(Chip chip, boolean hidden) {
        List<String> list = OBFUtilities.config.infoBarsHidden;
        list.remove(hiddenKey(chip));
        if (hidden) list.add(hiddenKey(chip));
        hiddenVersion = -1;
        OBFUtilities.config.save();
    }

    /** Start of a HUD frame: the next request reads the bars again. */
    public static void newFrame() { synced = false; }

    public static void setServerPack(String url) {
        String pack = url == null ? "" : url.replaceFirst("[?#].*$", "");
        if (pack.equals(serverPack)) return;
        serverPack = pack;
        // the infos of the other server must not stay in the config list
        MinecraftClient.getInstance().execute(KNOWN::clear);
    }

    /**
     * Forgets everything parsed with the previous resource pack: which glyphs are icons, their offsets, the bars
     * and the infos seen. Called after every resource reload (switching server loads its pack) and on disconnect.
     */
    public static void reset() {
        ENTRIES.clear();
        KNOWN.clear();
        COLOURED.clear();
        for (List<ProgressBar> list : PROGRESS_BARS.values()) list.clear();
        INFOS.clear();
        InfoBarHud.resetWidth();
    }

    public static void disconnected() {
        serverPack = "";
        reset();
    }

    /**
     * Called before the vanilla boss bars are drawn: moves every bar into {@code backup} and puts back only
     * the ones the mod doesn't redraw. Returns false, leaving the map untouched, when nothing is hidden.
     */
    public static boolean hideReplaced(Map<UUID, ClientBossBar> bars, Map<UUID, ClientBossBar> backup) {
        if (!active() || bars.isEmpty()) return false;
        sync(bars);
        boolean any = false;
        for (UUID id : bars.keySet()) {
            if (hidden(ENTRIES.get(id))) { any = true; break; }
        }
        if (!any) return false;
        backup.putAll(bars);
        bars.clear();
        for (Map.Entry<UUID, ClientBossBar> e : backup.entrySet()) {
            if (!hidden(ENTRIES.get(e.getKey()))) bars.put(e.getKey(), e.getValue());
        }
        return true;
    }

    private static boolean hidden(Entry e) {
        if (e == null) return false;
        ModConfig cfg = OBFUtilities.config;
        return switch (e.kind) {
            case JOB -> cfg.jobBarsEnabled;
            case SKILL -> cfg.skillBarsEnabled;
            case QUEST -> cfg.questBarsEnabled;
            case INFO -> cfg.infoBarsEnabled;
            case OTHER -> false;
        };
    }

    private static void sync() {
        if (synced) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return;
        sync(((BossBarHudAccessor) client.inGameHud.getBossBarHud()).obf$getBossBars());
    }

    private static void sync(Map<UUID, ClientBossBar> bars) {
        synced = true;
        for (List<ProgressBar> list : PROGRESS_BARS.values()) list.clear();
        INFOS.clear();
        if (!active()) {
            ENTRIES.clear();
            return;
        }
        ENTRIES.keySet().retainAll(bars.keySet());
        for (ClientBossBar bar : bars.values()) {
            Entry e = ENTRIES.computeIfAbsent(bar.getUuid(), u -> new Entry());
            if (e.name != bar.getName()) {
                e.name = bar.getName();
                classify(e);
            }
            if (e.bar != null) {
                e.bar.barPercent = bar.getPercent();
                PROGRESS_BARS.get(e.bar.type).add(e.bar);
            } else if (e.kind == Kind.INFO) {
                INFOS.add(e.chips);
            }
        }
    }

    private static void classify(Entry e) {
        String plain = LEGACY_CODE.matcher(e.name.getString()).replaceAll("");
        e.chips = null;
        Matcher m = JOB.matcher(plain);
        if (m.find()) {
            bar(e, Kind.JOB, ProgressBar.Type.JOB, m.group(1).trim())
                    .update(parseLevel(m.group(2)), parseNumber(m.group(4)), parseNumber(m.group(5)), m.group(6), "XP");
            return;
        }
        m = SKILL.matcher(plain);
        if (m.find()) {
            bar(e, Kind.SKILL, ProgressBar.Type.SKILL, m.group(1).trim())
                    .update(parseLevel(m.group(2)), parseNumber(m.group(3)), parseNumber(m.group(4)), m.group(5), "XP");
            return;
        }
        m = SKILL_MAX.matcher(plain);
        if (m.find()) {
            bar(e, Kind.SKILL, ProgressBar.Type.SKILL, m.group(1).trim()).updateMaxed(parseLevel(m.group(2)));
            return;
        }
        // the info bars also hold "x/y" values (vote party), so they are recognised by their font before quests
        List<Chip> chips = chips(e.name, true);
        if (chips != null && !chips.isEmpty()) {
            e.bar = null;
            e.chips = chips;
            e.kind = Kind.INFO;
            for (Chip chip : chips) if (!chip.loading()) KNOWN.put(chip.key(), chip);
            return;
        }
        if (quest(e, plain)) return;
        e.bar = null;
        e.kind = Kind.OTHER;
    }

    /** Keeps the same bar while the name holds, so the fill animation and level-up detection carry over. */
    private static ProgressBar bar(Entry e, Kind kind, ProgressBar.Type type, String name) {
        if (e.bar == null || e.bar.type != type || !e.bar.name.equals(name)) e.bar = new ProgressBar(type, name);
        e.kind = kind;
        return e.bar;
    }

    private static boolean quest(Entry e, String plain) {
        Matcher m = PROGRESS.matcher(plain);
        int start = -1, end = -1;
        String cur = null, max = null;
        while (m.find()) {
            start = m.start();
            end = m.end();
            cur = m.group(1);
            max = m.group(2);
        }
        if (start < 0) return false;
        String name = NAME_END.matcher(plain.substring(0, start)).replaceAll("").trim();
        String unit = UNIT_EDGES.matcher(plain.substring(end)).replaceAll("").trim();
        // the Quests bar is only the progress placeholder (" - 120/2500 Bûches d'acacia"): what it counts names it
        if (name.isEmpty()) {
            name = unit.isEmpty() ? "Quête" : unit;
            unit = "";
        }
        bar(e, Kind.QUEST, ProgressBar.Type.QUEST, name).update(-1, parseNumber(cur), parseNumber(max), "", unit);
        return true;
    }

    /** Plain or roman level ("12" or "XII"). */
    private static int parseLevel(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        int total = 0, prev = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            int v = switch (Character.toUpperCase(s.charAt(i))) {
                case 'I' -> 1; case 'V' -> 5; case 'X' -> 10; case 'L' -> 50;
                case 'C' -> 100; case 'D' -> 500; case 'M' -> 1000; default -> 0;
            };
            total += v < prev ? -v : v;
            prev = Math.max(prev, v);
        }
        return total;
    }

    /**
     * Reads "872956.10", "1,234.5", "1.234.567" or "2500". With both separators the last one is the decimal
     * point; a lone separator followed by exactly three digits groups thousands.
     */
    private static double parseNumber(String s) {
        // "12k" / "1.5M": the separator before a suffix is always the decimal point
        double factor = 1;
        char suffix = s.isEmpty() ? ' ' : Character.toLowerCase(s.charAt(s.length() - 1));
        if (suffix == 'k' || suffix == 'm') {
            factor = suffix == 'k' ? 1_000 : 1_000_000;
            s = s.substring(0, s.length() - 1).replace(',', '.');
            try { return Double.parseDouble(s) * factor; } catch (NumberFormatException e) { return 0; }
        }
        int dot = s.lastIndexOf('.'), comma = s.lastIndexOf(',');
        int last = Math.max(dot, comma);
        if (last >= 0) {
            char sep = s.charAt(last);
            boolean both = dot >= 0 && comma >= 0;
            boolean repeated = s.indexOf(sep) != last;
            boolean thousands = !both && (repeated || s.length() - last - 1 == 3);
            String whole = s.substring(0, last).replaceAll("[.,]", "");
            s = thousands ? whole + s.substring(last + 1) : whole + "." + s.substring(last + 1);
        }
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * Turns a CustomNameplates bar into chips (icon, label, value). Background pieces and offset glyphs live in
     * the nameplates:default font and are dropped; the shifted text fonts are drawn with the vanilla font.
     * Returns null when the text uses no nameplates font and {@code requireNameplates} is set.
     */
    static List<Chip> chips(Text name, boolean requireNameplates) {
        ChipBuilder b = new ChipBuilder(MinecraftClient.getInstance().textRenderer);
        boolean[] nameplates = { false };
        name.visit((style, s) -> {
            Identifier font = style.getFont();
            boolean np = font.getNamespace().equals("nameplates");
            if (np) nameplates[0] = true;
            if (np && font.getPath().equals("default")) {
                // each background piece frames one value: it marks the end of the chip
                b.separate();
                return Optional.empty();
            }
            Identifier f = np ? Style.DEFAULT_FONT_ID : font;
            s.codePoints().forEach(cp -> b.accept(cp, f));
            return Optional.empty();
        }, Style.EMPTY);
        if (requireNameplates && !nameplates[0]) return null;
        return b.finish();
    }

    private static final class ChipBuilder {
        private final TextRenderer tr;
        private final List<Chip> chips = new ArrayList<>();
        private final List<Piece> icons = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();
        private final StringBuilder iconKey = new StringBuilder();
        private final StringBuilder labelKey = new StringBuilder();
        private int spaces;
        private boolean skipCode;

        ChipBuilder(TextRenderer tr) { this.tr = tr; }

        void accept(int cp, Identifier font) {
            // legacy colour codes sent raw inside the text (&f on Survie): drop the marker and its code
            if (skipCode) { skipCode = false; return; }
            if (cp == '§') { skipCode = true; return; }
            if (cp == ' ') { spaces++; return; }
            if (Character.isISOControl(cp) || Character.getType(cp) == Character.FORMAT) return;
            if (isIcon(cp, font) || isColoured(cp, font)) {
                OrderedText t = Text.literal(Character.toString(cp)).setStyle(Style.EMPTY.withFont(font)).asOrderedText();
                int w = tr.getWidth(t);
                if (w <= 0 || w > MAX_ICON_W) return;
                if (!text.isEmpty()) endChip();
                icons.add(new Piece(t, w, 0, iconOffset(cp, font)));
                iconKey.setLength(0);
                iconKey.appendCodePoint(cp);
            } else {
                if (spaces >= 2 && !text.isEmpty()) endChip();
                else if (spaces == 1 && !text.isEmpty()) text.append(' ');
                text.appendCodePoint(cp);
            }
            spaces = 0;
        }

        void separate() { spaces = Math.max(spaces, 2); }

        // pack icons use their own ascent and padding: line their visible centre up with a capital of the text font
        private float iconOffset(int cp, Identifier font) {
            return glyphCentre('A', Style.DEFAULT_FONT_ID) - glyphCentre(cp, font);
        }

        private float glyphCentre(int cp, Identifier font) {
            FontStorage storage = ((TextRendererAccessor) tr).obf$getFontStorage(font);
            if (storage.getGlyph(cp, false) instanceof BitmapGlyphAccessor g) {
                Float centre = visibleCentre(g);
                if (centre != null) return centre;
            }
            GlyphRendererAccessor r = (GlyphRendererAccessor) storage.getGlyphRenderer(cp);
            return (r.obf$getMinY() + r.obf$getMaxY()) / 2f;
        }

        private static Float visibleCentre(BitmapGlyphAccessor g) {
            try {
                NativeImage img = g.obf$getImage();
                int first = -1, last = -1;
                for (int row = 0; row < g.obf$getHeight(); row++) {
                    for (int col = 0; col < g.obf$getWidth(); col++) {
                        if (img.getColor(g.obf$getX() + col, g.obf$getY() + row) >>> 24 == 0) continue;
                        if (first < 0) first = row;
                        last = row;
                        break;
                    }
                }
                if (first < 0) return null;
                // same origin as the glyph renderer: the top of the glyph sits at 10 - ascent
                float top = 10 - g.obf$getAscent(), sf = g.obf$getScaleFactor();
                return top + (first + last + 1) * sf / 2f;
            } catch (RuntimeException e) {
                // image already released by a resource reload, or not readable pixel by pixel
                return null;
            }
        }

        private static boolean isSmallCaps(String word) {
            boolean small = false;
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (c < 0x80 && Character.isLetterOrDigit(c)) return false;
                String name = Character.getName(c);
                small |= name != null && name.contains("SMALL CAPITAL");
            }
            return small;
        }

        /**
         * Text glyphs are white and tinted by the style; a glyph drawn in colour is a pack icon, whatever its
         * character or font. Scanned once per glyph.
         */
        private boolean isColoured(int cp, Identifier font) {
            return COLOURED.computeIfAbsent(font + "#" + cp, k -> {
                FontStorage storage = ((TextRendererAccessor) tr).obf$getFontStorage(font);
                if (!(storage.getGlyph(cp, false) instanceof BitmapGlyphAccessor g)) return false;
                try {
                    NativeImage img = g.obf$getImage();
                    for (int row = 0; row < g.obf$getHeight(); row++) {
                        for (int col = 0; col < g.obf$getWidth(); col++) {
                            int c = img.getColor(g.obf$getX() + col, g.obf$getY() + row);
                            if (c >>> 24 == 0) continue;
                            int r = c & 0xFF, gr = (c >> 8) & 0xFF, b = (c >> 16) & 0xFF;
                            if (Math.abs(r - gr) > 24 || Math.abs(gr - b) > 24 || Math.abs(r - b) > 24) return true;
                        }
                    }
                } catch (RuntimeException e) {
                    return false;
                }
                return false;
            });
        }

        private static boolean isIcon(int cp, Identifier font) {
            boolean privateUse = cp >= 0xE000 && cp <= 0xF8FF;
            boolean vanillaFont = font.getNamespace().equals("minecraft")
                    && (font.getPath().equals("default") || font.getPath().equals("uniform"));
            return privateUse || !vanillaFont;
        }

        private void endChip() {
            if (icons.isEmpty() && text.isEmpty()) return;
            // one icon per info: the one right before the value (Survie puts a money bag before the mobcoin)
            if (icons.size() > 1) icons.subList(0, icons.size() - 1).clear();
            List<Piece> pieces = new ArrayList<>(icons);
            int width = 0;
            for (Piece p : icons) width += p.gapBefore() + p.width();
            // labels are written in small capitals on both servers, so anything else next to one is the value
            // (a claim name has no digit); without small capitals, words holding a digit are the values
            String[] words = text.toString().split(" ");
            boolean smallCapsLabels = false;
            for (String w : words) smallCapsLabels |= isSmallCaps(w);
            StringBuilder group = new StringBuilder();
            boolean groupValue = false;
            for (String w : words) {
                if (w.isEmpty()) continue;
                boolean value = smallCapsLabels ? !isSmallCaps(w) : w.chars().anyMatch(Character::isDigit);
                if (!group.isEmpty() && value != groupValue) {
                    width += addText(pieces, group.toString(), groupValue);
                    group.setLength(0);
                }
                if (!group.isEmpty()) group.append(' ');
                group.append(w);
                groupValue = value;
            }
            if (!group.isEmpty()) width += addText(pieces, group.toString(), groupValue);
            // icons first: a placeholder still loading shows other text next to the same icon
            String key = !iconKey.isEmpty() ? "icon:" + iconKey : labelKey.toString();
            boolean loading = text.toString().toLowerCase(Locale.ROOT).contains("chargement");
            chips.add(new Chip(key, pieces, width, loading));
            icons.clear();
            text.setLength(0);
            iconKey.setLength(0);
            labelKey.setLength(0);
        }

        private int addText(List<Piece> pieces, String s, boolean value) {
            int color = (value ? Theme.TEXT : Theme.ACCENT) & 0xFFFFFF;
            OrderedText t = Text.literal(s).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))).asOrderedText();
            int w = tr.getWidth(t);
            int gap = pieces.isEmpty() ? 0 : WORD_GAP;
            pieces.add(new Piece(t, w, gap, 0));
            if (!value) labelKey.append(labelKey.isEmpty() ? "" : " ").append(s);
            return gap + w;
        }

        List<Chip> finish() {
            endChip();
            return chips;
        }
    }
}
