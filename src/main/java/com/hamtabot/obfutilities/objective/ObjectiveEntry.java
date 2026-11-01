package com.hamtabot.obfutilities.objective;

import com.hamtabot.obfutilities.ui.Theme;
import com.hamtabot.obfutilities.util.Format;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** A goal set by the player. {@code blockId} narrows it to one block, empty counts every block. */
public class ObjectiveEntry {

    /** Colours follow the matching sections of the stats HUD. */
    public enum Action {
        MINE("Casser", "blocs", Theme.SUCCESS, Items.IRON_PICKAXE),
        PLACE("Poser", "blocs", Theme.ACCENT, Items.BRICKS),
        KILL("Tuer", "mobs", Theme.DANGER, Items.IRON_SWORD);

        public final String label, unit;
        public final int color;
        final Item icon;

        Action(String label, String unit, int color, Item icon) {
            this.label = label;
            this.unit = unit;
            this.color = color;
            this.icon = icon;
        }
    }

    public String  name         = "";
    public Action  action       = Action.MINE;
    public String  blockId      = "";
    public long    target       = 1000;
    public long    progress     = 0;
    /** The bar empties and shows what is left instead of filling up with what is done. */
    public boolean countdown    = false;
    public boolean showOnScreen = true;

    public ObjectiveEntry() {}

    public ObjectiveEntry(String name, Action action, String blockId, long target, boolean countdown) {
        this.name = name;
        this.action = action;
        this.blockId = blockId;
        this.target = target;
        this.countdown = countdown;
    }

    public Action action() { return action != null ? action : Action.MINE; }

    public boolean isDone() { return progress >= target; }

    public long remaining() { return Math.max(0, target - progress); }

    /** The name the player gave, or one built from what is counted. */
    public String displayName() {
        if (!name.isBlank()) return name;
        String what = blockId.isEmpty() ? action().unit : Format.blockName(blockId);
        return action().label + " " + Format.number(target) + " " + what;
    }

    ItemStack icon() {
        ItemStack block = blockId.isEmpty() ? ItemStack.EMPTY : Format.icon(blockId, false);
        return block.isEmpty() ? new ItemStack(action().icon) : block;
    }

    boolean matches(Action action, String blockId, String itemId) {
        if (action() != action) return false;
        return this.blockId.isEmpty() || this.blockId.equals(blockId) || this.blockId.equals(itemId);
    }
}
