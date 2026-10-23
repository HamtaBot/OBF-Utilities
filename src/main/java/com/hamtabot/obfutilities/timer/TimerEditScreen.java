package com.hamtabot.obfutilities.timer;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class TimerEditScreen extends Screen {

    private static final int MAX_W = 280, HEADER_H = 24, PAD = 10, FIELD_H = 18, SEG_H = 16, GAP = 6;
    private static final String[] UNITS = { "h", "min", "s" };
    private static final String[] TYPE_LABELS = { TimerEntry.Type.COUNTDOWN.label, TimerEntry.Type.STOPWATCH.label };

    private final Screen parent;
    private final TimerEntry editing;
    private TextFieldWidget nameField;
    private final TextFieldWidget[] durationFields = new TextFieldWidget[3];
    private TimerEntry.Type type;

    private int winX, winY, winW, winH;

    public TimerEditScreen(Screen parent, TimerEntry editing) {
        super(Text.literal(editing == null ? "Nouveau timer" : "Modifier le timer"));
        this.parent = parent;
        this.editing = editing;
        this.type = editing != null ? editing.type() : TimerEntry.Type.COUNTDOWN;
    }

    private int nameY()     { return HEADER_H + 22; }
    private int typeY()     { return nameY() + FIELD_H + 22; }
    private int durationY() { return typeY() + SEG_H + 22; }
    private int buttonsY()  { return durationY() + FIELD_H + 14; }

    @Override
    protected void init() {
        winW = Math.min(width - 12, MAX_W);
        winH = buttonsY() + Theme.BUTTON_H + PAD;
        winX = (width - winW) / 2;
        winY = Math.max(4, (height - winH) / 2);

        String name = nameField != null ? nameField.getText() : editing != null ? editing.name : "";
        nameField = field(winX + PAD, winY + nameY(), winW - PAD * 2, 24, name);
        nameField.setPlaceholder(Text.literal("Vote, farm...").styled(s -> s.withColor(Theme.TEXT_DIM & 0xFFFFFF)));

        long total = editing != null ? editing.durationMs / 1000 : 30 * 60;
        long[] parts = { total / 3600, (total / 60) % 60, total % 60 };
        for (int i = 0; i < 3; i++) {
            String value = durationFields[i] != null ? durationFields[i].getText() : String.valueOf(parts[i]);
            int[] b = durationBounds(i);
            durationFields[i] = field(b[0], b[1], b[2] - textRenderer.getWidth(UNITS[i]) - 6, 3, value);
            durationFields[i].setTextPredicate(s -> s.matches("\\d{0,3}"));
        }
        updateDurationVisibility();
        setInitialFocus(nameField);
    }

    private TextFieldWidget field(int x, int y, int w, int maxLength, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x + 6, y + 5, w - 10, 10, Text.empty());
        f.setDrawsBackground(false);
        f.setMaxLength(maxLength);
        f.setText(value);
        addDrawableChild(f);
        return f;
    }

    private int[] durationBounds(int i) {
        int w = (winW - PAD * 2 - GAP * 2) / 3;
        return new int[]{ winX + PAD + i * (w + GAP), winY + durationY(), w, FIELD_H };
    }

    private void updateDurationVisibility() {
        for (TextFieldWidget f : durationFields) f.setVisible(type == TimerEntry.Type.COUNTDOWN);
    }

    private long durationMs() {
        long seconds = 0;
        long[] factors = { 3600, 60, 1 };
        for (int i = 0; i < 3; i++) {
            String s = durationFields[i].getText();
            if (!s.isEmpty()) seconds += Long.parseLong(s) * factors[i];
        }
        return seconds * 1000;
    }

    private boolean canSave() { return type == TimerEntry.Type.STOPWATCH || durationMs() > 0; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Draw.window(ctx, width, height, winX, winY, winW, winH, HEADER_H);
        Draw.headerTitle(ctx, textRenderer, winX + PAD, winY, HEADER_H, title.getString(), Theme.ACCENT);

        label(ctx, "Nom", nameY());
        Draw.inputFrame(ctx, winX + PAD, winY + nameY(), winW - PAD * 2, FIELD_H, nameField.isFocused());

        label(ctx, "Type", typeY());
        Draw.segments(ctx, textRenderer, winX + PAD, winY + typeY(), winW - PAD * 2, SEG_H, TYPE_LABELS, type.ordinal(), mouseX, mouseY);

        if (type == TimerEntry.Type.COUNTDOWN) {
            label(ctx, "Durée", durationY());
            for (int i = 0; i < 3; i++) {
                int[] b = durationBounds(i);
                Draw.inputFrame(ctx, b[0], b[1], b[2], FIELD_H, durationFields[i].isFocused());
                ctx.drawText(textRenderer, UNITS[i], b[0] + b[2] - 5 - textRenderer.getWidth(UNITS[i]), b[1] + 5, Theme.TEXT_DIM, false);
            }
        } else {
            ctx.drawText(textRenderer, Draw.ellipsize(textRenderer, "Compte le temps écoulé depuis le démarrage.", winW - PAD * 2),
                    winX + PAD, winY + durationY() + 5, Theme.TEXT_MUTED, false);
        }

        int[] cancel = button(0), save = button(1);
        Draw.button(ctx, textRenderer, cancel[0], cancel[1], cancel[2], cancel[3], "Annuler", Draw.ButtonStyle.SECONDARY,
                Draw.inside(mouseX, mouseY, cancel[0], cancel[1], cancel[2], cancel[3]), true);
        Draw.button(ctx, textRenderer, save[0], save[1], save[2], save[3], editing == null ? "Créer" : "Enregistrer",
                Draw.ButtonStyle.PRIMARY, Draw.inside(mouseX, mouseY, save[0], save[1], save[2], save[3]), canSave());

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void label(DrawContext ctx, String text, int relY) {
        ctx.drawText(textRenderer, text, winX + PAD, winY + relY - 11, Theme.TEXT, false);
    }

    private int[] button(int index) {
        int bw = (winW - PAD * 2 - GAP) / 2;
        return new int[]{ winX + PAD + index * (bw + GAP), winY + buttonsY(), bw, Theme.BUTTON_H };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int seg = Draw.segmentAt(mouseX, mouseY, winX + PAD, winY + typeY(), winW - PAD * 2, SEG_H, TYPE_LABELS.length);
            if (seg >= 0) {
                type = TimerEntry.Type.values()[seg];
                updateDurationVisibility();
                Draw.playClick();
                return true;
            }
            int[] cancel = button(0), save = button(1);
            if (Draw.inside(mouseX, mouseY, cancel[0], cancel[1], cancel[2], cancel[3])) { close(); return true; }
            if (Draw.inside(mouseX, mouseY, save[0], save[1], save[2], save[3])) { save(); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { save(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void save() {
        if (!canSave()) return;
        var timers = OBFUtilities.config.timers;
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Timer " + (editing != null ? timers.indexOf(editing) + 1 : timers.size() + 1);
        long duration = type == TimerEntry.Type.COUNTDOWN ? durationMs() : 0;

        if (editing == null) {
            if (timers.size() < Timers.MAX) timers.add(new TimerEntry(name, type, duration));
        } else {
            editing.name = name;
            editing.setType(type);
            editing.setDuration(duration);
        }
        OBFUtilities.config.save();
        close();
    }

    @Override public void close() { client.setScreen(parent); }

    @Override public boolean shouldPause() { return false; }
}
