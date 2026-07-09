package net.createbrowser.api;

import com.google.gson.Gson;
import net.createbrowser.api.model.UploadRequest;
import net.createbrowser.api.model.UploadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})

class ApiClientUploadTest {

    @TempDir
    Path tempDir;

    private static final Gson GSON = new Gson();

    @SuppressWarnings("unchecked")
    private ApiClient clientWithMockHttp(HttpClient mockHttp) throws Exception {
        ApiConfig cfg = new ApiConfig("test-key", "", "0.1.0", "1.20.1", 10, 0, 10);
        ApiClient client = new ApiClient(cfg);
        Field f = ApiClient.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        f.set(client, mockHttp);
        return client;
    }

    @Test
    void uploadReturnsResultOnSuccess() throws Exception {
        Path file = tempDir.resolve("build.nbt");
        Files.write(file, new byte[]{0x1F, (byte) 0x8B, 0x00, 0x00});

        UploadResult expected = new UploadResult("tok123", "https://createmod.com/schematics/build");
        String json = GSON.toJson(expected);

        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);
        when(mockResp.statusCode()).thenReturn(200);
        when(mockResp.body()).thenReturn(json);
        doReturn(mockResp).when(mockHttp).send(any(HttpRequest.class), any());

        ApiClient client = clientWithMockHttp(mockHttp);
        UploadRequest req = new UploadRequest(file, "Build", "desc",
            List.of("automation"), List.of("windmill"), List.of("create"));

        UploadResult result = client.uploadSchematic(req);

        assertEquals("tok123", result.token());
        assertEquals("https://createmod.com/schematics/build", result.url());
    }

    @Test
    void uploadWorksWithoutApiKey() throws Exception {
        Path file = tempDir.resolve("b.nbt");
        Files.write(file, new byte[]{0x1F, (byte) 0x8B});

        // Anonymous upload — empty API key must NOT throw
        ApiConfig cfg = new ApiConfig("", "", "0.1.0", "1.20.1", 10, 0, 10);
        ApiClient client = new ApiClient(cfg);

        UploadResult stub = new UploadResult("tok", "https://createmod.com/u/tok");
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);
        when(mockResp.statusCode()).thenReturn(200);
        when(mockResp.body()).thenReturn(GSON.toJson(stub));
        doReturn(mockResp).when(mockHttp).send(any(HttpRequest.class), any());

        Field f = ApiClient.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        f.set(client, mockHttp);

        UploadRequest req = new UploadRequest(file, "T", "", List.of(), List.of(), List.of());
        UploadResult result = client.uploadSchematic(req);
        assertEquals("tok", result.token());
    }

    @Test
    void uploadSendsCorrectHeaders() throws Exception {
        Path file = tempDir.resolve("c.nbt");
        Files.write(file, new byte[]{0x1F, (byte) 0x8B});

        UploadResult stub = new UploadResult("t", "http://example.com");
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);
        when(mockResp.statusCode()).thenReturn(200);
        when(mockResp.body()).thenReturn(GSON.toJson(stub));
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        doAnswer(inv -> {
            captured.set(inv.getArgument(0));
            return mockResp;
        }).when(mockHttp).send(any(HttpRequest.class), any());

        ApiClient client = clientWithMockHttp(mockHttp);
        UploadRequest req = new UploadRequest(file, "Title", "", List.of(), List.of(), List.of());
        client.uploadSchematic(req);

        HttpRequest req2 = captured.get();
        assertNotNull(req2);
        assertEquals("POST", req2.method());
        assertTrue(req2.headers().firstValue("X-API-Key").isEmpty(),
            "Anonymous upload must NOT send X-API-Key");
        assertTrue(req2.headers().firstValue("Content-Type").orElse("").startsWith("multipart/form-data"),
            "Content-Type must be multipart/form-data");
        assertTrue(req2.uri().toString().contains("/api/schematics/upload-anonymous"),
            "URI must point to anonymous upload endpoint");
    }

    @Test
    void uploadThrowsOn401() throws Exception {
        Path file = tempDir.resolve("d.nbt");
        Files.write(file, new byte[]{0x1F, (byte) 0x8B});

        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);
        when(mockResp.statusCode()).thenReturn(401);
        when(mockResp.body()).thenReturn("Unauthorized");
        doReturn(mockResp).when(mockHttp).send(any(HttpRequest.class), any());

        ApiClient client = clientWithMockHttp(mockHttp);
        UploadRequest req = new UploadRequest(file, "T", "", List.of(), List.of(), List.of());

        IOException ex = assertThrows(IOException.class, () -> client.uploadSchematic(req));
        assertTrue(ex.getMessage().contains("401"));
    }
}
