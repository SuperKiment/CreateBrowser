package net.createbrowser.api.model;

import com.google.gson.JsonElement;

import java.util.List;

/** A single search result from the createmod.com API. */
public final class SchematicEntry {

    private String name;
    private String title;
    private JsonElement author;
    private int views;
    private int downloads;
    private float rating;
    private List<String> images;
    private JsonElement categories;
    private JsonElement tags;

    SchematicEntry() {}

    public String name() { return name; }
    public String title() { return title; }
    public String author() { return AuthorJson.extract(author); }
    public int views() { return views; }
    public int downloads() { return downloads; }
    public float rating() { return rating; }
    public List<String> images() { return images != null ? images : List.of(); }
    public List<String> categories() { return AuthorJson.extractList(categories); }
    public List<String> tags() { return AuthorJson.extractList(tags); }
}
