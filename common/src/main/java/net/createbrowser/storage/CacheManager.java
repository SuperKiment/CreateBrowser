package net.createbrowser.storage;

import net.createbrowser.Constants;
import net.createbrowser.platform.Services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

/**
 * On-disk cache for API responses (JSON) and thumbnails (PNG).
 * Each cached entry is a single file; mtime is used as last-access timestamp for LRU eviction.
 * Files older than their declared TTL are considered expired.
 */
public final class CacheManager {

    private static final String API_DIR = "api";
    private static final String THUMB_DIR = "thumbs";

    private static volatile CacheManager instance;

    private static final long EVICT_INTERVAL_MS = 60_000L;

    private final Path baseDir;
    private final LongSupplier maxBytesSupplier;
    private volatile long lastEvictMs = 0;

    /** Cache without automatic size enforcement (unit tests call {@link #evictLru} directly). */
    public CacheManager(Path baseDir) {
        this(baseDir, null);
    }

    /** @param maxBytesSupplier size limit for automatic LRU eviction after writes; null disables it. */
    public CacheManager(Path baseDir, LongSupplier maxBytesSupplier) {
        this.baseDir = baseDir;
        this.maxBytesSupplier = maxBytesSupplier;
    }

    public static CacheManager get() {
        CacheManager local = instance;
        if (local == null) {
            synchronized (CacheManager.class) {
                local = instance;
                if (local == null) {
                    try {
                        local = new CacheManager(PathResolver.cacheDir(),
                            () -> Services.CONFIG.cacheMaxSizeMB() * 1024L * 1024L);
                    } catch (IOException e) {
                        Constants.LOG.error("[CreateBrowser] Cannot init CacheManager", e);
                        throw new IllegalStateException(e);
                    }
                    instance = local;
                }
            }
        }
        return local;
    }

    /** @param ttlMin TTL in minutes; entries older than this are treated as missing. */
    public Optional<String> getApi(String key, int ttlMin) {
        Path file = apiFile(key);
        if (!Files.exists(file)) return Optional.empty();
        try {
            long ageMs = System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis();
            if (ageMs > ttlMin * 60_000L) return Optional.empty();
            touch(file);
            return Optional.of(Files.readString(file));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void putApi(String key, String body) {
        Path file = apiFile(key);
        try {
            ensureDir(file.getParent());
            Files.writeString(file, body);
        } catch (IOException e) {
            Constants.LOG.warn("[CreateBrowser] Cache write failed for api/{}: {}", key, e.getMessage());
        }
        maybeEvict();
    }

    public Optional<Path> getThumb(String url) {
        Path file = thumbFile(url);
        if (!Files.exists(file)) return Optional.empty();
        touch(file);
        return Optional.of(file);
    }

    public void putThumb(String url, byte[] bytes) {
        Path file = thumbFile(url);
        try {
            ensureDir(file.getParent());
            Files.write(file, bytes);
        } catch (IOException e) {
            Constants.LOG.warn("[CreateBrowser] Cache write failed for thumb {}: {}", url, e.getMessage());
        }
        maybeEvict();
    }

    /** Enforces the configured size limit, at most once per {@link #EVICT_INTERVAL_MS}. */
    private void maybeEvict() {
        if (maxBytesSupplier == null) return;
        long now = System.currentTimeMillis();
        if (now - lastEvictMs < EVICT_INTERVAL_MS) return;
        lastEvictMs = now;
        evictLru(maxBytesSupplier.getAsLong());
    }

    /** Total size in bytes of all cache entries on disk. */
    public long currentSizeBytes() {
        return walkAllFiles().stream().mapToLong(p -> {
            try { return Files.size(p); } catch (IOException e) { return 0L; }
        }).sum();
    }

    /** Evict least-recently-used files until under {@code maxBytes}. */
    public void evictLru(long maxBytes) {
        List<Path> files = new ArrayList<>(walkAllFiles());
        long size = currentSizeBytes();
        if (size <= maxBytes) return;
        files.sort(Comparator.comparingLong(p -> {
            try { return Files.getLastModifiedTime(p).toMillis(); }
            catch (IOException e) { return Long.MAX_VALUE; }
        }));
        for (Path p : files) {
            if (size <= maxBytes) break;
            try {
                long s = Files.size(p);
                Files.delete(p);
                size -= s;
            } catch (IOException ignore) { }
        }
    }

    public Path apiFile(String key) {
        return baseDir.resolve(API_DIR).resolve(sha256(key) + ".json");
    }

    public Path thumbFile(String url) {
        return baseDir.resolve(THUMB_DIR).resolve(sha256(url) + ".png");
    }

    private List<Path> walkAllFiles() {
        List<Path> all = new ArrayList<>();
        for (String sub : new String[]{API_DIR, THUMB_DIR}) {
            Path d = baseDir.resolve(sub);
            if (!Files.exists(d)) continue;
            try (Stream<Path> s = Files.walk(d)) {
                s.filter(Files::isRegularFile).forEach(all::add);
            } catch (IOException ignore) { }
        }
        return all;
    }

    private static void ensureDir(Path dir) throws IOException {
        if (dir != null && !Files.exists(dir)) Files.createDirectories(dir);
    }

    private static void touch(Path file) {
        try {
            Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
        } catch (IOException ignore) { }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
