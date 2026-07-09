package net.createbrowser.config;

/** Exposes user-facing configuration values to common/ code. Implemented by each loader. */
public interface BrowserConfig {

    String apiKey();
    String modSecret();
    int timeoutSec();
    int maxReqPerSec();
    int maxRetries();
    int pageSize();
    int searchTtlMin();
    int detailTtlMin();
    int cacheMaxSizeMB();
    boolean showThumbnails();

    /** Fallback used when no SPI implementation is available (e.g. unit tests). */
    BrowserConfig DEFAULT = new BrowserConfig() {
        public String apiKey() { return ""; }
        public String modSecret() { return ""; }
        public int timeoutSec() { return 10; }
        public int maxReqPerSec() { return 2; }
        public int maxRetries() { return 3; }
        public int pageSize() { return 24; }
        public int searchTtlMin() { return 15; }
        public int detailTtlMin() { return 60; }
        public int cacheMaxSizeMB() { return 50; }
        public boolean showThumbnails() { return true; }
    };
}
