package net.createbrowser.api;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Downloads .nbt files from createmod.com via the POST /api/mod/download endpoint.
 * Uses HMAC-SHA256 authentication and XOR-v1 response decoding.
 * Requires a mod_download_secret obtained from the createmod.com maintainer.
 */
public final class ModDownloadClient {

    private static final String DOWNLOAD_URL = "https://createmod.com/api/mod/download";

    private final ApiConfig config;
    private final HttpClient httpClient;
    private final Supplier<String> usernameSupplier;

    public ModDownloadClient(ApiConfig config) {
        this(config, () -> Minecraft.getInstance().getUser().getName());
    }

    /** Package-private constructor for testing without a live Minecraft instance. */
    ModDownloadClient(ApiConfig config, Supplier<String> usernameSupplier) {
        this.config = config;
        this.usernameSupplier = usernameSupplier;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(config.timeoutSec()))
            .build();
    }

    /**
     * Downloads and XOR-decodes the .nbt bytes for the given schematic name.
     * Must be called from a background thread (never the game thread).
     */
    public byte[] downloadSchematic(String name) throws Exception {
        if (config.modSecret() == null || config.modSecret().isEmpty()) {
            throw new IllegalStateException("no_mod_secret");
        }

        long timestamp = System.currentTimeMillis() / 1000L;
        String username = usernameSupplier.get();
        String message = timestamp + ":" + config.modVersion() + ":" + username + ":" + name;
        String signature = computeHmac(message, config.modSecret());

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("message", message);
        bodyJson.addProperty("signature", signature);
        bodyJson.addProperty("type", "schematic");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(DOWNLOAD_URL))
            .header("Content-Type", "application/json")
            .header("User-Agent", config.userAgent())
            .timeout(Duration.ofSeconds(config.timeoutSec()))
            .POST(HttpRequest.BodyPublishers.ofString(bodyJson.toString()))
            .build();

        HttpResponse<byte[]> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (resp.statusCode() != 200) {
            throw new IOException("Download failed: HTTP " + resp.statusCode());
        }

        String encoding = resp.headers().firstValue("X-Mod-Encoding").orElse("");
        if (!"xor-v1".equals(encoding)) {
            throw new IOException("Unexpected encoding: " + encoding);
        }

        byte[] xorKey = deriveXORKey(config.modSecret(), timestamp);
        return xorEncode(resp.body(), xorKey);
    }

    private static String computeHmac(String message, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(sig.length * 2);
        for (byte b : sig) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /** SHA-256(secret bytes || timestamp decimal string bytes) → 32-byte XOR key. */
    static byte[] deriveXORKey(String secret, long timestamp) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(secret.getBytes(StandardCharsets.UTF_8));
        md.update(Long.toString(timestamp).getBytes(StandardCharsets.UTF_8));
        return md.digest();
    }

    /** Cyclic XOR — symmetric: encode and decode are the same operation. */
    static byte[] xorEncode(byte[] data, byte[] key) {
        if (key.length == 0) return data;
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return out;
    }
}
