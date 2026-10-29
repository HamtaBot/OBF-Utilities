package com.hamtabot.obfutilities.kit;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.hud.ListCardHud;
import com.hamtabot.obfutilities.ui.HudPos;
import com.hamtabot.obfutilities.ui.Theme;

import java.util.ArrayList;
import java.util.List;

public final class KitHud extends ListCardHud {

    public static final KitHud INSTANCE = new KitHud();
    private static final String EMPTY = "Ouvre /kit";

    private KitHud() { super("Kits", Theme.ACCENT, "00h 00m", 10, 140); }

    @Override
    public boolean isShown() {
        ModConfig cfg = OBFUtilities.config;
        return cfg.kitsEnabled && cfg.kitHudEnabled;
    }

    @Override public float scale() { return OBFUtilities.config.kitHudScale; }

    @Override protected void storeScale(float s) { OBFUtilities.config.kitHudScale = s; }

    @Override protected HudPos pos() { return OBFUtilities.config.kitHudPos; }

    public static boolean isListed(KitTracker.Kit kit) { return !OBFUtilities.config.kitHudHidden.contains(kit.key()); }

    public static void setListed(KitTracker.Kit kit, boolean listed) {
        List<String> hidden = OBFUtilities.config.kitHudHidden;
        hidden.remove(kit.key());
        if (!listed) hidden.add(kit.key());
        OBFUtilities.config.save();
    }

    @Override
    protected List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (KitTracker.Kit kit : KitTracker.kits(KitTracker.activeServer())) {
            if (!isListed(kit)) continue;
            KitTracker.State s = KitTracker.state(kit);
            switch (s.status) {
                case READY -> rows.add(new Row(KitTracker.displayName(kit), "Prêt", Theme.SUCCESS));
                case COOLDOWN -> rows.add(s.readyAt == 0
                        ? new Row(KitTracker.displayName(kit), "?", Theme.TEXT_DIM)
                        : new Row(KitTracker.displayName(kit), KitTracker.format(KitTracker.remaining(s)), Theme.TEXT_MUTED));
                default -> {}
            }
        }
        if (rows.isEmpty()) rows.add(new Row(EMPTY, "", Theme.TEXT_DIM));
        return rows;
    }
}
