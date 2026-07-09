package net.createbrowser.api.model;

import com.google.gson.annotations.SerializedName;

/** A single material entry from a schematic's block list. */
public final class Material {

    @SerializedName("block_id") private String blockId;
    @SerializedName("name") private String name;
    @SerializedName("count") private int count;

    Material() {}

    public String blockId() { return blockId; }
    public String name() { return name; }
    public int count() { return count; }

    @Override
    public String toString() {
        return count + "x " + name;
    }
}
