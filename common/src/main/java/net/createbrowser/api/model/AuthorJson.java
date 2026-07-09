package net.createbrowser.api.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Tolerant string resolvers: createmod.com may return strings, objects with a name field, or arrays of either. */
final class AuthorJson {

    private static final String[] CANDIDATE_KEYS = {"username", "name", "title", "displayName", "slug", "id"};

    private AuthorJson() {}

    static String extract(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonPrimitive()) return el.getAsString();
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            for (String k : CANDIDATE_KEYS) {
                if (obj.has(k)) {
                    JsonElement v = obj.get(k);
                    if (v != null && !v.isJsonNull() && v.isJsonPrimitive()) {
                        return v.getAsString();
                    }
                }
            }
        }
        return null;
    }

    static List<String> extractList(JsonElement el) {
        if (el == null || !el.isJsonArray()) return List.of();
        JsonArray arr = el.getAsJsonArray();
        List<String> out = new ArrayList<>(arr.size());
        for (JsonElement item : arr) {
            String s = extract(item);
            if (s != null) out.add(s);
        }
        return out;
    }

    private static final String[] VERSION_KEYS = {"version", "versionRange", "minVersion", "min_version", "required_version"};

    static String extractVersion(JsonElement el) {
        if (el == null || !el.isJsonObject()) return null;
        JsonObject obj = el.getAsJsonObject();
        for (String k : VERSION_KEYS) {
            if (obj.has(k)) {
                JsonElement v = obj.get(k);
                if (v != null && !v.isJsonNull() && v.isJsonPrimitive()) {
                    return v.getAsString();
                }
            }
        }
        return null;
    }

    static List<ModRequirement> extractMods(JsonElement el) {
        if (el == null || !el.isJsonArray()) return List.of();
        JsonArray arr = el.getAsJsonArray();
        List<ModRequirement> out = new ArrayList<>(arr.size());
        for (JsonElement item : arr) {
            String id = extract(item);
            if (id == null) continue;
            String version = extractVersion(item);
            out.add(new ModRequirement(id, version));
        }
        return out;
    }
}
