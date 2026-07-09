package net.createbrowser.api.model;

import javax.annotation.Nullable;

/** Optional filtering and sorting parameters for a schematic search. */
public record SearchFilters(@Nullable String category, @Nullable SizeBucket size, SortMode sort) {

    public static final SearchFilters NONE = new SearchFilters(null, null, SortMode.RECENT);

    public enum SortMode {
        RECENT("recent"),
        DOWNLOADS("downloads"),
        RATING("rating"),
        VIEWS("views");

        private final String apiValue;
        SortMode(String v) { this.apiValue = v; }
        public String apiValue() { return apiValue; }
    }

    /** Coarse block-count buckets for filtering by size. */
    public enum SizeBucket {
        SMALL(0, 500),
        MEDIUM(500, 5000),
        LARGE(5000, Integer.MAX_VALUE);

        private final int min;
        private final int max;
        SizeBucket(int min, int max) { this.min = min; this.max = max; }
        public int min() { return min; }
        public int max() { return max; }
    }
}
