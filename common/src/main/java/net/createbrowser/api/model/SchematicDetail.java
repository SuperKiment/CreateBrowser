package net.createbrowser.api.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Full detail of a schematic, returned by the GET /api/schematics/{name} endpoint. */
public final class SchematicDetail {

    private String id;
    private String name;
    private String title;
    private JsonElement author;
    private String description;
    private String content;
    private String excerpt;
    private int views;
    private int downloads;
    private float rating;
    private List<String> images;
    private JsonElement categories;
    private JsonElement tags;
    private JsonElement materials;
    private JsonElement mods;
    private String featuredImage;
    private List<String> gallery;
    private Dimensions dimensions;
    private int dimX;
    private int dimY;
    private int dimZ;
    @SerializedName(value = "blockCount", alternate = "block_count") private int blockCount;

    SchematicDetail() {}

    public String name() { return name; }
    public String title() { return title; }
    public String author() { return AuthorJson.extract(author); }
    public String description() { return firstNonBlank(description, content, excerpt); }
    public int views() { return views; }
    public int downloads() { return downloads; }
    public float rating() { return rating; }
    public List<String> images() { return SchematicImages.resolve(images, id, featuredImage, gallery); }
    public List<String> categories() { return AuthorJson.extractList(categories); }
    public List<String> tags() { return AuthorJson.extractList(tags); }
    public List<Material> materials() { return MaterialsJson.extract(materials); }
    public List<ModRequirement> mods() { return AuthorJson.extractMods(mods); }
    /** Nested {@code dimensions} object if present, else the flat dimX/dimY/dimZ fields; null when unknown. */
    public Dimensions dimensions() {
        if (dimensions != null) return dimensions;
        return dimX > 0 || dimY > 0 || dimZ > 0 ? new Dimensions(dimX, dimY, dimZ) : null;
    }
    public int blockCount() { return blockCount; }

    public static final class Dimensions {
        private int x;
        private int y;
        private int z;

        Dimensions() {}

        Dimensions(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int x() { return x; }
        public int y() { return y; }
        public int z() { return z; }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }
}
