package net.createbrowser.storage;

import java.nio.file.Path;

/** Metadata about a .nbt file in the local schematics/ directory. */
public record LocalFile(Path path, String displayName, long sizeBytes, long modifiedAt) {}
