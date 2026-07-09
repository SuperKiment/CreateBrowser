package net.createbrowser.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.createbrowser.Constants;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** JSON-backed store for download history. Capped at MAX_ENTRIES; oldest dropped first. */
public final class HistoryStore {

    public record HistoryEntry(String name, String title, String sourceId, long downloadedAt, String filePath) {}

    private static final int MAX_ENTRIES = 500;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<HistoryEntry>>() {}.getType();

    private static volatile HistoryStore instance;

    private final Path file;
    private final List<HistoryEntry> entries = new ArrayList<>();
    private boolean loaded = false;

    public HistoryStore(Path file) {
        this.file = file;
    }

    public static HistoryStore get() {
        HistoryStore local = instance;
        if (local == null) {
            synchronized (HistoryStore.class) {
                local = instance;
                if (local == null) {
                    try {
                        local = new HistoryStore(PathResolver.dataDir().resolve("history.json"));
                    } catch (IOException e) {
                        Constants.LOG.error("[CreateBrowser] Cannot init HistoryStore", e);
                        throw new IllegalStateException(e);
                    }
                    instance = local;
                }
            }
        }
        return local;
    }

    /** Returns entries newest-first. */
    public synchronized List<HistoryEntry> list() {
        ensureLoaded();
        List<HistoryEntry> copy = new ArrayList<>(entries);
        copy.sort(Comparator.comparingLong(HistoryEntry::downloadedAt).reversed());
        return Collections.unmodifiableList(copy);
    }

    public synchronized void recordDownload(HistoryEntry entry) {
        ensureLoaded();
        entries.removeIf(e -> e.name().equals(entry.name()) && e.sourceId().equals(entry.sourceId()));
        entries.add(entry);
        cap();
        save();
    }

    /** Records a view-only entry (no download). Skipped if a download record already exists. */
    public synchronized void recordView(String name, String title, String sourceId) {
        ensureLoaded();
        for (HistoryEntry e : entries) {
            if (e.name().equals(name) && e.sourceId().equals(sourceId) && !e.filePath().isEmpty()) {
                return;
            }
        }
        entries.removeIf(e -> e.name().equals(name) && e.sourceId().equals(sourceId));
        entries.add(new HistoryEntry(name, title, sourceId, System.currentTimeMillis(), ""));
        cap();
        save();
    }

    private void cap() {
        if (entries.size() > MAX_ENTRIES) {
            entries.sort(Comparator.comparingLong(HistoryEntry::downloadedAt));
            while (entries.size() > MAX_ENTRIES) entries.remove(0);
        }
    }

    public synchronized boolean remove(String name) {
        ensureLoaded();
        boolean removed = entries.removeIf(e -> e.name().equals(name));
        if (removed) save();
        return removed;
    }

    public synchronized void clear() {
        ensureLoaded();
        entries.clear();
        save();
    }

    private void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file);
            List<HistoryEntry> read = GSON.fromJson(json, LIST_TYPE);
            if (read != null) entries.addAll(read);
        } catch (Exception e) {
            Constants.LOG.warn("[CreateBrowser] Failed to read history.json — starting empty: {}", e.getMessage());
        }
    }

    private void save() {
        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            Files.writeString(file, GSON.toJson(entries),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            Constants.LOG.error("[CreateBrowser] Failed to save history.json", e);
        }
    }
}
