package net.createbrowser.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileNameResolverTest {

    @Test
    void sanitizesForwardSlash() {
        assertEquals("path_to_file", FileNameResolver.sanitize("path/to/file"));
    }

    @Test
    void sanitizesAngleBrackets() {
        assertEquals("_test_", FileNameResolver.sanitize("<test>"));
    }

    @Test
    void sanitizesSpaces() {
        assertEquals("my_schematic", FileNameResolver.sanitize("my schematic"));
    }

    @Test
    void sanitizesAccentedChars() {
        assertEquals("caf_", FileNameResolver.sanitize("café"));
    }

    @Test
    void preservesAllowedChars() {
        assertEquals("windmill-farm_v1.2", FileNameResolver.sanitize("windmill-farm_v1.2"));
    }

    @Test
    void sanitizesBackslash() {
        assertEquals("a_b", FileNameResolver.sanitize("a\\b"));
    }

    @Test
    void resolveUniqueReturnsSameNameWhenNoConflict(@TempDir Path dir) {
        Path result = FileNameResolver.resolveUnique(dir, "myfile", "nbt");
        assertEquals("myfile.nbt", result.getFileName().toString());
    }

    @Test
    void resolveUniqueAppendsSuffixOnConflict(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("myfile.nbt"));
        Path result = FileNameResolver.resolveUnique(dir, "myfile", "nbt");
        assertEquals("myfile_1.nbt", result.getFileName().toString());
    }

    @Test
    void resolveUniqueIncrementsCorrectly(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("farm.nbt"));
        Files.createFile(dir.resolve("farm_1.nbt"));
        Path result = FileNameResolver.resolveUnique(dir, "farm", "nbt");
        assertEquals("farm_2.nbt", result.getFileName().toString());
    }
}
