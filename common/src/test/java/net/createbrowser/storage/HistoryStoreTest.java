package net.createbrowser.storage;

import net.createbrowser.storage.HistoryStore.HistoryEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryStoreTest {

    private HistoryStore freshStore(Path dir) {
        return new HistoryStore(dir.resolve("history.json"));
    }

    @Test
    void recordsDownloadAndPersists(@TempDir Path dir) {
        HistoryStore store = freshStore(dir);
        store.recordDownload(new HistoryEntry("a", "A", "src", 100L, "/p/a.nbt"));
        HistoryStore reloaded = freshStore(dir);
        assertEquals(1, reloaded.list().size());
    }

    @Test
    void listReturnsNewestFirst(@TempDir Path dir) {
        HistoryStore store = freshStore(dir);
        store.recordDownload(new HistoryEntry("old", "O", "src", 100L, "/p/o.nbt"));
        store.recordDownload(new HistoryEntry("mid", "M", "src", 200L, "/p/m.nbt"));
        store.recordDownload(new HistoryEntry("new", "N", "src", 300L, "/p/n.nbt"));
        List<HistoryEntry> list = store.list();
        assertEquals("new", list.get(0).name());
        assertEquals("mid", list.get(1).name());
        assertEquals("old", list.get(2).name());
    }

    @Test
    void recordingSameNameReplacesEntry(@TempDir Path dir) {
        HistoryStore store = freshStore(dir);
        store.recordDownload(new HistoryEntry("a", "A", "src", 100L, "/p/a.nbt"));
        store.recordDownload(new HistoryEntry("a", "A", "src", 200L, "/p/a.nbt"));
        assertEquals(1, store.list().size());
        assertEquals(200L, store.list().get(0).downloadedAt());
    }

    @Test
    void clearEmptiesStore(@TempDir Path dir) {
        HistoryStore store = freshStore(dir);
        store.recordDownload(new HistoryEntry("a", "A", "src", 100L, "/p"));
        store.clear();
        assertTrue(store.list().isEmpty());
    }

    @Test
    void capsAt500EntriesDroppingOldest(@TempDir Path dir) {
        HistoryStore store = freshStore(dir);
        for (int i = 0; i < 510; i++) {
            store.recordDownload(new HistoryEntry("n" + i, "T", "src", i, "/p"));
        }
        List<HistoryEntry> list = store.list();
        assertEquals(500, list.size());
        assertEquals("n509", list.get(0).name());
        assertEquals("n10", list.get(list.size() - 1).name());
    }
}
