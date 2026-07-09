package net.createbrowser.storage;

import net.createbrowser.storage.FavoritesStore.FavoriteEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FavoritesStoreTest {

    private FavoritesStore freshStore(Path dir) {
        return new FavoritesStore(dir.resolve("favorites.json"));
    }

    @Test
    void emptyByDefault(@TempDir Path dir) {
        assertTrue(freshStore(dir).list().isEmpty());
    }

    @Test
    void addsAndPersists(@TempDir Path dir) {
        FavoritesStore store = freshStore(dir);
        store.add(new FavoriteEntry("windmill-farm", "Windmill", "createmod.com", 1000L));
        assertTrue(store.isFavorite("windmill-farm"));
        assertTrue(Files.exists(dir.resolve("favorites.json")));

        FavoritesStore reloaded = freshStore(dir);
        List<FavoriteEntry> list = reloaded.list();
        assertEquals(1, list.size());
        assertEquals("Windmill", list.get(0).title());
    }

    @Test
    void toggleAddsThenRemoves(@TempDir Path dir) {
        FavoritesStore store = freshStore(dir);
        FavoriteEntry e = new FavoriteEntry("a", "A", "src", 1L);
        assertTrue(store.toggle(e));
        assertTrue(store.isFavorite("a"));
        assertFalse(store.toggle(e));
        assertFalse(store.isFavorite("a"));
    }

    @Test
    void noDuplicateOnAdd(@TempDir Path dir) {
        FavoritesStore store = freshStore(dir);
        store.add(new FavoriteEntry("x", "X1", "src", 1L));
        store.add(new FavoriteEntry("x", "X2", "src", 2L));
        assertEquals(1, store.list().size());
    }

    @Test
    void removeReturnsFalseWhenAbsent(@TempDir Path dir) {
        assertFalse(freshStore(dir).remove("nope"));
    }
}
