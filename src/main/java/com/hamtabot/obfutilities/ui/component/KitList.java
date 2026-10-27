package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.kit.KitHud;
import com.hamtabot.obfutilities.kit.KitTracker;
import com.hamtabot.obfutilities.ui.Anim;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Live kit statuses, with a switch per line choosing whether the kit shows in the HUD. */
public class KitList extends Component {

    private static final int LINE = 16, PAD = 4;

    private final List<KitTracker.Kit> kits = KitTracker.KITS;
    private final Anim[] knobs = new Anim[kits.size()];

    public KitList() {
        for (int i = 0; i < kits.size(); i++) knobs[i] = new Anim(KitHud.isListed(kits.get(i)) ? 1f : 0f);
    }

    @Override public int getHeight() { return PAD * 2 + LINE * kits.size(); }

    private int lineY(int i) { return y + PAD + i * LINE; }

    private int lineAt(double mx, double my) {
        if (!isHovered(mx, my)) return -1;
        int i = (int) Math.floor((my - y - PAD) / LINE);
        return i >= 0 && i < kits.size() ? i : -1;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.card(ctx, x, y, width, getHeight(), Theme.SURFACE, Theme.BORDER);
        int hovered = lineAt(mouseX, mouseY);
        for (int i = 0; i < kits.size(); i++) {
            KitTracker.Kit kit = kits.get(i);
            int ly = lineY(i), ty = ly + (LINE - 8) / 2 + 1;
            if (i == hovered) Draw.round(ctx, x + 2, ly, width - 4, LINE, Theme.SURFACE_HOVER);

            boolean listed = KitHud.isListed(kit);
            Draw.toggle(ctx, x + Theme.ROW_PAD, ly + (LINE - Draw.SWITCH_H) / 2, knobs[i].update(listed ? 1f : 0f, 16f), i == hovered);
            int nameX = x + Theme.ROW_PAD + Draw.SWITCH_W + 8;
            ctx.drawText(tr(), KitTracker.displayName(kit), nameX, ty, listed ? Theme.TEXT : Theme.TEXT_MUTED, false);

            KitTracker.State s = KitTracker.state(kit);
            String status;
            int color;
            switch (s.status) {
                case READY -> { status = "Disponible"; color = Theme.SUCCESS; }
                case COOLDOWN -> {
                    status = s.readyAt == 0 ? "Récupéré, en attente du menu" : "Dans " + KitTracker.format(KitTracker.remaining(s));
                    color = Theme.WARNING;
                }
                case NOT_OWNED -> { status = "Non possédé"; color = Theme.TEXT_DIM; }
                default -> { status = "Inconnu"; color = Theme.TEXT_DIM; }
            }
            ctx.drawText(tr(), status, x + width - Theme.ROW_PAD - tr().getWidth(status), ty, color, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = button == 0 ? lineAt(mouseX, mouseY) : -1;
        if (i < 0) return false;
        KitHud.setListed(kits.get(i), !KitHud.isListed(kits.get(i)));
        Draw.playClick();
        return true;
    }
}
