package net.createbrowser.api.source;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.createbrowser.api.model.SchematicDetail;
import net.createbrowser.api.model.SchematicEntry;
import net.createbrowser.api.model.SearchFilters;
import net.createbrowser.api.model.SearchResult;
import net.createbrowser.storage.LocalFile;
import net.createbrowser.storage.SchematicFileManager;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** SchematicSource backed by the local schematics/ directory. */
public final class LocalSource implements SchematicSource {

    private static final Gson GSON = new Gson();

    @Override public String id() { return "local"; }
    @Override public String displayName() { return "Local"; }

    @Override
    public SearchResult search(String query, int page, int pageSize) throws IOException {
        return search(query, page, pageSize, SearchFilters.NONE);
    }

    @Override
    public SearchResult search(String query, int page, int pageSize, SearchFilters filters) throws IOException {
        List<LocalFile> files = SchematicFileManager.listLocal();
        String q = query == null ? "" : query.toLowerCase();
        List<LocalFile> filtered = new ArrayList<>(files.size());
        for (LocalFile f : files) {
            if (q.isEmpty() || f.displayName().toLowerCase().contains(q)) {
                filtered.add(f);
            }
        }
        int total = filtered.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        List<LocalFile> pageItems = from >= total ? List.of() : filtered.subList(from, to);
        return buildResult(pageItems, page, pageSize, total);
    }

    public List<LocalFile> listAll() throws IOException {
        return SchematicFileManager.listLocal();
    }

    @Override
    public SchematicDetail detail(String name) throws IOException {
        for (LocalFile f : SchematicFileManager.listLocal()) {
            if (f.displayName().equals(name)) {
                JsonObject o = new JsonObject();
                o.addProperty("name", name);
                o.addProperty("title", name);
                o.addProperty("description", "Local file at " + f.path());
                o.addProperty("block_count", 0);
                o.add("mods", new JsonArray());
                o.add("images", new JsonArray());
                return GSON.fromJson(o, SchematicDetail.class);
            }
        }
        throw new IOException("not_found");
    }

    @Override
    public byte[] download(String name) throws IOException {
        for (LocalFile f : SchematicFileManager.listLocal()) {
            if (f.displayName().equals(name)) {
                return Files.readAllBytes(f.path());
            }
        }
        throw new IOException("not_found");
    }

    @Override public boolean supportsDownload() { return true; }

    private static SearchResult buildResult(List<LocalFile> items, int page, int pageSize, int total) {
        JsonObject o = new JsonObject();
        JsonArray arr = new JsonArray();
        for (LocalFile f : items) {
            JsonObject e = new JsonObject();
            e.addProperty("name", f.displayName());
            e.addProperty("title", f.displayName());
            arr.add(e);
        }
        o.add("items", arr);
        o.addProperty("page", page);
        o.addProperty("pageSize", pageSize);
        o.addProperty("hasPrev", page > 1);
        o.addProperty("hasNext", page * pageSize < total);
        o.addProperty("total", total);
        return GSON.fromJson(o, SearchResult.class);
    }
}
