package net.createbrowser.storage;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves CreateBrowser data directories under the Minecraft instance folder. */
public final class PathResolver {

    private PathResolver() {}

    /** {@code <instance>/createbrowser/} — created if missing. */
    public static Path dataDir() throws IOException {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("createbrowser");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    /** {@code <instance>/createbrowser/cache/} — created if missing. */
    public static Path cacheDir() throws IOException {
        Path dir = dataDir().resolve("cache");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }
}
