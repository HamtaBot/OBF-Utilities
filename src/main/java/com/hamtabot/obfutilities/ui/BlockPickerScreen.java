package com.hamtabot.obfutilities.ui;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.ui.component.Component;
import com.hamtabot.obfutilities.util.Format;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlockPickerScreen extends Screen {

    private static final int MAX_W = 320, MAX_H = 300, HEADER_H = 24, SEG_H = 16, FOOTER_H = 16;

    private static final String[] SOURCE_LABELS = { "Blocs posés", "Blocs minés" };
    private static final Set<String> AIR = Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air");

    private final Screen parent;
    private final Consumer<List<String>> onConfirm;
    private final Set<String> alreadyAdded;
    private final Predicate<String> filter;
    private final Set<String> selected = new LinkedHashSet<>();
    private final ScrollList list = new ScrollList();
    private TextFieldWidget search;
    private boolean trackPlaced = true;
    private List<Option> allOptions = List.of();

    private int winX, winY, winW, winH;

    public BlockPickerScreen(Screen parent) { this(parent, "Ajouter un bloc", Set.of(), id -> true, null); }

    public BlockPickerScreen(Screen parent, String title, Set<String> alreadyAdded, Predicate<String> filter,
                             Consumer<List<String>> onConfirm) {
        super(Text.literal(title));
        this.parent = parent;
        this.alreadyAdded = alreadyAdded;
        this.filter = filter;
        this.onConfirm = onConfirm;
        if (onConfirm != null) trackPlaced = false;
    }

    private boolean multi() { return onConfirm != null; }

    @Override
    protected void init() {
        winW = Math.min(width - 12, MAX_W);
        winH = Math.min(height - 12, MAX_H);
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;

        String previous = search != null ? search.getText() : "";
        int fx = winX + 10, fy = fieldY();
        search = new TextFieldWidget(textRenderer, fx + 6, fy + 5, winW - 20 - 10, 10, Text.literal("Recherche"));
        search.setDrawsBackground(false);
        search.setMaxLength(64);
        search.setPlaceholder(Text.literal("Rechercher un bloc...").styled(s -> s.withColor(Theme.TEXT_DIM & 0xFFFFFF)));
        search.setText(previous);
        search.setChangedListener(s -> rebuildList(false));
        addDrawableChild(search);
        setInitialFocus(search);

        int listY = fy + 18 + 6;
        int listBottom = multi() ? footerY() - 6 : winY + winH - 8;
        list.setBounds(winX + 10, listY, winW - 20, listBottom - listY);
        if (allOptions.isEmpty()) reloadIds(); else rebuildList(true);
    }

    private int segY() { return winY + HEADER_H + 8; }

    private int fieldY() { return multi() ? winY + HEADER_H + 8 : segY() + SEG_H + 8; }

    private int footerY() { return winY + winH - 8 - FOOTER_H; }

    private void reloadIds() {
        allOptions = (trackPlaced ? Registries.ITEM.getIds() : Registries.BLOCK.getIds()).stream()
                .map(Identifier::toString)
                .filter(id -> !AIR.contains(id) && filter.test(id))
                .sorted()
                .map(Option::new)
                .toList();
        rebuildList(false);
    }

    private void rebuildList(boolean keepScroll) {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        List<Component> rows = new ArrayList<>();
        for (Option o : allOptions) {
            if (q.isEmpty() || o.id.contains(q) || o.search.contains(q)) rows.add(o);
        }
        list.setComponents(rows, keepScroll);
    }

    private void pick(String id) {
        if (multi()) {
            if (alreadyAdded.contains(id)) return;
            if (!selected.remove(id)) selected.add(id);
            Draw.playClick();
            return;
        }
        OBFUtilities.config.customBlocks.add(new ModConfig.CustomBlockEntry(id, trackPlaced));
        OBFUtilities.config.save();
        close();
    }

    private void confirm() {
        if (selected.isEmpty()) return;
        onConfirm.accept(new ArrayList<>(selected));
        close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Draw.window(ctx, width, height, winX, winY, winW, winH, HEADER_H);
        Draw.headerTitle(ctx, textRenderer, winX + 10, winY, HEADER_H, title.getString(), Theme.ACCENT);
        int[] c = closeBounds();
        Draw.closeButton(ctx, c[0], c[1], Theme.CLOSE_SIZE, Draw.inside(mouseX, mouseY, c[0], c[1], Theme.CLOSE_SIZE, Theme.CLOSE_SIZE));

        if (!multi()) Draw.segments(ctx, textRenderer, winX + 10, segY(), winW - 20, SEG_H, SOURCE_LABELS, trackPlaced ? 0 : 1, mouseX, mouseY);

        int fx = winX + 10, fy = fieldY();
        Draw.inputFrame(ctx, fx, fy, winW - 20, 18, search.isFocused());

        list.render(ctx, mouseX, mouseY);
        if (list.isEmpty()) {
            String none = "Aucun résultat";
            ctx.drawText(textRenderer, none, winX + (winW - textRenderer.getWidth(none)) / 2, fy + 40, Theme.TEXT_DIM, false);
        }

        if (multi()) renderFooter(ctx, mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderFooter(DrawContext ctx, int mouseX, int mouseY) {
        int fy = footerY();
        ctx.fill(winX + 1, fy - 4, winX + winW - 1, fy - 3, Theme.DIVIDER);

        int n = selected.size();
        String count = n == 0 ? "Coche un ou plusieurs blocs" : n + (n > 1 ? " blocs sélectionnés" : " bloc sélectionné");
        int[] add = addBounds(), clear = clearBounds();
        ctx.drawText(textRenderer, Draw.ellipsize(textRenderer, count, clear[0] - winX - 18), winX + 10, fy + 4,
                n == 0 ? Theme.TEXT_DIM : Theme.ACCENT, false);

        Draw.button(ctx, textRenderer, clear[0], fy, clear[2], FOOTER_H, "Vider", Draw.ButtonStyle.GHOST,
                Draw.inside(mouseX, mouseY, clear[0], fy, clear[2], FOOTER_H), n > 0);
        Draw.button(ctx, textRenderer, add[0], fy, add[2], FOOTER_H, addLabel(), Draw.ButtonStyle.PRIMARY,
                Draw.inside(mouseX, mouseY, add[0], fy, add[2], FOOTER_H), n > 0);
    }

    private String addLabel() { return selected.isEmpty() ? "Ajouter" : "Ajouter (" + selected.size() + ")"; }

    private int[] addBounds() {
        int w = textRenderer.getWidth("Ajouter (999)") + 14;
        return new int[]{ winX + winW - 10 - w, footerY(), w };
    }

    private int[] clearBounds() {
        int w = textRenderer.getWidth("Vider") + 14;
        return new int[]{ addBounds()[0] - 6 - w, footerY(), w };
    }

    private int[] closeBounds() { return new int[]{ winX + winW - Theme.CLOSE_SIZE - 5, winY + (HEADER_H - Theme.CLOSE_SIZE) / 2 }; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int[] c = closeBounds();
            if (Draw.inside(mouseX, mouseY, c[0], c[1], Theme.CLOSE_SIZE, Theme.CLOSE_SIZE)) { close(); return true; }
            if (multi()) {
                int[] add = addBounds(), clear = clearBounds();
                if (Draw.inside(mouseX, mouseY, add[0], add[1], add[2], FOOTER_H)) { confirm(); return true; }
                if (Draw.inside(mouseX, mouseY, clear[0], clear[1], clear[2], FOOTER_H)) {
                    if (!selected.isEmpty()) { selected.clear(); Draw.playClick(); }
                    return true;
                }
            } else {
                int seg = Draw.segmentAt(mouseX, mouseY, winX + 10, segY(), winW - 20, SEG_H, SOURCE_LABELS.length);
                if (seg >= 0) {
                    if ((seg == 0) != trackPlaced) { trackPlaced = seg == 0; reloadIds(); }
                    return true;
                }
            }
        }
        if (list.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (multi() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && !selected.isEmpty()) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (list.mouseDragged(mouseX, mouseY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        list.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (list.mouseScrolled(mouseX, mouseY, amount)) return true;
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override public void close() { client.setScreen(parent); }

    @Override public boolean shouldPause() { return false; }

    private class Option extends Component {

        private static final int BOX = 10;

        private final String id, name, search;
        private ItemStack icon;

        Option(String id) {
            this.id = id;
            this.name = Format.blockName(id);
            this.search = name.toLowerCase(Locale.ROOT);
        }

        @Override public int getHeight() { return 20; }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY) {
            if (icon == null) icon = Format.icon(id, trackPlaced);
            boolean added = multi() && alreadyAdded.contains(id);
            boolean checked = multi() && selected.contains(id);
            if (checked) Draw.round(ctx, x, y, width, 20, Theme.ACCENT_SURFACE);
            else if (!added && isHovered(mouseX, mouseY)) Draw.round(ctx, x, y, width, 20, Theme.SURFACE_HOVER);

            ctx.drawItem(icon, x + 3, y + 2);
            int right = x + width - 4;
            if (multi()) {
                right -= BOX + 6;
                renderCheck(ctx, right + 6, y + 5, checked, added);
            }
            ctx.drawText(tr(), Draw.ellipsize(tr(), name, right - x - 26), x + 24, y + 6, added ? Theme.TEXT_DIM : Theme.TEXT, false);

            int nameEnd = x + 24 + tr().getWidth(name) + 6;
            String extra = added ? "déjà ajouté" : id;
            String shown = Draw.ellipsize(tr(), extra, right - nameEnd);
            if (!shown.isEmpty() && nameEnd < right - 30) {
                ctx.drawText(tr(), shown, right - tr().getWidth(shown), y + 6, Theme.TEXT_DIM, false);
            }
        }

        private void renderCheck(DrawContext ctx, int bx, int by, boolean checked, boolean added) {
            if (added) { Draw.card(ctx, bx, by, BOX, BOX, Theme.SURFACE, Theme.DIVIDER); return; }
            Draw.card(ctx, bx, by, BOX, BOX, checked ? Theme.ACCENT_DARK : Theme.INPUT, checked ? Theme.ACCENT : Theme.BORDER);
            if (checked) ctx.drawText(tr(), "✔", bx + 2, by + 1, 0xFFFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !isHovered(mouseX, mouseY)) return false;
            pick(id);
            return true;
        }
    }
}
