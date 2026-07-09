package net.createbrowser.api.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/** Tolerant materials resolver: createmod.com may return an array of objects, a JSON-encoded string, or anything else. */
final class MaterialsJson {

    private static final Gson GSON = new Gson();

    private MaterialsJson() {}

    static List<Material> extract(JsonElement el) {
        if (el == null || el.isJsonNull()) return List.of();
        JsonElement source = el;
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            try {
                source = JsonParser.parseString(el.getAsString());
            } catch (Exception e) {
                return List.of();
            }
        }
        if (!source.isJsonArray()) return List.of();
        List<Material> out = new ArrayList<>();
        for (JsonElement item : source.getAsJsonArray()) {
            if (!item.isJsonObject()) continue;
            try {
                Material m = GSON.fromJson(item, Material.class);
                if (m != null) out.add(m);
            } catch (Exception ignored) {
                // skip malformed item
            }
        }
        return out;
    }
}
