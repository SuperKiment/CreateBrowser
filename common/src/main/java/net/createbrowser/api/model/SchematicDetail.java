package net.createbrowser.api.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Full detail of a schematic, returned by the GET /api/schematics/{name} endpoint. */
public final class SchematicDetail {

    private String name;
    private String title;
    private JsonElement author;
    private String description;
    private int views;
    private int downloads;
    private float rating;
    private List<String> images;
    private JsonElement categories;
    private JsonElement tags;
    private JsonElement materials;
    private JsonElement mods;
    private Dimensions dimensions;
    @SerializedName("block_count") private int blockCount;

    SchematicDetail() {}

    public String name() { return name; }
    public String title() { return title; }
    public String author() { return AuthorJson.extract(author); }
    public String description() { return description != null ? description : ""; }
    public int views() { return views; }
    public int downloads() { return downloads; }
    public float rating() { return rating; }
    public List<String> images() { return images != null ? images : List.of(); }
    public List<String> categories() { return AuthorJson.extractList(categories); }
    public List<String> tags() { return AuthorJson.extractList(tags); }
    public List<Material> materials() { return MaterialsJson.extract(materials); }
    public List<ModRequirement> mods() { return AuthorJson.extractMods(mods); }
    public Dimensions dimensions() { return dimensions; }
    public int blockCount() { return blockCount; }

    public static final class Dimensions {
        private int x;
        private int y;
        private int z;

        Dimensions() {}

        public int x() { return x; }
        public int y() { return y; }
        public int z() { return z; }
    }
}
