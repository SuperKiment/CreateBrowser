package net.createbrowser.api.model;

import javax.annotation.Nullable;

/** Optional filtering and sorting parameters for a schematic search. */
public record SearchFilters(@Nullable String category, @Nullable SizeBucket size, SortMode sort) {

    public static final SearchFilters NONE = new SearchFilters(null, null, SortMode.RELEVANCE);

    /** Sort modes; apiValue is the createmod.com numeric sort, null when only a client-side sort exists. */
    public enum SortMode {
        RELEVANCE(1),
        RECENT(2),
        DOWNLOADS(null),
        RATING(4),
        VIEWS(6);

        private final Integer apiValue;
        SortMode(Integer v) { this.apiValue = v; }
        @Nullable public Integer apiValue() { return apiValue; }
    }

    /** Coarse block-count buckets for filtering by size; ANY also keeps entries with an unknown count. */
    public enum SizeBucket {
        ANY(0, Integer.MAX_VALUE),
        SMALL(1, 500),
        MEDIUM(500, 5000),
        LARGE(5000, Integer.MAX_VALUE);

        private final int min;
        private final int max;
        SizeBucket(int min, int max) { this.min = min; this.max = max; }
        public int min() { return min; }
        public int max() { return max; }

        /** True when a block count falls in this bucket; unknown counts ({@code <= 0}) only match ANY. */
        public boolean matches(int blockCount) {
            return this == ANY || (blockCount >= min && blockCount < max);
        }
    }
}
