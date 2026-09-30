package net.createbrowser.api;

import net.createbrowser.Constants;
import net.createbrowser.config.BrowserConfig;

/** Immutable snapshot of configuration values needed by the API layer; value-equal so callers can detect changes. */
public record ApiConfig(String apiKey, String modSecret, String modVersion, String mcVersion,
                        int timeoutSec, int maxRetries, int maxReqPerSec) {

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

    /** Redacts credentials so an accidental log line never leaks them. */
    @Override
    public String toString() {
        return "ApiConfig[" + userAgent() + ", timeoutSec=" + timeoutSec + "]";
    }

    public String userAgent() {
        return "CreateBrowser/" + modVersion + " Minecraft/" + mcVersion;
    }
}
