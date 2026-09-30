package net.createbrowser.api.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Resolves image URLs: createmod.com returns bare file names (featuredImage, gallery) served under /api/files. */
final class SchematicImages {

    private static final String FILES_URL = "https://createmod.com/api/files/schematics/";

    private SchematicImages() {}

    static List<String> resolve(List<String> images, String id, String featuredImage, List<String> gallery) {
        if (images != null && !images.isEmpty()) return images;
        if (id == null || id.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        if (featuredImage != null && !featuredImage.isEmpty()) out.add(url(id, featuredImage));
        if (gallery != null) {
            for (String g : gallery) {
                if (g != null && !g.isEmpty() && !g.equals(featuredImage)) out.add(url(id, g));
            }
        }
        return out;
    }

    private static String url(String id, String file) {
        if (file.startsWith("http://") || file.startsWith("https://")) return file;
        return FILES_URL + id + "/" + URLEncoder.encode(file, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
