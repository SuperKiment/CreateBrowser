package net.createbrowser.api;

import net.createbrowser.api.model.UploadRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the multipart/form-data encoder in ApiClient (file-only, anonymous upload). */
class MultipartEncoderTest {

    @TempDir
    Path tempDir;

    private byte[] buildBody(ApiClient client, Path file, String boundary) throws Exception {
        Method m = ApiClient.class.getDeclaredMethod("buildMultipartBody", Path.class, String.class);
        m.setAccessible(true);
        return (byte[]) m.invoke(client, file, boundary);
    }

    private ApiClient dummyClient() {
        ApiConfig cfg = new ApiConfig("dummy-key", "", "0.1.0", "1.20.1", 10, 2, 3);
        return new ApiClient(cfg);
    }

    @Test
    void bodyContainsBoundaryTerminator() throws Exception {
        Path file = tempDir.resolve("test.nbt");
        Files.write(file, new byte[]{0x1F, (byte) 0x8B, 0x00, 0x01});

        String boundary = "----TestBoundary123";
        byte[] body = buildBody(dummyClient(), file, boundary);
        String text = new String(body, StandardCharsets.UTF_8);

        assertTrue(text.contains("--" + boundary + "--"), "Body must end with closing boundary");
    }

    @Test
    void bodyContainsFilePartWithFilename() throws Exception {
        Path file = tempDir.resolve("myschematic.nbt");
        byte[] nbtData = {0x1F, (byte) 0x8B, 0x08, 0x00};
        Files.write(file, nbtData);

        String boundary = "----B";
        byte[] body = buildBody(dummyClient(), file, boundary);
        String text = new String(body, StandardCharsets.UTF_8);

        assertTrue(text.contains("filename=\"myschematic.nbt\""), "Must include filename in file part");
        assertTrue(text.contains("Content-Type: application/octet-stream"), "File part must declare content type");
        assertTrue(text.contains("name=\"file\""), "Field name must be 'file'");
    }

    @Test
    void bodyContainsActualFileBytes() throws Exception {
        Path file = tempDir.resolve("data.nbt");
        byte[] nbtData = {0x1F, (byte) 0x8B, 0x08, 0x00, 0x42, 0x43};
        Files.write(file, nbtData);

        String boundary = "----C";
        byte[] body = buildBody(dummyClient(), file, boundary);

        // The raw bytes should appear in the body
        boolean found = false;
        outer:
        for (int i = 0; i <= body.length - nbtData.length; i++) {
            for (int j = 0; j < nbtData.length; j++) {
                if (body[i + j] != nbtData[j]) continue outer;
            }
            found = true;
            break;
        }
        assertTrue(found, "NBT file bytes must appear verbatim in the multipart body");
    }

    @Test
    void noTextFieldsInBody() throws Exception {
        Path file = tempDir.resolve("clean.nbt");
        Files.write(file, new byte[]{0x1F, (byte) 0x8B});

        String boundary = "----D";
        byte[] body = buildBody(dummyClient(), file, boundary);
        String text = new String(body, StandardCharsets.UTF_8);

        assertFalse(text.contains("name=\"title\""), "Anonymous upload must not send title field");
        assertFalse(text.contains("name=\"description\""), "Anonymous upload must not send description field");
        assertFalse(text.contains("name=\"tags\""), "Anonymous upload must not send tags field");
    }

    @Test
    void boundaryAppearsInBothPartsAndTerminator() throws Exception {
        Path file = tempDir.resolve("multi.nbt");
        Files.write(file, new byte[]{0x1F, (byte) 0x8B});

        String boundary = "----UniqueXYZ";
        byte[] body = buildBody(dummyClient(), file, boundary);
        String text = new String(body, StandardCharsets.UTF_8);

        long count = text.lines()
            .filter(l -> l.startsWith("--" + boundary))
            .count();
        assertTrue(count >= 2, "Boundary must appear as opening and closing delimiter");
    }
}
