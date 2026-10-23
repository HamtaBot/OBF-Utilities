package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.util.Format;

public class ShadeBlockRow extends BlockEntryRow {

    private final ModConfig.BlockShadeEntry entry;

    public ShadeBlockRow(ModConfig.BlockShadeEntry entry, Runnable onDelete) {
        super(entry.blockId, Format.icon(entry.blockId, false), entry.enabled, onDelete);
        this.entry = entry;
    }

    @Override protected boolean isEnabled() { return entry.enabled; }

    @Override protected void toggle() { entry.enabled = !entry.enabled; }

    @Override protected String subtitle() { return entry.enabled ? "Texture sombre appliquée" : "Texture d'origine"; }
}
