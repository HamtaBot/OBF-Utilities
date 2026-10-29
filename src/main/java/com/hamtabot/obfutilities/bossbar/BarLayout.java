package com.hamtabot.obfutilities.bossbar;

import com.hamtabot.obfutilities.ui.HudElement;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Places the boss bar blocks (infos, jobs, skills, quests) so they never overlap: they are laid out in a fixed
 * order and a block touching one already placed is pushed just below it. Blocks grow while bars stack up, so
 * this runs on every frame, once, and the result is shared by rendering and the HUD editor's hit tests.
 */
public final class BarLayout {

    /** A block that knows where it would sit on its own; null when it has nothing to show. */
    public interface Element {
        int[] rawBounds(boolean editing);
    }

    /** A block the HUD editor moves and resizes. */
    public interface Block extends Element, HudElement {
        /** Where the block is drawn in the HUD editor, after the other blocks were moved out of its way. */
        @Override
        default int[] bounds() {
            int[] b = BarLayout.bounds(this, true);
            return b != null ? b : new int[4];
        }
    }

    private static final int GAP = 4;

    private static final List<Block> BLOCKS = List.of(InfoBarHud.BLOCK, ProgressHud.JOBS, ProgressHud.SKILLS, ProgressHud.QUESTS);
    private static final Map<Element, int[]> PLACED = new IdentityHashMap<>();
    private static final List<int[]> TAKEN = new ArrayList<>();
    private static boolean valid, validEditing;
    /** The block being dragged in the editor goes first, so it never jumps away from the mouse. */
    private static Block priority;

    private BarLayout() {}

    /** The blocks in placement order. */
    public static List<Block> blocks() { return BLOCKS; }

    public static void invalidate() { valid = false; }

    /** Once per rendered frame: the bars and the layout are read again. */
    public static void newFrame() {
        BossBars.newFrame();
        valid = false;
    }

    public static void setPriority(Block b) {
        priority = b;
        invalidate();
    }

    public static int[] bounds(Element e, boolean editing) {
        if (!valid || validEditing != editing) resolve(editing);
        return PLACED.get(e);
    }

    private static void resolve(boolean editing) {
        PLACED.clear();
        TAKEN.clear();
        int screenH = MinecraftClient.getInstance().getWindow().getScaledHeight();
        if (priority != null) place(priority, editing, screenH);
        for (Block b : BLOCKS) if (b != priority) place(b, editing, screenH);
        valid = true;
        validEditing = editing;
    }

    private static void place(Block e, boolean editing, int screenH) {
        int[] r = e.rawBounds(editing);
        if (r == null) return;
        // every push moves the block strictly down, so this ends after at most one pass per placed block
        for (int pass = 0; pass <= TAKEN.size(); pass++) {
            int[] hit = null;
            for (int[] t : TAKEN) if (overlaps(r, t)) { hit = t; break; }
            if (hit == null) break;
            r[1] = hit[1] + hit[3] + GAP;
        }
        // pushed off the bottom: stay on screen, against its bottom edge
        if (r[1] + r[3] > screenH) r[1] = Math.max(0, screenH - r[3]);
        TAKEN.add(r);
        PLACED.put(e, r);
    }

    private static boolean overlaps(int[] a, int[] b) {
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
    }
}
