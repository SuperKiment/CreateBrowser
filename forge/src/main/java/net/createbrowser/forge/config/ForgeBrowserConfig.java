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

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("general");
        API_KEY = BUILDER
            .comment("API key for createmod.com — get yours at https://createmod.com/settings")
            .define("apiKey", "");
        MOD_SECRET = BUILDER
            .comment("Mod download secret — obtain from the CreateBrowser maintainer")
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
            .comment("Number of results per page (6-48)")
            .defineInRange("pageSize", 24, 6, 48);
        SHOW_THUMBNAILS = BUILDER
            .comment("Show thumbnail images on result rows and detail screen")
            .define("showThumbnails", true);
        BUILDER.pop();

        BUILDER.push("cache");
        CACHE_MAX_SIZE_MB = BUILDER
            .comment("Maximum on-disk cache size in MB (10-500)")
            .defineInRange("maxSizeMB", 50, 10, 500);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    @Override public String apiKey() { return API_KEY.get(); }
    @Override public String modSecret() { return MOD_SECRET.get(); }
    @Override public int timeoutSec() { return TIMEOUT_SEC.get(); }
    @Override public int maxReqPerSec() { return MAX_REQ_PER_SEC.get(); }
    @Override public int maxRetries() { return MAX_RETRIES.get(); }
    @Override public int pageSize() { return PAGE_SIZE.get(); }
    @Override public int searchTtlMin() { return 15; }
    @Override public int detailTtlMin() { return 60; }
    @Override public int cacheMaxSizeMB() { return CACHE_MAX_SIZE_MB.get(); }
    @Override public boolean showThumbnails() { return SHOW_THUMBNAILS.get(); }
}
