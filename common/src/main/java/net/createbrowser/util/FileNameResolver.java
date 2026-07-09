package net.createbrowser.util;

import java.nio.file.Files;
import java.nio.file.Path;

/** Sanitizes schematic file names and resolves conflicts in the schematics folder. */
public final class FileNameResolver {

    private FileNameResolver() {}

    /** Replaces any character outside [a-zA-Z0-9._-] with an underscore. */
    public static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    /**
     * Returns a unique path under {@code dir} for a file named {@code baseName + "." + ext}.
     * If the file already exists, appends _1, _2, … up to 1000 times before giving up.
     */
    public static Path resolveUnique(Path dir, String baseName, String ext) {
        String sanitized = sanitize(baseName);
        Path candidate = dir.resolve(sanitized + "." + ext);
        int counter = 1;
        while (Files.exists(candidate) && counter <= 1000) {
            candidate = dir.resolve(sanitized + "_" + counter + "." + ext);
            counter++;
        }
        return candidate;
    }
}
