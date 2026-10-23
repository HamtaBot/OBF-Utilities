package com.hamtabot.obfutilities.waypoint;

import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class WaypointCreateScreen extends Screen {

    private static final int[] PRESET_COLORS = {
            0xFF00E5FF, 0xFFFF5252, 0xFF66BB6A, 0xFFFFD54F,
            0xFFCE93D8, 0xFFFF9800, 0xFFFFFFFF, 0xFF90A4AE,
    };
    private static final String[] COLOR_NAMES = {
            "Cyan", "Rouge", "Vert", "Jaune", "Violet", "Orange", "Blanc", "Gris"
    };

    private static final int MAX_W = 280, HEADER_H = 24, PAD = 10, GAP = 4;

    private TextFieldWidget nameField;
    private int selectedColor = 0;
    private final int px, py, pz;
    private final String dimension;
    private final Screen parent;

    private int winX, winY, winW, winH, swatch;

    public WaypointCreateScreen(Screen parent) {
        super(Text.literal("Nouveau waypoint"));
        PlayerEntity player = MinecraftClient.getInstance().player;
        this.px = player != null ? (int) Math.floor(player.getX()) : 0;
        this.py = player != null ? (int) Math.floor(player.getY()) : 0;
        this.pz = player != null ? (int) Math.floor(player.getZ()) : 0;
        this.dimension = WaypointManager.getCurrentDimension();
        this.parent    = parent;
    }

    private int fieldY()   { return HEADER_H + 34; }
    private int colorsY()  { return fieldY() + 18 + 24; }
    private int buttonsY() { return colorsY() + swatch + 14; }

    @Override
    protected void init() {
        winW = Math.min(width - 12, MAX_W);
        swatch = Math.min(24, (winW - PAD * 2 - GAP * (PRESET_COLORS.length - 1)) / PRESET_COLORS.length);
        winH = buttonsY() + Theme.BUTTON_H + PAD;
        winX = (width - winW) / 2;
        winY = Math.max(4, (height - winH) / 2);

        String previous = nameField != null ? nameField.getText() : "";
        nameField = new TextFieldWidget(textRenderer, winX + PAD + 6, winY + fieldY() + 5, winW - PAD * 2 - 10, 10, Text.literal("Nom"));
        nameField.setDrawsBackground(false);
        nameField.setMaxLength(32);
        nameField.setPlaceholder(Text.literal("Waypoint").styled(s -> s.withColor(Theme.TEXT_DIM & 0xFFFFFF)));
        nameField.setText(previous);
        addDrawableChild(nameField);
        setInitialFocus(nameField);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Draw.window(ctx, width, height, winX, winY, winW, winH, HEADER_H);
        Draw.headerTitle(ctx, textRenderer, winX + PAD, winY, HEADER_H, "Nouveau waypoint", PRESET_COLORS[selectedColor]);

        String pos = px + ", " + py + ", " + pz + "  ·  " + dimension.replace("minecraft:", "");
        ctx.drawText(textRenderer, Draw.ellipsize(textRenderer, pos, winW - PAD * 2), winX + PAD, winY + HEADER_H + 9, Theme.TEXT_MUTED, false);

        ctx.drawText(textRenderer, "Nom", winX + PAD, winY + fieldY() - 11, Theme.TEXT, false);
        Draw.inputFrame(ctx, winX + PAD, winY + fieldY(), winW - PAD * 2, 18, nameField.isFocused());

        ctx.drawText(textRenderer, "Couleur", winX + PAD, winY + colorsY() - 11, Theme.TEXT, false);
        String chosen = COLOR_NAMES[selectedColor];
        ctx.drawText(textRenderer, chosen, winX + winW - PAD - textRenderer.getWidth(chosen), winY + colorsY() - 11, PRESET_COLORS[selectedColor], false);

        String hoveredName = null;
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int sx = swatchX(i), sy = winY + colorsY();
            boolean hov = Draw.inside(mouseX, mouseY, sx, sy, swatch, swatch);
            if (i == selectedColor) Draw.round(ctx, sx - 2, sy - 2, swatch + 4, swatch + 4, 0xFFFFFFFF);
            else if (hov) Draw.round(ctx, sx - 1, sy - 1, swatch + 2, swatch + 2, 0x88FFFFFF);
            Draw.round(ctx, sx, sy, swatch, swatch, PRESET_COLORS[i]);
            if (hov) hoveredName = COLOR_NAMES[i];
        }

        int[] cancel = button(0), create = button(1);
        Draw.button(ctx, textRenderer, cancel[0], cancel[1], cancel[2], cancel[3], "Annuler", Draw.ButtonStyle.SECONDARY,
                Draw.inside(mouseX, mouseY, cancel[0], cancel[1], cancel[2], cancel[3]), true);
        Draw.button(ctx, textRenderer, create[0], create[1], create[2], create[3], "Créer", Draw.ButtonStyle.PRIMARY,
                Draw.inside(mouseX, mouseY, create[0], create[1], create[2], create[3]), true);

        super.render(ctx, mouseX, mouseY, delta);
        if (hoveredName != null) ctx.drawTooltip(textRenderer, Text.literal(hoveredName), mouseX, mouseY);
    }

    private int swatchX(int i) { return winX + PAD + i * (swatch + GAP); }

    private int[] button(int index) {
        int bw = (winW - PAD * 2 - 6) / 2;
        return new int[]{ winX + PAD + index * (bw + 6), winY + buttonsY(), bw, Theme.BUTTON_H };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < PRESET_COLORS.length; i++) {
                if (Draw.inside(mouseX, mouseY, swatchX(i), winY + colorsY(), swatch, swatch)) {
                    selectedColor = i;
                    return true;
                }
            }
            int[] cancel = button(0), create = button(1);
            if (Draw.inside(mouseX, mouseY, cancel[0], cancel[1], cancel[2], cancel[3])) { close(); return true; }
            if (Draw.inside(mouseX, mouseY, create[0], create[1], create[2], create[3])) { createWaypoint(); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { createWaypoint(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void createWaypoint() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Waypoint";
        WaypointManager.add(new Waypoint(name, px, py, pz, dimension, PRESET_COLORS[selectedColor]));
        close();
    }

    @Override public void close() { client.setScreen(parent); }

    @Override public boolean shouldPause() { return false; }
}
