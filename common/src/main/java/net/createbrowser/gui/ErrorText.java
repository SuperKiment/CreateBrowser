package net.createbrowser.gui;

import net.minecraft.network.chat.Component;

/** Maps raw exception messages from the API and storage layers to translated, user-facing error text. */
final class ErrorText {

    private ErrorText() {}

    static Component of(String raw) {
        if (raw == null) return key("unknown");
        if (raw.contains("no_mod_secret")) return key("no_mod_secret");
        if (raw.contains("no_api_key") || raw.contains("http.401")) return key("no_api_key");
        if (raw.contains("http.429")) return key("rate_limited");
        if (raw.contains("http.404")) return key("not_found");
        if (raw.contains("GZip header")) return key("invalid_file");
        if (raw.contains("http.")) return Component.literal(raw.substring(raw.indexOf("http.")));
        return key("network");
    }

    static Component key(String code) {
        return Component.translatable("createbrowser.error." + code);
    }
}
