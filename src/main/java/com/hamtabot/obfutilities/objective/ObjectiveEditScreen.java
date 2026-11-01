package com.hamtabot.obfutilities.objective;

import com.hamtabot.obfutilities.ui.BlockPickerScreen;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import com.hamtabot.obfutilities.util.Format;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ObjectiveEditScreen extends Screen {

    private static final int MAX_W = 280, HEADER_H = 24, PAD = 10, FIELD_H = 18, SEG_H = 16, GAP = 6;
    private static final String[] ACTION_LABELS = new String[ObjectiveEntry.Action.values().length];
    static {
        for (ObjectiveEntry.Action a : ObjectiveEntry.Action.values()) ACTION_LABELS[a.ordinal()] = a.label;
    }
    private static final String[] MODE_LABELS = { "Compteur", "Décompte" };

    private final Screen parent;
    private final ObjectiveEntry editing;
    private TextFieldWidget nameField, targetField;
    private ObjectiveEntry.Action action;
    private String blockId;
    private boolean countdown;

    private int winX, winY, winW, winH;

    public ObjectiveEditScreen(Screen parent, ObjectiveEntry editing) {
        super(Text.literal(editing == null ? "Nouvel objectif" : "Modifier l'objectif"));
        this.parent = parent;
        this.editing = editing;
        this.action = editing != null ? editing.action() : ObjectiveEntry.Action.MINE;
        this.blockId = editing != null ? editing.blockId : "";
        this.countdown = editing != null && editing.countdown;
    }

    private int nameY()    { return HEADER_H + 22; }
    private int actionY()  { return nameY() + FIELD_H + 22; }
    private int blockY()   { return actionY() + SEG_H + 22; }
    private int targetY()  { return blockY() + Theme.BUTTON_H + 22; }
    private int modeY()    { return targetY() + FIELD_H + 22; }
    private int buttonsY() { return modeY() + SEG_H + 14; }

    @Override
    protected void init() {
        winW = Math.min(width - 12, MAX_W);
        winH = buttonsY() + Theme.BUTTON_H + PAD;
        winX = (width - winW) / 2;
        winY = Math.max(4, (height - winH) / 2);

        // the block picker comes back here through init: what was typed stays
        String name = nameField != null ? nameField.getText() : editing != null ? editing.name : "";
        nameField = field(winY + nameY(), 32, name);
        nameField.setPlaceholder(Text.literal("Automatique").styled(s -> s.withColor(Theme.TEXT_DIM & 0xFFFFFF)));

        String target = targetField != null ? targetField.getText() : String.valueOf(editing != null ? editing.target : 1000);
        targetField = field(winY + targetY(), 9, target);
        targetField.setTextPredicate(s -> s.matches("\\d{0,9}"));

        setInitialFocus(nameField);
    }

    private TextFieldWidget field(int y, int maxLength, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, winX + PAD + 6, y + 5, winW - PAD * 2 - 10, 10, Text.empty());
        f.setDrawsBackground(false);
        f.setMaxLength(maxLength);
        f.setText(value);
        addDrawableChild(f);
        return f;
    }

    private long target() {
        String s = targetField.getText();
        return s.isEmpty() ? 0 : Long.parseLong(s);
    }

    private boolean canSave() { return target() > 0; }

    private boolean countsBlocks() { return action != ObjectiveEntry.Action.KILL; }

    private int[] allBlocksButton() {
        int w = textRenderer.getWidth("Tous") + 14;
        return new int[]{ winX + winW - PAD - w, winY + blockY(), w, Theme.BUTTON_H };
    }

    private int[] pickBlockButton() {
        int[] all = allBlocksButton();
        return new int[]{ winX + PAD, winY + blockY(), all[0] - GAP - winX - PAD, Theme.BUTTON_H };
    }

    private int[] button(int index) {
        int bw = (winW - PAD * 2 - GAP) / 2;
        return new int[]{ winX + PAD + index * (bw + GAP), winY + buttonsY(), bw, Theme.BUTTON_H };
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Draw.window(ctx, width, height, winX, winY, winW, winH, HEADER_H);
        Draw.headerTitle(ctx, textRenderer, winX + PAD, winY, HEADER_H, title.getString(), Theme.ACCENT);

        label(ctx, "Nom", nameY());
        Draw.inputFrame(ctx, winX + PAD, winY + nameY(), winW - PAD * 2, FIELD_H, nameField.isFocused());

        label(ctx, "Action", actionY());
        Draw.segments(ctx, textRenderer, winX + PAD, winY + actionY(), winW - PAD * 2, SEG_H, ACTION_LABELS, action.ordinal(), mouseX, mouseY);

        if (countsBlocks()) {
            label(ctx, "Bloc", blockY());
            int[] pick = pickBlockButton(), all = allBlocksButton();
            String shown = Draw.ellipsize(textRenderer, blockId.isEmpty() ? "Tous les blocs" : Format.blockName(blockId), pick[2] - 12);
            Draw.button(ctx, textRenderer, pick[0], pick[1], pick[2], pick[3], shown, Draw.ButtonStyle.SECONDARY,
                    Draw.inside(mouseX, mouseY, pick[0], pick[1], pick[2], pick[3]), true);
            Draw.button(ctx, textRenderer, all[0], all[1], all[2], all[3], "Tous", Draw.ButtonStyle.SECONDARY,
                    Draw.inside(mouseX, mouseY, all[0], all[1], all[2], all[3]), !blockId.isEmpty());
        } else {
            ctx.drawText(textRenderer, Draw.ellipsize(textRenderer, "Compte les mobs tués, stacks compris.", winW - PAD * 2),
                    winX + PAD, winY + blockY() + 5, Theme.TEXT_MUTED, false);
        }

        label(ctx, "Quantité à atteindre", targetY());
        Draw.inputFrame(ctx, winX + PAD, winY + targetY(), winW - PAD * 2, FIELD_H, targetField.isFocused());

        label(ctx, "Barre", modeY());
        String hint = countdown ? "Se vide : il reste x %" : "Se remplit : x % atteints";
        ctx.drawText(textRenderer, hint, winX + winW - PAD - textRenderer.getWidth(hint), winY + modeY() - 11, Theme.TEXT_MUTED, false);
        Draw.segments(ctx, textRenderer, winX + PAD, winY + modeY(), winW - PAD * 2, SEG_H, MODE_LABELS, countdown ? 1 : 0, mouseX, mouseY);

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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int seg = Draw.segmentAt(mouseX, mouseY, winX + PAD, winY + actionY(), winW - PAD * 2, SEG_H, ACTION_LABELS.length);
            if (seg >= 0) {
                action = ObjectiveEntry.Action.values()[seg];
                Draw.playClick();
                return true;
            }
            seg = Draw.segmentAt(mouseX, mouseY, winX + PAD, winY + modeY(), winW - PAD * 2, SEG_H, MODE_LABELS.length);
            if (seg >= 0) {
                countdown = seg == 1;
                Draw.playClick();
                return true;
            }
            if (countsBlocks()) {
                int[] pick = pickBlockButton(), all = allBlocksButton();
                if (Draw.inside(mouseX, mouseY, pick[0], pick[1], pick[2], pick[3])) {
                    Draw.playClick();
                    client.setScreen(new BlockPickerScreen(this, "Choisir un bloc", id -> blockId = id));
                    return true;
                }
                if (Draw.inside(mouseX, mouseY, all[0], all[1], all[2], all[3]) && !blockId.isEmpty()) {
                    blockId = "";
                    Draw.playClick();
                    return true;
                }
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
        var objectives = Objectives.all();
        String name = nameField.getText().trim();
        String block = countsBlocks() ? blockId : "";

        if (editing == null) {
            if (objectives.size() < Objectives.MAX) objectives.add(new ObjectiveEntry(name, action, block, target(), countdown));
        } else {
            // what was counted so far no longer means anything once the goal counts something else
            boolean restart = editing.action() != action || !editing.blockId.equals(block);
            editing.name = name;
            editing.action = action;
            editing.blockId = block;
            editing.target = target();
            editing.countdown = countdown;
            editing.progress = restart ? 0 : Math.min(editing.progress, editing.target);
            Objectives.changed(editing);
        }
        Objectives.save();
        close();
    }

    @Override public void close() { client.setScreen(parent); }

    @Override public boolean shouldPause() { return false; }
}
