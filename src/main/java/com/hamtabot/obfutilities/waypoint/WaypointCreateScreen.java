package com.hamtabot.obfutilities.waypoint;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class WaypointCreateScreen extends Screen {

    private static final int[] PRESET_COLORS = {
            0xFF00E5FF, 0xFFFF5252, 0xFF66BB6A, 0xFFFFD54F,
            0xFFCE93D8, 0xFFFF9800, 0xFFFFFFFF, 0xFF90A4AE,
    };
    private static final String[] COLOR_NAMES = {
            "Cyan", "Rouge", "Vert", "Jaune", "Violet", "Orange", "Blanc", "Gris"
    };

    private TextFieldWidget nameField;
    private int selectedColor = 0;
    private final int px, py, pz;
    private final String dimension;
    private final Runnable onCreated;

    private static final int W = 300, H = 208;

    public WaypointCreateScreen(Runnable onCreated) {
        super(Text.literal("Créer un waypoint"));
        PlayerEntity player = MinecraftClient.getInstance().player;
        this.px = player != null ? (int) player.getX() : 0;
        this.py = player != null ? (int) player.getY() : 0;
        this.pz = player != null ? (int) player.getZ() : 0;
        this.dimension = WaypointManager.getCurrentDimension();
        this.onCreated = onCreated;
    }

    @Override
    protected void init() {
        int cx = (this.width - W) / 2;
        int cy = (this.height - H) / 2;

        nameField = new TextFieldWidget(textRenderer, cx + 10, cy + 38, W - 20, 18, Text.literal(""));
        nameField.setPlaceholder(Text.literal("Nom du waypoint..."));
        nameField.setMaxLength(32);
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("§a✔ Créer"),
                btn -> createWaypoint()
        ).dimensions(cx + 10, cy + 148, (W - 25) / 2, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("§c✘ Annuler"),
                btn -> close()
        ).dimensions(cx + W/2 + 5, cy + 148, (W - 25) / 2, 18).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int cx = (this.width - W) / 2;
        int cy = (this.height - H) / 2;

        context.fill(cx, cy, cx+W, cy+H, 0xEE0D0D0D);
        context.fill(cx, cy, cx+W, cy+1, 0xFFFFD54F);
        context.fill(cx, cy+H-1, cx+W, cy+H, 0xFFFFD54F);
        context.fill(cx, cy, cx+1, cy+H, 0xFFFFD54F);
        context.fill(cx+W-1, cy, cx+W, cy+H, 0xFFFFD54F);
        context.fill(cx+1, cy+1, cx+W-1, cy+20, 0xFF1A1A1A);
        context.drawTextWithShadow(textRenderer, "§e◆ Créer un waypoint", cx+8, cy+6, 0xFFFFD54F);

        String posStr = "Position : " + px + ", " + py + ", " + pz + " (" + dimension.replace("minecraft:","") + ")";
        context.drawText(textRenderer, "§7" + posStr, cx+10, cy+28, 0xFFB0BEC5, false);

        context.drawText(textRenderer, "§7Nom :", cx+10, cy+62, 0xFFB0BEC5, false);

        context.drawText(textRenderer, "§7Couleur :", cx+10, cy+80, 0xFFB0BEC5, false);

        int colorSwatchSize = 28;
        int colorStartX = cx + 10;
        int colorY = cy + 92;
        int gap = 4;

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int bx = colorStartX + i * (colorSwatchSize + gap);
            int color = PRESET_COLORS[i];

            context.fill(bx, colorY, bx + colorSwatchSize, colorY + colorSwatchSize, color);

            if (i == selectedColor) {
                context.fill(bx-2, colorY-2, bx+colorSwatchSize+2, colorY-1, 0xFFFFFFFF);
                context.fill(bx-2, colorY+colorSwatchSize+1, bx+colorSwatchSize+2, colorY+colorSwatchSize+2, 0xFFFFFFFF);
                context.fill(bx-2, colorY-2, bx-1, colorY+colorSwatchSize+2, 0xFFFFFFFF);
                context.fill(bx+colorSwatchSize+1, colorY-2, bx+colorSwatchSize+2, colorY+colorSwatchSize+2, 0xFFFFFFFF);
            }

            if (mouseX >= bx && mouseX <= bx+colorSwatchSize && mouseY >= colorY && mouseY <= colorY+colorSwatchSize) {
                context.drawTooltip(textRenderer, Text.literal(COLOR_NAMES[i]), mouseX, mouseY);
            }
        }

        context.drawText(textRenderer, "§7Sélection : " + COLOR_NAMES[selectedColor], cx+10, cy+130, PRESET_COLORS[selectedColor], false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = (this.width - W) / 2;
        int cy = (this.height - H) / 2;
        int colorSwatchSize = 28;
        int colorStartX = cx + 10;
        int colorY = cy + 92;
        int gap = 4;

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int bx = colorStartX + i * (colorSwatchSize + gap);
            if (mouseX >= bx && mouseX <= bx+colorSwatchSize && mouseY >= colorY && mouseY <= colorY+colorSwatchSize) {
                selectedColor = i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void createWaypoint() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Waypoint";
        WaypointManager.add(new Waypoint(name, px, py, pz, dimension, PRESET_COLORS[selectedColor], false));
        if (onCreated != null) onCreated.run();
        close();
    }

    @Override public boolean shouldPause() { return false; }
}