package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import com.hamtabot.obfutilities.util.Format;
import net.minecraft.client.gui.DrawContext;

public class CustomBlockRow extends BlockEntryRow {

    private static final int MODE_W = 44;

    private final ModConfig.CustomBlockEntry entry;
    private final Runnable onModeChange;

    public CustomBlockRow(ModConfig.CustomBlockEntry entry, Runnable onModeChange, Runnable onDelete) {
        super(entry.blockId, Format.icon(entry.blockId, true), entry.enabled, onDelete);
        this.entry = entry;
        this.onModeChange = onModeChange;
    }

    @Override protected boolean isEnabled() { return entry.enabled; }

    @Override protected void toggle() { entry.enabled = !entry.enabled; }

    @Override protected String subtitle() { return entry.trackPlaced ? "Compte les blocs posés" : "Compte les blocs minés"; }

    @Override protected int extraWidth() { return MODE_W; }

    @Override
    protected void renderExtra(DrawContext ctx, int ex, int mouseX, int mouseY) {
        int by = ctrlY(Theme.BUTTON_H);
        Draw.button(ctx, tr(), ex, by, MODE_W, Theme.BUTTON_H, entry.trackPlaced ? "Posé" : "Miné",
                Draw.ButtonStyle.SECONDARY, Draw.inside(mouseX, mouseY, ex, by, MODE_W, Theme.BUTTON_H), true);
    }

    @Override
    protected boolean clickExtra(double mouseX, double mouseY, int ex) {
        if (!Draw.inside(mouseX, mouseY, ex, ctrlY(Theme.BUTTON_H), MODE_W, Theme.BUTTON_H)) return false;
        entry.trackPlaced = !entry.trackPlaced;
        Draw.playClick();
        onModeChange.run();
        return true;
    }
}
