package com.hamtabot.obfutilities.kit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import com.hamtabot.obfutilities.util.JsonFiles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the DeluxeMenus /kit menu of each server and keeps each kit's cooldown as a real-time timestamp. */
public final class KitTracker {

    public enum Status { UNKNOWN, NOT_OWNED, READY, COOLDOWN }

    /** Both servers sit behind the same proxy and open their own /kit menu, told apart by its size. */
    public enum Server {
        ONEBLOCK("OneBlock", 36), SURVIE("Survie", 54);

        public final String label;
        final int menuSize;

        Server(String label, int menuSize) { this.label = label; this.menuSize = menuSize; }
    }

    public record Kit(Server server, String key, String name, int slot, String cmiId, boolean tiered) {
        String cmiId(int tier) { return tiered ? cmiId + tier : cmiId; }
    }

    public static final List<Kit> KITS = List.of(
            new Kit(Server.ONEBLOCK, "joueur",    "Joueur",      6,  "joueur",        false),
            new Kit(Server.ONEBLOCK, "weak",      "OneWeak",     7,  "oneweak",       false),
            new Kit(Server.ONEBLOCK, "strong",    "OneStrong",   15, "onestrong",     false),
            new Kit(Server.ONEBLOCK, "master",    "OneMaster",   16, "onemaster",     false),
            new Kit(Server.ONEBLOCK, "legend",    "OneLegend",   24, "onelegend",     false),
            new Kit(Server.ONEBLOCK, "supreme",   "OneSupreme",  25, "onesupreme",    false),
            new Kit(Server.ONEBLOCK, "empereur",  "OneEmpereur", 33, "oneempereur",   false),
            new Kit(Server.ONEBLOCK, "eternel",   "OneÉternel",  34, "oneeternel",    false),
            new Kit(Server.ONEBLOCK, "cles",      "Clés",        1,  "clestier",      true),
            new Kit(Server.ONEBLOCK, "netherite", "Netherite",   2,  "netheritetier", true),
            new Kit(Server.ONEBLOCK, "monnaie",   "Monnaie",     3,  "monnaietier",   true),
            // survie keys are prefixed so their saved cooldowns and HUD toggles never mix with OneBlock's
            new Kit(Server.SURVIE, "survie_nourriture", "Nourriture",   20, "nourriture",  false),
            new Kit(Server.SURVIE, "survie_aventurier", "Aventurier",   21, "aventurier",  false),
            new Kit(Server.SURVIE, "survie_outils",     "Outils",       22, "outils",      false),
            new Kit(Server.SURVIE, "survie_oceans",     "Océans",       23, "oceans",      false),
            new Kit(Server.SURVIE, "survie_enchanteur", "Enchantement", 24, "enchanteur",  false),
            new Kit(Server.SURVIE, "survie_quartz",     "Quartz",       25, "quartz",      false),
            new Kit(Server.SURVIE, "survie_joueur",     "Joueur",       28, "joueur",      false),
            new Kit(Server.SURVIE, "survie_weak",       "OneWeak",      29, "oneweak",     false),
            new Kit(Server.SURVIE, "survie_strong",     "OneStrong",    30, "onestrong",   false),
            new Kit(Server.SURVIE, "survie_master",     "OneMaster",    31, "onemaster",   false),
            new Kit(Server.SURVIE, "survie_legend",     "OneLegend",    32, "onelegend",   false),
            new Kit(Server.SURVIE, "survie_supreme",    "OneSupreme",   33, "onesupreme",  false),
            new Kit(Server.SURVIE, "survie_empereur",   "OneEmpereur",  34, "oneempereur", false));

    public static List<Kit> kits(Server server) {
        return KITS.stream().filter(k -> k.server() == server).toList();
    }

    public static class State {
        public Status status = Status.UNKNOWN;
        public long readyAt;
        public long cooldownMs;
        public long claimedAt;
        public int tier;
    }

    private static final Path FILE = Paths.get("config/obfutilities_kits.json");
    private static final Gson GSON = new Gson();

    private static final int MAX_MENU_SIZE = 54, CLAIM_ALL_SLOT = 30;
    private static final String CLAIM_ALL_MARKER = "tous les kits disponibles";
    private static final String DURATION_MARKER = "ᴅᴀɴѕ";
    // the survie menu prints its cooldown on the line after this one
    private static final String DURATION_NEXT_LINE_MARKER = "Temps restant avant prochain kit";
    private static final Pattern TIER = Pattern.compile("ᴛɪᴇʀ\\s*:\\s*(\\d)");
    private static final Pattern CLOCK = Pattern.compile("(\\d+):(\\d{1,2})(?::(\\d{1,2}))?");
    private static final Pattern PART = Pattern.compile("(\\d+)\\s*([\\p{L}]*)");
    private static final Pattern CLAIM_COMMAND = Pattern.compile("^(?:cmi\\s+)?c?kits?\\s+(\\S+)", Pattern.CASE_INSENSITIVE);

    private static final long CLAIM_LEARN_WINDOW_MS = 60_000;
    private static final long SCAN_RETRY_MS = 1000, SCAN_GIVE_UP_MS = 60_000;
    private static final long SCAN_TIMEOUT_MS = 5000;

    private static Map<String, Map<String, State>> accounts = new HashMap<>();

    private static long pendingScanAt, pendingScanDeadline, awaitingUntil;
    private static boolean manualScan, silentScan;
    private static int syncIdBeforeScan = -1;
    private static final ItemStack[] lastSeen = new ItemStack[MAX_MENU_SIZE];
    private static int lastSyncId = -1;

    private KitTracker() {}

    public static void load() {
        Map<String, Map<String, State>> d = JsonFiles.read(FILE, new TypeToken<Map<String, Map<String, State>>>() {}.getType(), GSON);
        if (d != null) accounts = d;
        accounts.values().removeIf(m -> m == null);
        for (Map<String, State> m : accounts.values()) m.values().removeIf(s -> s == null || s.status == null);
    }

    private static void save() { JsonFiles.write(FILE, accounts, GSON); }

    private static Map<String, State> states() {
        return accounts.computeIfAbsent(MinecraftClient.getInstance().getSession().getUuid(), k -> new HashMap<>());
    }

    public static State state(Kit kit) { return states().computeIfAbsent(kit.key(), k -> new State()); }

    public static boolean hasData() {
        for (Kit k : KITS) if (state(k).status != Status.UNKNOWN) return true;
        return false;
    }

    /** The server whose /kit menu was opened last, which is the one the player is on. */
    public static Server activeServer() {
        try {
            return Server.valueOf(OBFUtilities.config.kitServer);
        } catch (RuntimeException e) {
            return Server.ONEBLOCK;
        }
    }

    public static void setActiveServer(Server server) {
        if (server == activeServer()) return;
        OBFUtilities.config.kitServer = server.name();
        OBFUtilities.config.save();
    }

    public static long remaining(State s) { return Math.max(0, s.readyAt - System.currentTimeMillis()); }

    public static String displayName(Kit kit) {
        int tier = state(kit).tier;
        return kit.tiered() && tier > 0 ? kit.name() + " T" + tier : kit.name();
    }

    public static String format(long ms) {
        long s = ms / 1000, d = s / 86400, h = s / 3600 % 24, m = s / 60 % 60;
        if (d > 0) return String.format("%dj %02dh", d, h);
        if (h > 0) return String.format("%dh %02dm", h, m);
        if (m > 0) return String.format("%dm %02ds", m, s % 60);
        return s % 60 + "s";
    }

    // ---- scanning ----

    /** Opens /kit as soon as no other screen is in the way, reads it and closes it again. */
    public static void scanNow() {
        long now = System.currentTimeMillis();
        manualScan = true;
        pendingScanAt = now;
        pendingScanDeadline = now + SCAN_GIVE_UP_MS;
    }

    public static boolean isScanning() { return pendingScanAt != 0 || awaitingUntil != 0; }

    public static void tick(MinecraftClient client) {
        if (!OBFUtilities.config.kitsEnabled) {
            pendingScanAt = awaitingUntil = 0;
            return;
        }
        long now = System.currentTimeMillis();
        GenericContainerScreenHandler menu = openKitMenu(client);
        if (menu != null) read(menu, menuServer(menu));
        else lastSyncId = -1;

        if (pendingScanAt != 0 && now >= pendingScanAt) startScan(client, menu, now);
        if (awaitingUntil != 0) {
            if (menu != null && menu.syncId != syncIdBeforeScan) finishScan(client, true);
            else if (now >= awaitingUntil) finishScan(client, false);
        }
        notifyReady(client, now);
    }

    private static void startScan(MinecraftClient client, GenericContainerScreenHandler menu, long now) {
        if (client.currentScreen != null && menu == null) {
            if (now < pendingScanDeadline) pendingScanAt = now + SCAN_RETRY_MS;
            else pendingScanAt = 0;
            return;
        }
        pendingScanAt = 0;
        silentScan = menu == null;
        syncIdBeforeScan = menu != null ? menu.syncId : -1;
        awaitingUntil = now + SCAN_TIMEOUT_MS;
        client.player.networkHandler.sendChatCommand("kit");
    }

    private static void finishScan(MinecraftClient client, boolean success) {
        awaitingUntil = 0;
        if (success && silentScan) client.player.closeHandledScreen();
        if (manualScan) {
            client.player.sendMessage(Text.literal(success ? "Cooldowns des kits mis à jour." : "Le menu /kit ne s'est pas ouvert.")
                    .formatted(success ? Formatting.GREEN : Formatting.RED), false);
        }
        manualScan = false;
    }

    // ---- menu reading ----

    private static GenericContainerScreenHandler openKitMenu(MinecraftClient client) {
        if (!(client.currentScreen instanceof GenericContainerScreen screen)) return null;
        GenericContainerScreenHandler handler = screen.getScreenHandler();
        return menuServer(handler) != null ? handler : null;
    }

    /** Which server's /kit menu this is, or null when it isn't one. */
    private static Server menuServer(ScreenHandler handler) {
        if (!(handler instanceof GenericContainerScreenHandler h)) return null;
        int size = h.getRows() * 9;
        if (size == Server.ONEBLOCK.menuSize) {
            for (String line : lore(h.getSlot(CLAIM_ALL_SLOT).getStack())) {
                if (line.contains(CLAIM_ALL_MARKER)) return Server.ONEBLOCK;
            }
        } else if (size == Server.SURVIE.menuSize) {
            for (Kit kit : kits(Server.SURVIE)) {
                for (String line : lore(h.getSlot(kit.slot()).getStack())) {
                    if (line.contains("Kit disponible") || line.contains("possédez pas ce kit")) return Server.SURVIE;
                }
            }
        }
        return null;
    }

    private static void read(GenericContainerScreenHandler menu, Server server) {
        if (menu.syncId != lastSyncId) {
            lastSyncId = menu.syncId;
            Arrays.fill(lastSeen, null);
            setActiveServer(server);
        }
        boolean changed = false;
        for (Kit kit : kits(server)) {
            ItemStack stack = menu.getSlot(kit.slot()).getStack();
            if (stack == lastSeen[kit.slot()]) continue;
            lastSeen[kit.slot()] = stack;
            changed |= apply(kit, lore(stack));
        }
        if (changed) save();
    }

    private static boolean apply(Kit kit, List<String> lore) {
        State s = state(kit);
        Status status = null;
        long remaining = 0, granularity = 0;
        boolean durationNext = false;
        for (String line : lore) {
            if (line.contains("possédez pas")) status = Status.NOT_OWNED;
            else if (line.contains("déjà récupéré") || line.contains("Kit disponible : Non")) status = Status.COOLDOWN;
            else if (line.contains("Kit disponible : Oui")) status = Status.READY;
            else if (line.contains("récupérer le kit") && status == null) status = Status.READY;
            Matcher tier = TIER.matcher(line);
            if (tier.find()) s.tier = Integer.parseInt(tier.group(1));
            int at = line.indexOf(DURATION_MARKER);
            String duration = at >= 0 ? line.substring(at + DURATION_MARKER.length()) : durationNext ? line : null;
            durationNext = line.contains(DURATION_NEXT_LINE_MARKER);
            if (duration != null) {
                long[] d = parseDuration(duration);
                if (d != null) { remaining = d[0]; granularity = d[1]; }
            }
        }
        if (status == null) return false;
        if (status == Status.COOLDOWN && remaining <= 0) status = Status.READY;

        long now = System.currentTimeMillis();
        Status before = s.status;
        long readyBefore = s.readyAt;
        s.status = status;
        if (status != Status.COOLDOWN) {
            s.readyAt = 0;
            return before != status;
        }
        // the menu only shows whole units, so keep the earlier estimate while it still fits the displayed value
        long low = now + remaining, high = low + granularity;
        if (s.readyAt < low || s.readyAt > high) s.readyAt = granularity > 1000 ? high : low;
        // first cooldown read right after a claim gives the kit's full cooldown
        if (s.claimedAt != 0 && now - s.claimedAt <= CLAIM_LEARN_WINDOW_MS) s.cooldownMs = roundToMinute(s.readyAt - s.claimedAt);
        s.claimedAt = 0;
        return before != status || readyBefore != s.readyAt;
    }

    private static long roundToMinute(long ms) {
        return ms < 120_000 ? ms : Math.round(ms / 60_000.0) * 60_000;
    }

    /** Returns {duration, smallest unit shown} in ms, or null when the text holds no duration. */
    public static long[] parseDuration(String text) {
        Matcher clock = CLOCK.matcher(text);
        if (clock.find()) {
            long a = Long.parseLong(clock.group(1)), b = Long.parseLong(clock.group(2));
            if (clock.group(3) != null) return new long[]{ (a * 3600 + b * 60 + Long.parseLong(clock.group(3))) * 1000, 1000 };
            return new long[]{ (a * 60 + b) * 1000, 1000 };
        }
        long total = 0, smallest = 0;
        boolean found = false;
        Matcher m = PART.matcher(text);
        while (m.find()) {
            long unit = unitMs(m.group(2).toLowerCase(Locale.ROOT));
            if (unit == 0) continue;
            total += Long.parseLong(m.group(1)) * unit;
            smallest = smallest == 0 ? unit : Math.min(smallest, unit);
            found = true;
        }
        return found ? new long[]{ total, smallest } : null;
    }

    private static long unitMs(String u) {
        if (u.isEmpty()) return 0;
        if (u.startsWith("se") && !u.startsWith("sec")) return 7 * 86_400_000L;
        return switch (u.charAt(0)) {
            case 'w' -> 7 * 86_400_000L;
            case 'd', 'j' -> 86_400_000L;
            case 'h' -> 3_600_000L;
            case 'm' -> u.startsWith("mo") ? 0 : 60_000L;
            case 's' -> 1000L;
            default -> 0;
        };
    }

    public static List<String> lore(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        NbtCompound display = stack.getSubNbt("display");
        if (display == null) return lines;
        NbtList list = display.getList("Lore", NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) {
            try {
                Text text = Text.Serializer.fromJson(list.getString(i));
                if (text != null) lines.add(Formatting.strip(text.getString()));
            } catch (Exception ignored) {}
        }
        return lines;
    }

    // ---- claims ----

    public static void onSlotClick(int syncId, int slot, int button, SlotActionType action) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!OBFUtilities.config.kitsEnabled || client.player == null || button != 0) return;
        if (action != SlotActionType.PICKUP && action != SlotActionType.QUICK_MOVE) return;
        ScreenHandler handler = client.player.currentScreenHandler;
        Server server = handler == null || handler.syncId != syncId ? null : menuServer(handler);
        if (server == null) return;

        // the menu stays open and refreshes after "Kit All", so the new cooldowns are read on the next ticks
        if (server == Server.ONEBLOCK && slot == CLAIM_ALL_SLOT) {
            for (Kit kit : kits(server)) if (state(kit).status == Status.READY) claimed(kit);
            save();
            return;
        }
        for (Kit kit : kits(server)) {
            if (kit.slot() == slot && state(kit).status == Status.READY) {
                claimed(kit);
                save();
            }
        }
    }

    public static void onCommandSent(String command) {
        if (!OBFUtilities.config.kitsEnabled) return;
        Matcher m = CLAIM_COMMAND.matcher(command.trim());
        if (!m.find()) return;
        String id = m.group(1).toLowerCase(Locale.ROOT);
        // both servers share kit ids, so a typed command counts for the server the player is on
        for (Kit kit : kits(activeServer())) {
            State s = state(kit);
            boolean match = kit.tiered() ? id.startsWith(kit.cmiId()) : id.equals(kit.cmiId());
            if (!match || s.status != Status.READY) continue;
            if (kit.tiered() && s.tier > 0 && !id.equals(kit.cmiId(s.tier))) continue;
            claimed(kit);
            save();
        }
    }

    private static void claimed(Kit kit) {
        State s = state(kit);
        long now = System.currentTimeMillis();
        s.status = Status.COOLDOWN;
        s.claimedAt = now;
        s.readyAt = s.cooldownMs > 0 ? now + s.cooldownMs : 0;
    }

    // ---- notifications ----

    private static void notifyReady(MinecraftClient client, long now) {
        List<String> ready = new ArrayList<>();
        Server active = activeServer();
        for (Kit kit : KITS) {
            State s = state(kit);
            if (s.status != Status.COOLDOWN || s.readyAt == 0 || s.readyAt > now) continue;
            s.status = Status.READY;
            s.readyAt = 0;
            ready.add(kit.server() == active ? displayName(kit) : displayName(kit) + " (" + kit.server().label + ")");
        }
        if (ready.isEmpty()) return;
        save();
        if (!OBFUtilities.config.kitTitleEnabled) return;
        Draw.title(ready.size() == 1 ? "Kit disponible" : ready.size() + " kits disponibles", Theme.SUCCESS,
                String.join(", ", ready) + " · /kit");
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f));
    }
}
