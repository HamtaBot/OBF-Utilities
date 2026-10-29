package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.bossbar.BossBars;
import com.hamtabot.obfutilities.bossbar.InfoBarHud;
import com.hamtabot.obfutilities.ui.Anim;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** The infos read from the server bars with their live value, and a switch per line choosing whether it is shown. */
public class InfoChipList extends Component {

    private static final int LINE = 16, PAD = 4;

    private final List<String> keys = new ArrayList<>();
    private final List<Anim> knobs = new ArrayList<>();

    public InfoChipList() {
        for (BossBars.Chip chip : BossBars.knownInfos()) {
            keys.add(chip.key());
            knobs.add(new Anim(BossBars.isInfoHidden(chip) ? 0f : 1f));
        }
    }

    public boolean isEmpty() { return keys.isEmpty(); }

    @Override public int getHeight() { return PAD * 2 + LINE * keys.size(); }

    private int lineAt(double mx, double my) {
        if (!isHovered(mx, my)) return -1;
        int i = (int) Math.floor((my - y - PAD) / LINE);
        return i >= 0 && i < keys.size() ? i : -1;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.card(ctx, x, y, width, getHeight(), Theme.SURFACE, Theme.BORDER);
        int hovered = lineAt(mouseX, mouseY);
        for (int i = 0; i < keys.size(); i++) {
            BossBars.Chip chip = BossBars.knownInfo(keys.get(i));
            if (chip == null) continue;
            int ly = y + PAD + i * LINE, ty = ly + (LINE - 8) / 2 + 1;
            if (i == hovered) Draw.round(ctx, x + 2, ly, width - 4, LINE, Theme.SURFACE_HOVER);

            boolean shown = !BossBars.isInfoHidden(chip);
            Draw.toggle(ctx, x + Theme.ROW_PAD, ly + (LINE - Draw.SWITCH_H) / 2, knobs.get(i).update(shown ? 1f : 0f, 16f), i == hovered);
            InfoBarHud.drawChip(ctx, tr(), chip, x + Theme.ROW_PAD + Draw.SWITCH_W + 8, ty, InfoBarHud.ALL);
            String state = shown ? "Affiché" : "Masqué";
            ctx.drawText(tr(), state, x + width - Theme.ROW_PAD - tr().getWidth(state), ty, shown ? Theme.SUCCESS : Theme.TEXT_DIM, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = button == 0 ? lineAt(mouseX, mouseY) : -1;
        if (i < 0) return false;
        BossBars.Chip chip = BossBars.knownInfo(keys.get(i));
        if (chip == null) return false;
        BossBars.setInfoHidden(chip, !BossBars.isInfoHidden(chip));
        Draw.playClick();
        return true;
    }
}
