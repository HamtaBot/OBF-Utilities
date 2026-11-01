package com.hamtabot.obfutilities.objective;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.bossbar.ProgressBar;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Advances the player's goals from the same events as the stats HUD, and keeps their cards up to date. */
public final class Objectives {

    public static final int MAX = 10;
    private static final long SAVE_EVERY_MS = 30_000;

    private static final Map<ObjectiveEntry, ProgressBar> CARDS = new IdentityHashMap<>();
    private static final List<ProgressBar> SHOWN = new ArrayList<>();
    private static boolean dirty;
    private static long lastSave = System.currentTimeMillis();

    private Objectives() {}

    public static List<ObjectiveEntry> all() { return OBFUtilities.config.objectives; }

    public static void onBlockMined(String blockId) { add(ObjectiveEntry.Action.MINE, blockId, null, 1); }

    public static void onBlockPlaced(String blockId, String itemId) { add(ObjectiveEntry.Action.PLACE, blockId, itemId, 1); }

    public static void onMobKilled(int count) { add(ObjectiveEntry.Action.KILL, null, null, count); }

    private static void add(ObjectiveEntry.Action action, String blockId, String itemId, int amount) {
        for (ObjectiveEntry o : all()) {
            if (o.isDone() || !o.matches(action, blockId, itemId)) continue;
            o.progress = Math.min(o.target, o.progress + amount);
            dirty = true;
            ProgressBar card = CARDS.get(o);
            if (card != null) update(card, o);
            if (o.isDone()) complete(o);
        }
    }

    private static void complete(ObjectiveEntry o) {
        Draw.title("Objectif atteint", Theme.SUCCESS, o.displayName());
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f));
        save();
    }

    /** Progress changes with every block, so it is written to disk now and then rather than each time. */
    public static void tick() {
        if (dirty && System.currentTimeMillis() - lastSave >= SAVE_EVERY_MS) save();
    }

    public static void save() {
        lastSave = System.currentTimeMillis();
        dirty = false;
        OBFUtilities.config.save();
    }

    public static void restart(ObjectiveEntry o) {
        o.progress = 0;
        changed(o);
        save();
    }

    public static void remove(ObjectiveEntry o) {
        all().remove(o);
        changed(o);
        save();
    }

    /** Its card is rebuilt the next time it is needed, with the new name, icon and numbers. */
    public static void changed(ObjectiveEntry o) { CARDS.remove(o); }

    /** The cards to draw: a reached objective leaves the screen and stays in the config list. */
    public static List<ProgressBar> bars() {
        SHOWN.clear();
        for (ObjectiveEntry o : all()) if (o.showOnScreen && !o.isDone()) SHOWN.add(card(o));
        return SHOWN;
    }

    public static ProgressBar card(ObjectiveEntry o) {
        ProgressBar card = CARDS.get(o);
        if (card == null) {
            card = ProgressBar.objective(o.displayName(), o.action().color, o.icon());
            update(card, o);
            card.fill.set(card.target());
            CARDS.put(o, card);
        }
        return card;
    }

    private static void update(ProgressBar card, ObjectiveEntry o) {
        String unit = o.action().unit;
        if (o.countdown) card.updateObjective(o.remaining(), o.target, unit + " restants", o.isDone());
        else card.updateObjective(o.progress, o.target, unit, o.isDone());
    }
}
