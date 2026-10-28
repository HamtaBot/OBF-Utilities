package com.hamtabot.obfutilities.grade;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.kit.KitTracker;
import com.hamtabot.obfutilities.kit.KitTracker.Server;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import com.hamtabot.obfutilities.util.JsonFiles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads the DeluxeMenus /grade menu: grades bought with money last 30 days, grades bought with tokens are permanent. */
public final class GradeTracker {

    public enum Status { UNKNOWN, INACTIVE, TEMPORARY, PERMANENT }

    public record Grade(Server server, String key, String name, int slot) {}

    public static final List<Grade> GRADES = List.of(
            new Grade(Server.ONEBLOCK, "oneweak",     "OneWeak",     1),
            new Grade(Server.ONEBLOCK, "onestrong",   "OneStrong",   2),
            new Grade(Server.ONEBLOCK, "onemaster",   "OneMaster",   3),
            new Grade(Server.ONEBLOCK, "onelegend",   "OneLegend",   4),
            new Grade(Server.ONEBLOCK, "onesupreme",  "OneSupreme",  5),
            new Grade(Server.ONEBLOCK, "oneempereur", "OneEmpereur", 6),
            new Grade(Server.ONEBLOCK, "oneeternel",  "OneÉternel",  7),
            new Grade(Server.SURVIE, "survie_oneweak",     "OneWeak",     3),
            new Grade(Server.SURVIE, "survie_onestrong",   "OneStrong",   4),
            new Grade(Server.SURVIE, "survie_onemaster",   "OneMaster",   5),
            new Grade(Server.SURVIE, "survie_onelegend",   "OneLegend",   12),
            new Grade(Server.SURVIE, "survie_onesupreme",  "OneSupreme",  13),
            new Grade(Server.SURVIE, "survie_oneempereur", "OneEmpereur", 14));

    public static List<Grade> grades(Server server) {
        return GRADES.stream().filter(g -> g.server() == server).toList();
    }

    public static class State {
        public Status status = Status.UNKNOWN;
        public long expiresAt;
        public boolean warned;
    }

    private static final Path FILE = Paths.get("config/obfutilities_grades.json");
    private static final Gson GSON = new Gson();

    private static final int MAX_MENU_SIZE = 54;
    private static final String ACTIVE_MARKER = "Grade actif";
    private static final String EXPIRY_MARKER = "Expiration du grade dans";
    private static final long WARN_BEFORE_MS = 86_400_000L;

    private static final long SCAN_RETRY_MS = 1000, SCAN_GIVE_UP_MS = 60_000;
    private static final long SCAN_TIMEOUT_MS = 5000;

    private static Map<String, Map<String, State>> accounts = new HashMap<>();

    private static long pendingScanAt, pendingScanDeadline, awaitingUntil;
    private static boolean silentScan;
    private static int syncIdBeforeScan = -1;
    private static final ItemStack[] lastSeen = new ItemStack[MAX_MENU_SIZE];
    private static int lastSyncId = -1;

    private GradeTracker() {}

    public static void load() {
        Map<String, Map<String, State>> d = JsonFiles.read(FILE, new TypeToken<Map<String, Map<String, State>>>() {}.getType(), GSON);
        if (d != null) accounts = d;
        accounts.values().removeIf(m -> m == null);
        for (Map<String, State> m : accounts.values()) m.values().removeIf(s -> s == null || s.status == null);
    }

    private static void save() { JsonFiles.write(FILE, accounts, GSON); }

    public static State state(Grade grade) {
        return accounts.computeIfAbsent(MinecraftClient.getInstance().getSession().getUuid(), k -> new HashMap<>())
                .computeIfAbsent(grade.key(), k -> new State());
    }

    public static long remaining(State s) { return Math.max(0, s.expiresAt - System.currentTimeMillis()); }

    // ---- scanning ----

    /** Opens /grade as soon as no other screen is in the way, reads it and closes it again. */
    public static void scanNow() {
        long now = System.currentTimeMillis();
        pendingScanAt = now;
        pendingScanDeadline = now + SCAN_GIVE_UP_MS;
    }

    public static boolean isScanning() { return pendingScanAt != 0 || awaitingUntil != 0; }

    public static void tick(MinecraftClient client) {
        if (!OBFUtilities.config.gradesEnabled) {
            pendingScanAt = awaitingUntil = 0;
            return;
        }
        long now = System.currentTimeMillis();
        GenericContainerScreenHandler menu = null;
        Server server = null;
        if (client.currentScreen instanceof GenericContainerScreen screen) {
            menu = screen.getScreenHandler();
            server = menuServer(menu);
            if (server == null) menu = null;
        }
        if (menu != null) read(menu, server);
        else lastSyncId = -1;

        if (pendingScanAt != 0 && now >= pendingScanAt) startScan(client, menu, now);
        if (awaitingUntil != 0) {
            if (menu != null && menu.syncId != syncIdBeforeScan) finishScan(client, true);
            else if (now >= awaitingUntil) finishScan(client, false);
        }
        notifyExpiry(client, now);
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
        client.player.networkHandler.sendChatCommand("grade");
    }

    private static void finishScan(MinecraftClient client, boolean success) {
        awaitingUntil = 0;
        if (success && silentScan) client.player.closeHandledScreen();
        client.player.sendMessage(Text.literal(success ? "Grades mis à jour." : "Le menu /grade ne s'est pas ouvert.")
                .formatted(success ? Formatting.GREEN : Formatting.RED), false);
    }

    // ---- menu reading ----

    /** Which server's /grade menu this is: every grade slot of that server shows a "Grade actif" line. */
    private static Server menuServer(GenericContainerScreenHandler h) {
        int size = h.getRows() * 9;
        for (Server server : Server.values()) {
            boolean all = true;
            for (Grade g : grades(server)) {
                if (g.slot() >= size || !hasLine(KitTracker.lore(h.getSlot(g.slot()).getStack()), ACTIVE_MARKER)) { all = false; break; }
            }
            if (all) return server;
        }
        return null;
    }

    private static boolean hasLine(List<String> lore, String marker) {
        for (String line : lore) if (line.contains(marker)) return true;
        return false;
    }

    private static void read(GenericContainerScreenHandler menu, Server server) {
        if (menu.syncId != lastSyncId) {
            lastSyncId = menu.syncId;
            Arrays.fill(lastSeen, null);
            KitTracker.setActiveServer(server);
        }
        boolean changed = false;
        for (Grade grade : grades(server)) {
            ItemStack stack = menu.getSlot(grade.slot()).getStack();
            if (stack == lastSeen[grade.slot()]) continue;
            lastSeen[grade.slot()] = stack;
            changed |= apply(grade, KitTracker.lore(stack));
        }
        if (changed) save();
    }

    private static boolean apply(Grade grade, List<String> lore) {
        boolean seen = false, active = false;
        long[] expiry = null;
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains(ACTIVE_MARKER)) {
                seen = true;
                if (line.contains("Oui")) active = true;
            }
            int at = line.indexOf(EXPIRY_MARKER);
            if (at < 0) continue;
            active = true;
            // OneBlock prints the time on the same line, Survie on the next one
            String rest = line.substring(at + EXPIRY_MARKER.length()).replaceFirst("^[\\s:]+", "");
            if (rest.isEmpty() && i + 1 < lore.size()) rest = lore.get(i + 1);
            expiry = KitTracker.parseDuration(rest);
        }
        if (!seen) return false;

        State s = state(grade);
        Status before = s.status;
        long expiresBefore = s.expiresAt;
        // a held grade with no expiry was bought with tokens
        s.status = !active ? Status.INACTIVE : expiry == null ? Status.PERMANENT : Status.TEMPORARY;
        if (s.status != Status.TEMPORARY) {
            s.expiresAt = 0;
            s.warned = false;
            return before != s.status;
        }
        // the menu only shows whole units, so keep the earlier estimate while it still fits the displayed value
        long low = System.currentTimeMillis() + expiry[0], high = low + expiry[1];
        if (s.expiresAt < low || s.expiresAt > high) s.expiresAt = expiry[1] > 1000 ? high : low;
        if (remaining(s) > WARN_BEFORE_MS) s.warned = false;
        return before != s.status || expiresBefore != s.expiresAt;
    }

    // ---- notifications ----

    private static void notifyExpiry(MinecraftClient client, long now) {
        List<String> expired = new ArrayList<>(), soon = new ArrayList<>();
        Server active = KitTracker.activeServer();
        for (Grade grade : GRADES) {
            State s = state(grade);
            if (s.status != Status.TEMPORARY || s.expiresAt == 0) continue;
            String name = grade.server() == active ? grade.name() : grade.name() + " (" + grade.server().label + ")";
            if (s.expiresAt <= now) {
                s.status = Status.INACTIVE;
                s.expiresAt = 0;
                s.warned = false;
                expired.add(name);
            } else if (!s.warned && s.expiresAt - now <= WARN_BEFORE_MS) {
                s.warned = true;
                soon.add(name);
            }
        }
        if (expired.isEmpty() && soon.isEmpty()) return;
        save();
        if (!OBFUtilities.config.gradeTitleEnabled) return;
        if (!expired.isEmpty()) {
            Draw.title(expired.size() == 1 ? "Grade expiré" : expired.size() + " grades expirés", Theme.DANGER,
                    String.join(", ", expired) + " · /grade");
        } else {
            Draw.title("Grade bientôt expiré", Theme.WARNING, String.join(", ", soon) + " · moins de 24h");
        }
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f));
    }
}
