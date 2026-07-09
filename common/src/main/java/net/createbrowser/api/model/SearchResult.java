package net.createbrowser.api.model;

import java.util.List;

/** Paginated response from GET /api/schematics. */
public final class SearchResult {

    private List<SchematicEntry> items;
    private int page;
    private int pageSize;
    private boolean hasPrev;
    private boolean hasNext;
    private int total;
    private String term;

    SearchResult() {}

    public List<SchematicEntry> items() { return items != null ? items : List.of(); }
    public int page() { return page; }
    public int pageSize() { return pageSize; }
    public boolean hasPrev() { return hasPrev; }
    public boolean hasNext() { return hasNext; }
    public int total() { return total; }
    public String term() { return term != null ? term : ""; }

    public int totalPages() {
        if (pageSize <= 0) return 1;
        return Math.max(1, (int) Math.ceil((double) total / pageSize));
    }
}
