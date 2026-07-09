package net.createbrowser.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CacheManagerTest {

    @Test
    void apiPutAndGetRoundTrip(@TempDir Path dir) {
        CacheManager c = new CacheManager(dir);
        c.putApi("key1", "{\"hello\":1}");
        Optional<String> got = c.getApi("key1", 60);
        assertTrue(got.isPresent());
        assertEquals("{\"hello\":1}", got.get());
    }

    @Test
    void apiMissingKeyReturnsEmpty(@TempDir Path dir) {
        assertTrue(new CacheManager(dir).getApi("nope", 60).isEmpty());
    }

    @Test
    void apiExpiredEntryReturnsEmpty(@TempDir Path dir) throws IOException {
        CacheManager c = new CacheManager(dir);
        c.putApi("k", "body");
        Path file = c.apiFile("k");
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() - 2 * 3600_000L));
        assertTrue(c.getApi("k", 60).isEmpty(), "60-min TTL should expire 2-hour-old entry");
    }

    @Test
    void thumbPutAndGetRoundTrip(@TempDir Path dir) throws IOException {
        CacheManager c = new CacheManager(dir);
        byte[] bytes = {1, 2, 3, 4};
        c.putThumb("https://example.com/x.png", bytes);
        Optional<Path> got = c.getThumb("https://example.com/x.png");
        assertTrue(got.isPresent());
        assertArrayEquals(bytes, Files.readAllBytes(got.get()));
    }

    @Test
    void currentSizeBytesSumsAllEntries(@TempDir Path dir) {
        CacheManager c = new CacheManager(dir);
        c.putApi("a", "12345");
        c.putThumb("u", new byte[10]);
        assertEquals(15L, c.currentSizeBytes());
    }

    @Test
    void evictLruDropsOldestUntilUnderLimit(@TempDir Path dir) throws IOException {
        CacheManager c = new CacheManager(dir);
        c.putApi("old", "X".repeat(100));
        c.putApi("mid", "Y".repeat(100));
        c.putApi("new", "Z".repeat(100));

        long now = System.currentTimeMillis();
        Files.setLastModifiedTime(c.apiFile("old"), FileTime.fromMillis(now - 30_000));
        Files.setLastModifiedTime(c.apiFile("mid"), FileTime.fromMillis(now - 20_000));
        Files.setLastModifiedTime(c.apiFile("new"), FileTime.fromMillis(now - 10_000));

        c.evictLru(150L);

        assertFalse(Files.exists(c.apiFile("old")), "oldest must be evicted");
        assertTrue(Files.exists(c.apiFile("new")), "newest must survive");
        assertTrue(c.currentSizeBytes() <= 150L);
    }
}
