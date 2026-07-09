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
import java.util.List;

/** JSON-backed store for favorite schematics. Thread-safe for read/write of the favorites list. */
public final class FavoritesStore {

    public record FavoriteEntry(String name, String title, String sourceId, long addedAt) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<FavoriteEntry>>() {}.getType();

    private static volatile FavoritesStore instance;

    private final Path file;
    private final List<FavoriteEntry> entries = new ArrayList<>();
    private boolean loaded = false;

    public FavoritesStore(Path file) {
        this.file = file;
    }

    public static FavoritesStore get() {
        FavoritesStore local = instance;
        if (local == null) {
            synchronized (FavoritesStore.class) {
                local = instance;
                if (local == null) {
                    try {
                        local = new FavoritesStore(PathResolver.dataDir().resolve("favorites.json"));
                    } catch (IOException e) {
                        Constants.LOG.error("[CreateBrowser] Cannot init FavoritesStore", e);
                        throw new IllegalStateException(e);
                    }
                    instance = local;
                }
            }
        }
        return local;
    }

    public synchronized List<FavoriteEntry> list() {
        ensureLoaded();
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public synchronized boolean isFavorite(String name) {
        ensureLoaded();
        return entries.stream().anyMatch(e -> e.name().equals(name));
    }

    public synchronized void add(FavoriteEntry entry) {
        ensureLoaded();
        if (entries.stream().noneMatch(e -> e.name().equals(entry.name()))) {
            entries.add(entry);
            save();
        }
    }

    public synchronized boolean remove(String name) {
        ensureLoaded();
        boolean removed = entries.removeIf(e -> e.name().equals(name));
        if (removed) save();
        return removed;
    }

    /** @return true if the entry is now favorited, false if it was removed. */
    public synchronized boolean toggle(FavoriteEntry entry) {
        ensureLoaded();
        if (entries.removeIf(e -> e.name().equals(entry.name()))) {
            save();
            return false;
        }
        entries.add(entry);
        save();
        return true;
    }

    private void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file);
            List<FavoriteEntry> read = GSON.fromJson(json, LIST_TYPE);
            if (read != null) entries.addAll(read);
        } catch (Exception e) {
            Constants.LOG.warn("[CreateBrowser] Failed to read favorites.json — starting empty: {}", e.getMessage());
        }
    }

    private void save() {
        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            Files.writeString(file, GSON.toJson(entries),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            Constants.LOG.error("[CreateBrowser] Failed to save favorites.json", e);
        }
    }
}
