package net.createbrowser.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Wraps an EditBox with 300 ms (6-tick) debounce.
 * Call {@link #tick()} from the parent screen's tick() every game tick.
 */
public final class SearchBarWidget {

    public final EditBox editBox;

    private final Consumer<String> onSearch;
    private int debounceTimer = 0;

    public SearchBarWidget(Font font, int x, int y, int width, int height, Consumer<String> onSearch) {
        this.onSearch = onSearch;
        this.editBox = new EditBox(font, x, y, width, height,
            Component.translatable("createbrowser.screen.search.placeholder"));
        this.editBox.setHint(Component.translatable("createbrowser.screen.search.placeholder"));
        this.editBox.setMaxLength(100);
        this.editBox.setResponder(text -> debounceTimer = 6);
    }

    /** Must be called each game tick. Fires the search callback after the debounce window. */
    public void tick() {
        editBox.tick();
        if (debounceTimer > 0 && --debounceTimer == 0) {
            String query = editBox.getValue().trim();
            if (!query.isEmpty()) {
                onSearch.accept(query);
            }
        }
    }

    public String getValue() {
        return editBox.getValue().trim();
    }

    /** Triggers search immediately, bypassing the debounce timer. */
    public void triggerSearch() {
        debounceTimer = 0;
        String query = editBox.getValue().trim();
        if (!query.isEmpty()) {
            onSearch.accept(query);
        }
    }
}
