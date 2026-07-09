package net.createbrowser.storage;

import net.createbrowser.nbt.NbtMetadataReader;
import net.createbrowser.util.FileNameResolver;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Writes .nbt files to the Minecraft instance's schematics/ directory. */
public final class SchematicFileManager {

    private SchematicFileManager() {}

    /** Returns the schematics directory, creating it if needed. */
    public static Path schematicsDir() throws IOException {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("schematics");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    /** Convenience overload that resolves the schematics directory from the Minecraft instance. */
    public static Path saveSchematic(String name, byte[] data) throws IOException {
        return saveSchematic(schematicsDir(), name, data);
    }

    /**
     * Validates and writes {@code data} to {@code dir} as a .nbt file.
     * The file name is sanitized and deduplicated automatically.
     *
     * @return the path of the file that was written
     * @throws IOException if the data is not a valid GZip-compressed NBT
     */
    public static Path saveSchematic(Path dir, String name, byte[] data) throws IOException {
        if (!NbtMetadataReader.isValidGzipNbt(data)) {
            throw new IOException("Invalid .nbt file: missing GZip header (0x1F 0x8B)");
        }

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path target = FileNameResolver.resolveUnique(dir, name, "nbt");
        Files.write(target, data, StandardOpenOption.CREATE_NEW);
        return target;
    }

    /** Lists the .nbt files in the schematics/ directory, newest first. */
    public static List<LocalFile> listLocal() throws IOException {
        return listLocal(schematicsDir());
    }

    public static List<LocalFile> listLocal(Path dir) throws IOException {
        if (!Files.exists(dir)) return List.of();
        List<LocalFile> result = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".nbt"))
             .filter(Files::isRegularFile)
             .forEach(p -> {
                 try {
                     result.add(new LocalFile(
                         p,
                         stripExt(p.getFileName().toString()),
                         Files.size(p),
                         Files.getLastModifiedTime(p).toMillis()
                     ));
                 } catch (IOException ignore) { }
             });
        }
        result.sort(Comparator.comparingLong(LocalFile::modifiedAt).reversed());
        return result;
    }

    /** Deletes a .nbt file. Refuses paths outside the schematics/ directory. */
    public static void delete(Path file) throws IOException {
        delete(schematicsDir(), file);
    }

    public static void delete(Path schemDir, Path file) throws IOException {
        Path normalizedDir = schemDir.toAbsolutePath().normalize();
        Path normalizedFile = file.toAbsolutePath().normalize();
        if (!normalizedFile.startsWith(normalizedDir)) {
            throw new IOException("Refusing to delete outside schematics dir: " + normalizedFile);
        }
        Files.deleteIfExists(normalizedFile);
    }

    /** Renames a .nbt file in the schematics/ directory; sanitizes and resolves conflicts. */
    public static Path rename(Path file, String newName) throws IOException {
        return rename(schematicsDir(), file, newName);
    }

    public static Path rename(Path schemDir, Path file, String newName) throws IOException {
        Path normalizedDir = schemDir.toAbsolutePath().normalize();
        Path normalizedFile = file.toAbsolutePath().normalize();
        if (!normalizedFile.startsWith(normalizedDir)) {
            throw new IOException("Refusing to rename outside schematics dir: " + normalizedFile);
        }
        Path target = FileNameResolver.resolveUnique(normalizedDir, newName, "nbt");
        Files.move(normalizedFile, target);
        return target;
    }

    private static String stripExt(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }
}
