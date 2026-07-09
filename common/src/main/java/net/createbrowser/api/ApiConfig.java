package net.createbrowser.api;

import net.createbrowser.Constants;
import net.createbrowser.config.BrowserConfig;

/** Immutable snapshot of configuration values needed by the API layer. */
public final class ApiConfig {

    private final String apiKey;
    private final String modSecret;
    private final String modVersion;
    private final String mcVersion;
    private final int timeoutSec;
    private final int maxRetries;
    private final int maxReqPerSec;

    public ApiConfig(String apiKey, String modSecret, String modVersion, String mcVersion,
                     int timeoutSec, int maxRetries, int maxReqPerSec) {
        this.apiKey = apiKey;
        this.modSecret = modSecret;
        this.modVersion = modVersion;
        this.mcVersion = mcVersion;
        this.timeoutSec = timeoutSec;
        this.maxRetries = maxRetries;
        this.maxReqPerSec = maxReqPerSec;
    }

    public static ApiConfig fromBrowserConfig(BrowserConfig cfg) {
        return new ApiConfig(
            cfg.apiKey(),
            cfg.modSecret(),
            Constants.MOD_VERSION,
            Constants.MC_VERSION,
            cfg.timeoutSec(),
            cfg.maxRetries(),
            cfg.maxReqPerSec()
        );
    }

    public String apiKey() { return apiKey; }
    public String modSecret() { return modSecret; }
    public String modVersion() { return modVersion; }
    public String mcVersion() { return mcVersion; }
    public int timeoutSec() { return timeoutSec; }
    public int maxRetries() { return maxRetries; }
    public int maxReqPerSec() { return maxReqPerSec; }

    public String userAgent() {
        return "CreateBrowser/" + modVersion + " Minecraft/" + mcVersion;
    }
}
