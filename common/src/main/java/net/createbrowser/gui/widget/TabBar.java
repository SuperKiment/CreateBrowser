package net.createbrowser.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/** Horizontal tab bar widget. Renders a row of selectable tab buttons. */
public final class TabBar<T> {

    public record Tab<T>(T value, Component label) {}

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<Tab<T>> tabs;
    private final Consumer<T> onSelect;
    private T active;

    public TabBar(int x, int y, int width, int height, List<Tab<T>> tabs, T initial, Consumer<T> onSelect) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.tabs = tabs;
        this.active = initial;
        this.onSelect = onSelect;
    }

    public void setActive(T value) { this.active = value; }
    public T active() { return active; }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int tabWidth = tabs.isEmpty() ? width : width / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            Tab<T> t = tabs.get(i);
            int tx = x + i * tabWidth;
            boolean hovered = mouseX >= tx && mouseX < tx + tabWidth && mouseY >= y && mouseY < y + height;
            boolean selected = t.value().equals(active);

            int bg = selected ? 0xFF505060 : (hovered ? 0xFF383844 : 0xFF222230);
            graphics.fill(tx, y, tx + tabWidth, y + height, bg);
            graphics.fill(tx, y + height - 1, tx + tabWidth, y + height,
                selected ? 0xFFFFCC44 : 0xFF111118);

            String label = t.label().getString();
            int labelWidth = font.width(label);
            int color = selected ? 0xFFFFFF : (hovered ? 0xDDDDDD : 0xAAAAAA);
            graphics.drawString(font, label,
                tx + (tabWidth - labelWidth) / 2,
                y + (height - 8) / 2,
                color);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (mouseY < y || mouseY >= y + height || mouseX < x || mouseX >= x + width) return false;
        int tabWidth = tabs.isEmpty() ? width : width / tabs.size();
        int idx = (int) ((mouseX - x) / tabWidth);
        if (idx < 0 || idx >= tabs.size()) return false;
        Tab<T> clicked = tabs.get(idx);
        if (!clicked.value().equals(active)) {
            active = clicked.value();
            onSelect.accept(active);
        }
        return true;
    }
}
