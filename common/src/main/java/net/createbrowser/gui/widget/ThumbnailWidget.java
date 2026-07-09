package net.createbrowser.gui.widget;

import com.mojang.blaze3d.platform.NativeImage;
import net.createbrowser.Constants;
import net.createbrowser.storage.CacheManager;
import net.createbrowser.util.AsyncExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders an image fetched from a remote URL. Loads asynchronously via the on-disk cache,
 * uploads the result as a DynamicTexture, and reuses one texture per URL across all widgets.
 */
public final class ThumbnailWidget {

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final Map<String, ResourceLocation> URL_TO_TEXTURE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> FAILED = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> IN_FLIGHT = new ConcurrentHashMap<>();

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final String url;
    private boolean kicked = false;

    public ThumbnailWidget(int x, int y, int width, int height, String url) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.url = url;
    }

    public void render(GuiGraphics graphics) {
        ResourceLocation tex = url != null ? URL_TO_TEXTURE.get(url) : null;
        if (tex != null) {
            graphics.blit(tex, x, y, 0, 0, width, height, width, height);
            return;
        }
        graphics.fill(x, y, x + width, y + height, 0x33888888);
        graphics.fill(x, y, x + width, y + 1, 0x55BBBBBB);
        graphics.fill(x, y + height - 1, x + width, y + height, 0x55444444);
        if (url != null && !kicked && !FAILED.containsKey(url)) {
            kicked = true;
            kickoffLoad(url);
        }
    }

    private static void kickoffLoad(String url) {
        if (IN_FLIGHT.putIfAbsent(url, Boolean.TRUE) != null) return;
        AsyncExecutor.run(
            () -> {
                Optional<Path> cached = CacheManager.get().getThumb(url);
                if (cached.isPresent()) return Files.readAllBytes(cached.get());
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
                HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() != 200) {
                    throw new RuntimeException("http." + resp.statusCode());
                }
                byte[] bytes = resp.body();
                CacheManager.get().putThumb(url, bytes);
                return bytes;
            },
            bytes -> {
                IN_FLIGHT.remove(url);
                try {
                    NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
                    DynamicTexture dyn = new DynamicTexture(img);
                    String id = "thumb/" + Integer.toHexString(url.hashCode() & 0x7fffffff);
                    ResourceLocation rl = new ResourceLocation(Constants.MOD_ID, id);
                    Minecraft.getInstance().getTextureManager().register(rl, dyn);
                    URL_TO_TEXTURE.put(url, rl);
                } catch (Exception e) {
                    Constants.LOG.warn("[CreateBrowser] Thumb decode failed for {}: {}", url, e.getMessage());
                    FAILED.put(url, Boolean.TRUE);
                }
            },
            err -> {
                IN_FLIGHT.remove(url);
                FAILED.put(url, Boolean.TRUE);
                Constants.LOG.debug("[CreateBrowser] Thumb load failed for {}: {}", url, err.getMessage());
            }
        );
    }

    /** Stateless draw helper — convenient for list rows whose position changes every render. */
    public static void draw(GuiGraphics graphics, int x, int y, int w, int h, String url) {
        ResourceLocation tex = url != null ? URL_TO_TEXTURE.get(url) : null;
        if (tex != null) {
            graphics.blit(tex, x, y, 0, 0, w, h, w, h);
            return;
        }
        graphics.fill(x, y, x + w, y + h, 0x33888888);
        graphics.fill(x, y, x + w, y + 1, 0x55BBBBBB);
        graphics.fill(x, y + h - 1, x + w, y + h, 0x55444444);
        if (url != null && !FAILED.containsKey(url)) {
            kickoffLoad(url);
        }
    }

    /** Releases all cached textures. Call when the user closes the browser. */
    public static void disposeAll() {
        Map<String, ResourceLocation> snapshot = new HashMap<>(URL_TO_TEXTURE);
        URL_TO_TEXTURE.clear();
        for (ResourceLocation rl : snapshot.values()) {
            try { Minecraft.getInstance().getTextureManager().release(rl); } catch (Exception ignore) {}
        }
    }
}
