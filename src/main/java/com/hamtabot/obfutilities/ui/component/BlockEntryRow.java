package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Anim;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import com.hamtabot.obfutilities.util.Format;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public abstract class BlockEntryRow extends Component {

    protected static final int H = 30, DEL_W = 16;

    private final String blockId;
    private final ItemStack icon;
    private final Runnable onDelete;
    private final Anim knob;

    protected BlockEntryRow(String blockId, ItemStack icon, boolean enabled, Runnable onDelete) {
        this.blockId = blockId;
        this.icon = icon;
        this.onDelete = onDelete;
        this.knob = new Anim(enabled ? 1f : 0f);
    }

    protected abstract boolean isEnabled();

    protected abstract void toggle();

    protected abstract String subtitle();

    protected int extraWidth() { return 0; }

    protected void renderExtra(DrawContext ctx, int ex, int mouseX, int mouseY) {}

    protected boolean clickExtra(double mouseX, double mouseY, int ex) { return false; }

    @Override public int getHeight() { return H; }

    private int delX()    { return x + width - Theme.ROW_PAD - DEL_W; }
    private int switchX() { return delX() - 8 - Draw.SWITCH_W; }
    private int extraX()  { return switchX() - 8 - extraWidth(); }
    protected int ctrlY(int h) { return y + (H - h) / 2; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean hov = isHovered(mouseX, mouseY);
        Draw.card(ctx, x, y, width, H, hov ? Theme.SURFACE_HOVER : Theme.SURFACE, Theme.BORDER);

        Draw.round(ctx, x + 5, y + 5, 20, 20, Theme.INPUT);
        ctx.drawItem(icon, x + 7, y + 7);

        int textW = (extraWidth() > 0 ? extraX() : switchX()) - (x + 31) - 4;
        ctx.drawText(tr(), Draw.ellipsize(tr(), Format.blockName(blockId), textW), x + 31, y + 6,
                isEnabled() ? Theme.TEXT : Theme.TEXT_DIM, false);
        ctx.drawText(tr(), Draw.ellipsize(tr(), subtitle(), textW), x + 31, y + 17, Theme.TEXT_DIM, false);

        if (extraWidth() > 0) renderExtra(ctx, extraX(), mouseX, mouseY);

        Draw.toggle(ctx, switchX(), ctrlY(Draw.SWITCH_H), knob.update(isEnabled() ? 1f : 0f, 16f),
                Draw.inside(mouseX, mouseY, switchX(), ctrlY(Draw.SWITCH_H), Draw.SWITCH_W, Draw.SWITCH_H));

        Draw.closeButton(ctx, delX(), ctrlY(DEL_W), DEL_W, Draw.inside(mouseX, mouseY, delX(), ctrlY(DEL_W), DEL_W, DEL_W));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;
        if (Draw.inside(mouseX, mouseY, delX(), ctrlY(DEL_W), DEL_W, DEL_W)) {
            Draw.playClick();
            onDelete.run();
            return true;
        }
        if (extraWidth() > 0 && clickExtra(mouseX, mouseY, extraX())) return true;
        if (Draw.inside(mouseX, mouseY, switchX() - 4, y, Draw.SWITCH_W + 8, H)) {
            toggle();
            Draw.playClick();
            return true;
        }
        return false;
    }
}
