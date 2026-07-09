package net.createbrowser.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/**
 * Composite pagination control: [Prev] "Page n / m" [Next].
 * Add {@link #prevButton} and {@link #nextButton} to the parent screen via addRenderableWidget,
 * then call {@link #renderLabel} from the screen's render() method.
 */
public final class PaginationWidget {

    public final Button prevButton;
    public final Button nextButton;

    private int page = 1;
    private int totalPages = 1;
    private final int labelX;
    private final int labelY;

    public PaginationWidget(int x, int y, int width, IntConsumer onPageChange) {
        this.labelX = x + width / 2;
        this.labelY = y + 4;

        prevButton = Button.builder(
            Component.translatable("createbrowser.pagination.prev"),
            b -> onPageChange.accept(page - 1)
        ).bounds(x, y, 50, 20).build();

        nextButton = Button.builder(
            Component.translatable("createbrowser.pagination.next"),
            b -> onPageChange.accept(page + 1)
        ).bounds(x + width - 50, y, 50, 20).build();
    }

    public void update(int page, int totalPages, boolean hasPrev, boolean hasNext) {
        this.page = page;
        this.totalPages = totalPages;
        prevButton.active = hasPrev;
        nextButton.active = hasNext;
    }

    public void renderLabel(GuiGraphics graphics, Font font) {
        String label = page + " / " + totalPages;
        graphics.drawCenteredString(font, label, labelX, labelY, 0xCCCCCC);
    }
}
