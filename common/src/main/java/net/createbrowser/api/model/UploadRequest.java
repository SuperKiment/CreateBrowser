package net.createbrowser.api.model;

import java.nio.file.Path;
import java.util.List;

/** Parameters for uploading a schematic to createmod.com. */
public record UploadRequest(
    Path file,
    String title,
    String description,
    List<String> categories,
    List<String> tags,
    List<String> requiredMods
) {}
