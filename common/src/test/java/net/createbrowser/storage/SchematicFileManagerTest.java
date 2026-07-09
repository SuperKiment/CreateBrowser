package net.createbrowser.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SchematicFileManagerTest {

    /** Minimal valid GZip header. */
    private static final byte[] VALID_NBT = {0x1F, (byte) 0x8B, 0x00, 0x00, 0x00, 0x00};

    @Test
    void savesSchematicWithCorrectName(@TempDir Path dir) throws IOException {
        Path saved = SchematicFileManager.saveSchematic(dir, "windmill-farm", VALID_NBT);
        assertEquals("windmill-farm.nbt", saved.getFileName().toString());
        assertTrue(Files.exists(saved));
    }

    @Test
    void writesExactBytes(@TempDir Path dir) throws IOException {
        Path saved = SchematicFileManager.saveSchematic(dir, "test", VALID_NBT);
        assertArrayEquals(VALID_NBT, Files.readAllBytes(saved));
    }

    @Test
    void resolvesConflictWithSuffix(@TempDir Path dir) throws IOException {
        SchematicFileManager.saveSchematic(dir, "farm", VALID_NBT);
        Path second = SchematicFileManager.saveSchematic(dir, "farm", VALID_NBT);
        assertEquals("farm_1.nbt", second.getFileName().toString());
    }

    @Test
    void resolvesTwoConflicts(@TempDir Path dir) throws IOException {
        SchematicFileManager.saveSchematic(dir, "farm", VALID_NBT);
        SchematicFileManager.saveSchematic(dir, "farm", VALID_NBT);
        Path third = SchematicFileManager.saveSchematic(dir, "farm", VALID_NBT);
        assertEquals("farm_2.nbt", third.getFileName().toString());
    }

    @Test
    void rejectsDataWithoutGzipHeader(@TempDir Path dir) {
        byte[] invalid = {0x00, 0x01, 0x02, 0x03};
        assertThrows(IOException.class,
            () -> SchematicFileManager.saveSchematic(dir, "bad", invalid));
    }

    @Test
    void rejectsTooShortData(@TempDir Path dir) {
        byte[] tooShort = {0x1F};
        assertThrows(IOException.class,
            () -> SchematicFileManager.saveSchematic(dir, "short", tooShort));
    }

    @Test
    void rejectsEmptyData(@TempDir Path dir) {
        assertThrows(IOException.class,
            () -> SchematicFileManager.saveSchematic(dir, "empty", new byte[0]));
    }

    @Test
    void sanitizesSpecialCharsInName(@TempDir Path dir) throws IOException {
        Path saved = SchematicFileManager.saveSchematic(dir, "my schematic/v2", VALID_NBT);
        String name = saved.getFileName().toString();
        assertFalse(name.contains("/"), "Sanitized name must not contain /");
        assertFalse(name.contains(" "), "Sanitized name must not contain spaces");
        assertTrue(name.endsWith(".nbt"));
    }

    @Test
    void createsDirIfAbsent(@TempDir Path base) throws IOException {
        Path subDir = base.resolve("newdir");
        assertFalse(Files.exists(subDir));
        SchematicFileManager.saveSchematic(subDir, "test", VALID_NBT);
        assertTrue(Files.isDirectory(subDir));
    }
}
