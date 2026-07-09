package net.createbrowser.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Click-to-open dropdown widget. Selecting an option calls onChange. */
public final class FilterDropdown<T> {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<T> options;
    private final Function<T, Component> labelOf;
    private final Consumer<T> onChange;

    private T selected;
    private boolean open = false;

    public FilterDropdown(int x, int y, int width, int height,
                          List<T> options, T initial,
                          Function<T, Component> labelOf, Consumer<T> onChange) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.options = options;
        this.selected = initial;
        this.labelOf = labelOf;
        this.onChange = onChange;
    }

    public T selected() { return selected; }
    public void setSelected(T value) { this.selected = value; }
    public boolean isOpen() { return open; }
    public void close() { this.open = false; }

    /** Render the closed control. Call renderOverlay() AFTER drawing the rest of the screen. */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        boolean hovered = inBounds(mouseX, mouseY, x, y, width, height);
        int bg = hovered ? 0xFF555560 : 0xFF333340;
        graphics.fill(x, y, x + width, y + height, bg);
        graphics.fill(x, y, x + width, y + 1, 0xFF666670);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF111118);

        String label = labelOf.apply(selected).getString();
        graphics.drawString(font, label, x + 4, y + (height - 8) / 2, 0xFFFFFF);
        graphics.drawString(font, open ? "^" : "v", x + width - 10, y + (height - 8) / 2, 0xCCCCCC);
    }

    /** Render the open dropdown list. Must be called after the rest of the screen so it sits on top. */
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!open) return;
        var font = Minecraft.getInstance().font;
        int rowH = height;
        int listY = y + height;
        int listHeight = options.size() * rowH;
        graphics.fill(x, listY, x + width, listY + listHeight, 0xFF222228);
        for (int i = 0; i < options.size(); i++) {
            T opt = options.get(i);
            int rowY = listY + i * rowH;
            boolean rowHover = inBounds(mouseX, mouseY, x, rowY, width, rowH);
            if (rowHover) graphics.fill(x, rowY, x + width, rowY + rowH, 0xFF444450);
            graphics.drawString(font, labelOf.apply(opt).getString(),
                x + 4, rowY + (rowH - 8) / 2,
                opt.equals(selected) ? 0xFFCC44 : 0xFFFFFF);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (open) {
            int rowH = height;
            int listY = y + height;
            int listHeight = options.size() * rowH;
            if (inBounds(mouseX, mouseY, x, listY, width, listHeight)) {
                int idx = (int) ((mouseY - listY) / rowH);
                if (idx >= 0 && idx < options.size()) {
                    T newSel = options.get(idx);
                    open = false;
                    if (!newSel.equals(selected)) {
                        selected = newSel;
                        onChange.accept(selected);
                    }
                    return true;
                }
            }
            open = false;
            return inBounds(mouseX, mouseY, x, y, width, height);
        }
        if (inBounds(mouseX, mouseY, x, y, width, height)) {
            open = true;
            return true;
        }
        return false;
    }

    private static boolean inBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
