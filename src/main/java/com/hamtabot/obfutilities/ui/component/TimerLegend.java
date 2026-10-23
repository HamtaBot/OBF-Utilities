package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;

public class TimerLegend extends Component {

    private static final int BTN = 14, LINE = 18;

    private static final String[] LABELS = {
            "Démarrer ou mettre en pause",
            "Relancer depuis le début",
            "Modifier le nom, le type ou la durée",
            "Afficher le timer à l'écran",
            "Supprimer le timer"
    };

    @Override public int getHeight() { return Theme.ROW_PAD * 2 + LINE * LABELS.length - (LINE - BTN); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        int h = getHeight();
        Draw.tintedCard(ctx, x, y, width, h, Theme.ACCENT);

        int bx = x + Theme.ROW_PAD + 3;
        int textX = bx + Draw.SWITCH_W + 8;
        for (int i = 0; i < LABELS.length; i++) {
            int by = y + Theme.ROW_PAD + i * LINE;
            int cx = bx + BTN / 2, cy = by + BTN / 2;
            if (i <= 2) {
                Draw.button(ctx, tr(), bx, by, BTN, BTN, "", i == 0 ? Draw.ButtonStyle.PRIMARY : Draw.ButtonStyle.SECONDARY, false, true);
            }
            switch (i) {
                case 0 -> Draw.playIcon(ctx, cx, cy, 0xFFFFFFFF);
                case 1 -> Draw.restartIcon(ctx, cx, cy, 0xFFFFFFFF);
                case 2 -> Draw.editIcon(ctx, cx, cy, 0xFFFFFFFF);
                case 3 -> Draw.toggle(ctx, bx, by + (BTN - Draw.SWITCH_H) / 2, 1f, false);
                case 4 -> Draw.closeButton(ctx, bx, by, BTN, false);
            }
            String label = Draw.ellipsize(tr(), LABELS[i], x + width - Theme.ROW_PAD - textX);
            ctx.drawText(tr(), label, textX, by + (BTN - 8) / 2 + 1, Theme.TEXT, false);
        }
    }
}
