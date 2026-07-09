package net.createbrowser.gui.widget;

import net.createbrowser.api.model.SchematicEntry;
import net.createbrowser.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/** Scrollable list of schematic search results. */
public final class SchematicListWidget extends ObjectSelectionList<SchematicListWidget.Entry> {

    private final Consumer<SchematicEntry> onSelect;

    public SchematicListWidget(Minecraft mc, int width, int height, int top, int bottom, int itemHeight,
                               Consumer<SchematicEntry> onSelect) {
        super(mc, width, height, top, bottom, itemHeight);
        this.onSelect = onSelect;
    }

    public void setEntries(List<SchematicEntry> entries) {
        clearEntries();
        for (SchematicEntry e : entries) {
            addEntry(new Entry(e, onSelect));
        }
    }

    /** Controls visibility — ObjectSelectionList has no built-in visibility flag. */
    public boolean visible = true;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (visible) super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    public static final class Entry extends ObjectSelectionList.Entry<Entry> {

        private final SchematicEntry data;
        private final Consumer<SchematicEntry> onSelect;

        Entry(SchematicEntry data, Consumer<SchematicEntry> onSelect) {
            this.data = data;
            this.onSelect = onSelect;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean isHovering, float partialTick) {
            var font = Minecraft.getInstance().font;

            if (isHovering) {
                graphics.fill(left, top, left + width, top + height, 0x22FFFFFF);
            }

            int textOffset = 5;
            boolean drawThumb = Services.CONFIG.showThumbnails()
                && data.images() != null && !data.images().isEmpty();
            if (drawThumb) {
                int thumbSize = 32;
                int thumbY = top + (height - thumbSize) / 2;
                ThumbnailWidget.draw(graphics, left + 2, thumbY, thumbSize, thumbSize, data.images().get(0));
                textOffset = thumbSize + 8;
            }

            String title = data.title() != null ? data.title() : data.name();
            graphics.drawString(font, title, left + textOffset, top + 3, 0xFFFFFF);

            String byLine = "by " + (data.author() != null ? data.author() : "?");
            graphics.drawString(font, byLine, left + textOffset, top + 15, 0xAAAAAA);

            String meta = String.format("★ %.1f  ↓ %d", data.rating(), data.downloads());
            int metaWidth = font.width(meta);
            graphics.drawString(font, meta, left + width - metaWidth - 5, top + 9, 0xFFCC44);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                onSelect.accept(data);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(data.title() != null ? data.title() : data.name());
        }
    }
}
