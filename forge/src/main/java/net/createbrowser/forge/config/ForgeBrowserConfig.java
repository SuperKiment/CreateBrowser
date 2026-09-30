package net.createbrowser.forge.config;

import net.createbrowser.config.BrowserConfig;
import net.minecraftforge.common.ForgeConfigSpec;

/** BrowserConfig implementation backed by a Forge client TOML config file. */
public final class ForgeBrowserConfig implements BrowserConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> API_KEY;
    private static final ForgeConfigSpec.ConfigValue<String> MOD_SECRET;
    private static final ForgeConfigSpec.IntValue TIMEOUT_SEC;
    private static final ForgeConfigSpec.IntValue MAX_REQ_PER_SEC;
    private static final ForgeConfigSpec.IntValue MAX_RETRIES;
    private static final ForgeConfigSpec.IntValue PAGE_SIZE;
    private static final ForgeConfigSpec.BooleanValue SHOW_THUMBNAILS;
    private static final ForgeConfigSpec.IntValue CACHE_MAX_SIZE_MB;
    private static final ForgeConfigSpec.IntValue SEARCH_TTL_MIN;
    private static final ForgeConfigSpec.IntValue DETAIL_TTL_MIN;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("general");
        API_KEY = BUILDER
            .comment("API key for createmod.com (search, detail, download) — generate one at https://createmod.com/settings/api-keys")
            .define("apiKey", "");
        MOD_SECRET = BUILDER
            .comment("Optional HMAC mod secret issued by createmod.com — only used for downloads when apiKey is empty")
            .define("modSecret", "");
        BUILDER.pop();

        BUILDER.push("network");
        TIMEOUT_SEC = BUILDER
            .comment("HTTP request timeout in seconds")
            .defineInRange("timeoutSeconds", 10, 1, 60);
        MAX_REQ_PER_SEC = BUILDER
            .comment("Maximum API requests per second (1-10)")
            .defineInRange("maxRequestsPerSecond", 2, 1, 10);
        MAX_RETRIES = BUILDER
            .comment("Maximum retry attempts on server errors (0-10)")
            .defineInRange("maxRetries", 3, 0, 10);
        BUILDER.pop();

        BUILDER.push("ui");
        PAGE_SIZE = BUILDER
            .comment("Results per page; createmod.com accepts 8, 16, 24, 32, 64 or 100 (other values fall back to 24)")
            .defineInRange("pageSize", 24, 8, 100);
        SHOW_THUMBNAILS = BUILDER
            .comment("Show thumbnail images on result rows and detail screen")
            .define("showThumbnails", true);
        BUILDER.pop();

        BUILDER.push("cache");
        CACHE_MAX_SIZE_MB = BUILDER
            .comment("Maximum on-disk cache size in MB (10-500)")
            .defineInRange("maxSizeMB", 50, 10, 500);
        SEARCH_TTL_MIN = BUILDER
            .comment("Search results cache TTL in minutes (1-1440)")
            .defineInRange("searchTtlMinutes", 15, 1, 1440);
        DETAIL_TTL_MIN = BUILDER
            .comment("Detail pages cache TTL in minutes (1-1440)")
            .defineInRange("detailTtlMinutes", 60, 1, 1440);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    @Override public String apiKey() { return API_KEY.get(); }
    @Override public String modSecret() { return MOD_SECRET.get(); }
    @Override public int timeoutSec() { return TIMEOUT_SEC.get(); }
    @Override public int maxReqPerSec() { return MAX_REQ_PER_SEC.get(); }
    @Override public int maxRetries() { return MAX_RETRIES.get(); }
    @Override public int pageSize() { return PAGE_SIZE.get(); }
    @Override public int searchTtlMin() { return SEARCH_TTL_MIN.get(); }
    @Override public int detailTtlMin() { return DETAIL_TTL_MIN.get(); }
    @Override public int cacheMaxSizeMB() { return CACHE_MAX_SIZE_MB.get(); }
    @Override public boolean showThumbnails() { return SHOW_THUMBNAILS.get(); }

    @Override public void setApiKey(String value) { API_KEY.set(value); }
    @Override public void setModSecret(String value) { MOD_SECRET.set(value); }
    @Override public void setShowThumbnails(boolean value) { SHOW_THUMBNAILS.set(value); }
}
