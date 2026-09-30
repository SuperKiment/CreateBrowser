package net.createbrowser.api.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/** A single search result from the createmod.com API. */
public final class SchematicEntry {

    private String id;
    private String name;
    private String title;
    private JsonElement author;
    private int views;
    private int downloads;
    private float rating;
    private List<String> images;
    private JsonElement categories;
    private JsonElement tags;
    private String featuredImage;
    private List<String> gallery;
    @SerializedName(value = "blockCount", alternate = "block_count") private int blockCount;

    SchematicEntry() {}

    public String name() { return name; }
    public String title() { return title; }
    public String author() { return AuthorJson.extract(author); }
    public int views() { return views; }
    public int downloads() { return downloads; }
    public float rating() { return rating; }
    public List<String> images() { return SchematicImages.resolve(images, id, featuredImage, gallery); }
    public List<String> categories() { return AuthorJson.extractList(categories); }
    public List<String> tags() { return AuthorJson.extractList(tags); }
    /** Total block count, or 0 when the API did not report it. */
    public int blockCount() { return blockCount; }
}
