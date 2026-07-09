package net.createbrowser.api;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ModDownloadClientTest {

    @Test
    void xorIsItsOwnInverse() throws Exception {
        byte[] data = "Hello, CreateBrowser!".getBytes(StandardCharsets.UTF_8);
        byte[] key = ModDownloadClient.deriveXORKey("test-secret", 1710500000L);
        byte[] encoded = ModDownloadClient.xorEncode(data, key);
        byte[] decoded = ModDownloadClient.xorEncode(encoded, key);
        assertArrayEquals(data, decoded, "XOR encode then decode should recover original data");
    }

    @Test
    void deriveXORKeyProduces32Bytes() throws Exception {
        byte[] key = ModDownloadClient.deriveXORKey("secret", 1000L);
        assertEquals(32, key.length, "SHA-256 output should be 32 bytes");
    }

    @Test
    void differentTimestampsProduceDifferentKeys() throws Exception {
        byte[] key1 = ModDownloadClient.deriveXORKey("secret", 1000L);
        byte[] key2 = ModDownloadClient.deriveXORKey("secret", 1001L);
        assertFalse(java.util.Arrays.equals(key1, key2),
            "Different timestamps must produce different XOR keys");
    }

    @Test
    void differentSecretsProduceDifferentKeys() throws Exception {
        byte[] key1 = ModDownloadClient.deriveXORKey("secretA", 1000L);
        byte[] key2 = ModDownloadClient.deriveXORKey("secretB", 1000L);
        assertFalse(java.util.Arrays.equals(key1, key2),
            "Different secrets must produce different XOR keys");
    }

    @Test
    void xorEncodeWithEmptyKeyReturnsDataUnchanged() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        byte[] result = ModDownloadClient.xorEncode(data, new byte[0]);
        assertArrayEquals(data, result);
    }

    @Test
    void xorEncodeWithSingleByteKey() {
        byte[] data = {0x01, 0x02, 0x03};
        byte[] key = {(byte) 0xFF};
        byte[] expected = {(byte) 0xFE, (byte) 0xFD, (byte) 0xFC};
        assertArrayEquals(expected, ModDownloadClient.xorEncode(data, key));
    }

    @Test
    void keyIsDeterministic() throws Exception {
        byte[] key1 = ModDownloadClient.deriveXORKey("my-secret", 1710500000L);
        byte[] key2 = ModDownloadClient.deriveXORKey("my-secret", 1710500000L);
        assertArrayEquals(key1, key2, "Same inputs must always produce the same key");
    }
}
