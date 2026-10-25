package com.hamtabot.obfutilities.timer;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import com.hamtabot.obfutilities.util.Format;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.util.List;

public final class Timers {

    public static final int MAX = 10;
    public static final int H = 16;
    private static final int PAD = 6, BAR_GAP = 7;

    private Timers() {}

    public static List<TimerEntry> all() { return OBFUtilities.config.timers; }

    public static void tick(MinecraftClient client) {
        List<TimerEntry> timers = all();
        boolean changed = false;
        for (int i = 0; i < timers.size(); i++) {
            TimerEntry t = timers.get(i);
            if (!t.isCountdown() || !t.running || t.remaining() > 0) continue;
            t.finish();
            changed = true;
            Draw.title("Timer #" + (i + 1) + " terminé", Theme.ACCENT, t.name);
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f));
        }
        if (changed) OBFUtilities.config.save();
    }

    public static int color(TimerEntry t) {
        if (t.finished) return Theme.SUCCESS;
        return t.isCountdown() ? Theme.ACCENT : Theme.PURPLE;
    }

    public static int statusColor(TimerEntry t) { return t.running || t.finished ? color(t) : Theme.TEXT_DIM; }

    public static String status(TimerEntry t) {
        if (t.finished) return "Terminé";
        String clock = Format.clock(t.displayMs());
        return t.running ? clock : clock + " (pause)";
    }

    public static String label(int index, TimerEntry t) { return "#" + (index + 1) + "  " + t.name; }

    private static int baseWidth(TextRenderer tr, String label, String status) {
        return BAR_GAP + tr.getWidth(label) + 8 + tr.getWidth(status) + PAD;
    }

    public static void render(DrawContext ctx, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        TextRenderer tr = client.textRenderer;
        List<TimerEntry> timers = all();
        for (int i = 0; i < timers.size(); i++) {
            TimerEntry t = timers.get(i);
            if (!t.showOnScreen) continue;
            String label = label(i, t), status = status(t);
            int w = baseWidth(tr, label, status);
            int[] b = bounds(t, w);

            ctx.getMatrices().push();
            ctx.getMatrices().translate(b[0], b[1], 0);
            ctx.getMatrices().scale(t.scale, t.scale, 1f);
            Draw.overlayCard(ctx, 0, 0, w, H, color(t), editing);
            int ty = (H - 8) / 2 + 1;
            ctx.drawText(tr, label, BAR_GAP, ty, Theme.TEXT, false);
            ctx.drawText(tr, status, w - PAD - tr.getWidth(status), ty, statusColor(t), false);
            ctx.getMatrices().pop();

            if (editing) Draw.resizeGrip(ctx, b[0] + b[2], b[1] + b[3]);
        }
    }

    public static int[] bounds(int index) {
        TimerEntry t = all().get(index);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        return bounds(t, baseWidth(tr, label(index, t), status(t)));
    }

    private static int[] bounds(TimerEntry t, int baseWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        int w = Math.round(baseWidth * t.scale), h = Math.round(H * t.scale);
        int[] p = t.pos.resolve(w, h, t.x, t.y);
        return new int[]{ p[0], p[1], w, h };
    }

    public static boolean isInResizeGrip(int index, double mx, double my) {
        int[] b = bounds(index);
        return Draw.inResizeGrip(mx, my, b[0] + b[2], b[1] + b[3]);
    }
}
