package com.hamtabot.obfutilities.ui.component;

import com.hamtabot.obfutilities.ui.Anim;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import com.hamtabot.obfutilities.waypoint.Waypoint;
import com.hamtabot.obfutilities.waypoint.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class WaypointRow extends Component {

    private static final int H = 30, SHARE_W = 50, DEL_W = 16, CONFIRM_W = 58;

    private final Waypoint wp;
    private final Runnable onDelete;
    private final Anim knob;
    private boolean confirmDelete = false;
    private long copiedUntil = 0;

    public WaypointRow(Waypoint wp, Runnable onDelete) {
        this.wp = wp;
        this.onDelete = onDelete;
        this.knob = new Anim(wp.enabled ? 1f : 0f);
    }

    @Override public int getHeight() { return H; }

    private int delW()    { return confirmDelete ? CONFIRM_W : DEL_W; }
    private int delX()    { return x + width - Theme.ROW_PAD - delW(); }
    private int switchX() { return delX() - 8 - Draw.SWITCH_W; }
    private int shareX()  { return switchX() - 8 - SHARE_W; }
    private int ctrlY(int h) { return y + (H - h) / 2; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean hov = isHovered(mouseX, mouseY);
        Draw.card(ctx, x, y, width, H, hov ? Theme.SURFACE_HOVER : Theme.SURFACE, Theme.BORDER);
        ctx.fill(x + 1, y + 2, x + 3, y + H - 2, wp.color | 0xFF000000);

        int textW = shareX() - (x + 10) - 4;
        ctx.drawText(tr(), Draw.ellipsize(tr(), wp.name, textW), x + 10, y + 6, wp.enabled ? Theme.TEXT : Theme.TEXT_DIM, false);
        ctx.drawText(tr(), Draw.ellipsize(tr(), subtitle(), textW), x + 10, y + 17, Theme.TEXT_DIM, false);

        boolean copied = System.currentTimeMillis() < copiedUntil;
        Draw.button(ctx, tr(), shareX(), ctrlY(Theme.BUTTON_H), SHARE_W, Theme.BUTTON_H,
                copied ? "Copié !" : "Partager", copied ? Draw.ButtonStyle.PRIMARY : Draw.ButtonStyle.SECONDARY,
                Draw.inside(mouseX, mouseY, shareX(), ctrlY(Theme.BUTTON_H), SHARE_W, Theme.BUTTON_H), true);

        Draw.toggle(ctx, switchX(), ctrlY(Draw.SWITCH_H), knob.update(wp.enabled ? 1f : 0f, 16f),
                Draw.inside(mouseX, mouseY, switchX(), ctrlY(Draw.SWITCH_H), Draw.SWITCH_W, Draw.SWITCH_H));

        int dh = confirmDelete ? Theme.BUTTON_H : DEL_W;
        boolean delHov = Draw.inside(mouseX, mouseY, delX(), ctrlY(dh), delW(), dh);
        if (confirmDelete) Draw.button(ctx, tr(), delX(), ctrlY(dh), delW(), dh, "Confirmer", Draw.ButtonStyle.DANGER, delHov, true);
        else Draw.closeButton(ctx, delX(), ctrlY(dh), DEL_W, delHov);
    }

    private String subtitle() {
        String s = wp.x + ", " + wp.y + ", " + wp.z + "  ·  " + wp.getDimShort();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && WaypointManager.getCurrentDimension().equals(wp.dimension)) {
            double dx = wp.x + 0.5 - client.player.getX(), dz = wp.z + 0.5 - client.player.getZ();
            s += "  ·  " + (int) Math.sqrt(dx * dx + dz * dz) + " m";
        }
        return s;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        boolean wasConfirming = confirmDelete;
        if (!isHovered(mouseX, mouseY)) { confirmDelete = false; return false; }

        int dh = confirmDelete ? Theme.BUTTON_H : DEL_W;
        if (Draw.inside(mouseX, mouseY, delX(), ctrlY(dh), delW(), dh)) {
            Draw.playClick();
            if (wasConfirming) onDelete.run(); else confirmDelete = true;
            return true;
        }
        confirmDelete = false;
        if (Draw.inside(mouseX, mouseY, shareX(), ctrlY(Theme.BUTTON_H), SHARE_W, Theme.BUTTON_H)) {
            MinecraftClient.getInstance().keyboard.setClipboard("[OBF-WP:" + wp.toShareString() + "]");
            copiedUntil = System.currentTimeMillis() + 1500;
            Draw.playClick();
            return true;
        }
        if (Draw.inside(mouseX, mouseY, switchX() - 4, y, Draw.SWITCH_W + 8, H)) {
            wp.enabled = !wp.enabled;
            WaypointManager.save();
            Draw.playClick();
            return true;
        }
        return false;
    }
}
