package net.createbrowser.api;

import com.google.gson.Gson;
import net.createbrowser.Constants;
import net.createbrowser.api.model.SchematicDetail;
import net.createbrowser.api.model.SearchFilters;
import net.createbrowser.api.model.SearchResult;
import net.createbrowser.api.model.UploadRequest;
import net.createbrowser.api.model.UploadResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;

/** Synchronous HTTP client for the createmod.com public API. Always call from a background thread. */
public final class ApiClient {

    private static final String BASE_URL = "https://createmod.com";
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    private final ApiConfig config;
    private final RateLimiter rateLimiter;

    public ApiClient(ApiConfig config) {
        this.config = config;
        this.rateLimiter = new RateLimiter(config.maxReqPerSec());
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(config.timeoutSec()))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    public SearchResult searchSchematics(String query, int page, int pageSize) throws IOException, InterruptedException {
        return searchSchematics(query, page, pageSize, SearchFilters.NONE);
    }

    public SearchResult searchSchematics(String query, int page, int pageSize, SearchFilters filters)
            throws IOException, InterruptedException {
        // Server-side params mirror the createmod.com OpenAPI spec (per_page, numeric sort).
        // Category is still filtered client-side: the API expects category keys, results expose names.
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        StringBuilder url = new StringBuilder(BASE_URL)
            .append("/api/schematics?query=").append(encoded)
            .append("&page=").append(page)
            .append("&per_page=").append(pageSize);
        if (filters != null && filters.sort() != null && filters.sort().apiValue() != null) {
            url.append("&sort=").append(filters.sort().apiValue());
        }
        String body = get(url.toString());
        return GSON.fromJson(body, SearchResult.class);
    }

    public SchematicDetail getSchematicDetail(String name) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String url = BASE_URL + "/api/schematics/" + encoded;
        String body = get(url);
        return GSON.fromJson(body, SchematicDetail.class);
    }

    /**
     * Downloads the raw .nbt bytes through GET /api/schematics/{name}/download (API key auth).
     * The endpoint answers 302 to the file, which the HttpClient follows.
     */
    public byte[] downloadSchematic(String name) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
        HttpRequest request = authorizedGet(BASE_URL + "/api/schematics/" + encoded + "/download");
        return executeWithRetry(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    /** Anonymous upload — no API key required. Only the .nbt file bytes are sent. */
    public UploadResult uploadSchematic(UploadRequest req) throws IOException, InterruptedException {
        String boundary = "----CreateBrowser" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = buildMultipartBody(req.file(), boundary);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/schematics/upload-anonymous"))
            .header("User-Agent", config.userAgent())
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();

        Constants.LOG.debug("POST upload-anonymous {} bytes", body.length);
        String responseBody = executeWithRetry(request, HttpResponse.BodyHandlers.ofString());
        return GSON.fromJson(responseBody, UploadResult.class);
    }

    private byte[] buildMultipartBody(java.nio.file.Path file, String boundary) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] crlf = "\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] dashes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);

        byte[] fileBytes = Files.readAllBytes(file);
        String fileName = file.getFileName().toString();
        out.write(dashes); out.write(crlf);
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"")
            .getBytes(StandardCharsets.UTF_8)); out.write(crlf);
        out.write("Content-Type: application/octet-stream".getBytes(StandardCharsets.UTF_8));
        out.write(crlf); out.write(crlf);
        out.write(fileBytes); out.write(crlf);
        out.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8)); out.write(crlf);
        return out.toByteArray();
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = authorizedGet(url);
        Constants.LOG.debug("GET {} (UA={}, key.len={})", url, config.userAgent(), config.apiKey().length());
        try {
            return executeWithRetry(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            Constants.LOG.error("HTTP request failed: {} — {}: {}", url, e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    private HttpRequest authorizedGet(String url) throws IOException {
        if (config.apiKey().isEmpty()) {
            throw new IOException("no_api_key");
        }
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("X-API-Key", config.apiKey())
            .header("User-Agent", config.userAgent())
            .timeout(Duration.ofSeconds(config.timeoutSec()))
            .GET()
            .build();
    }

    private <T> T executeWithRetry(HttpRequest request, HttpResponse.BodyHandler<T> handler)
            throws IOException, InterruptedException {
        IOException lastError = null;
        for (int attempt = 0; attempt <= config.maxRetries(); attempt++) {
            rateLimiter.acquire();
            HttpResponse<T> resp = httpClient.send(request, handler);
            int status = resp.statusCode();

            if (status == 200) return resp.body();
            if (status == 401) throw new IOException("http.401");
            if (status == 429) throw new IOException("http.429");

            if (status >= 500) {
                lastError = new IOException("http." + status);
                if (attempt < config.maxRetries()) {
                    Thread.sleep((long) Math.pow(2, attempt + 1) * 500L);
                }
            } else {
                String text = resp.body() instanceof byte[] b ? new String(b, StandardCharsets.UTF_8)
                    : String.valueOf(resp.body());
                String detail = resp.body() != null && !text.isBlank() ? " — " + text.strip() : "";
                Constants.LOG.warn("HTTP {} from {}{}", status, request.uri(), detail);
                throw new IOException("http." + status + detail);
            }
        }
        throw lastError != null ? lastError : new IOException("http.unknown");
    }
}
