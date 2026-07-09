package net.createbrowser.nbt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NbtMetadataReaderTest {

    @Test
    void acceptsValidGzipHeader() {
        byte[] data = {0x1F, (byte) 0x8B, 0x00, 0x00, 0x00};
        assertTrue(NbtMetadataReader.isValidGzipNbt(data));
    }

    @Test
    void rejectsWrongFirstByte() {
        byte[] data = {0x00, (byte) 0x8B, 0x00};
        assertFalse(NbtMetadataReader.isValidGzipNbt(data));
    }

    @Test
    void rejectsWrongSecondByte() {
        byte[] data = {0x1F, 0x00, 0x00};
        assertFalse(NbtMetadataReader.isValidGzipNbt(data));
    }

    @Test
    void rejectsOneByteBuffer() {
        assertFalse(NbtMetadataReader.isValidGzipNbt(new byte[]{0x1F}));
    }

    @Test
    void rejectsEmptyBuffer() {
        assertFalse(NbtMetadataReader.isValidGzipNbt(new byte[0]));
    }

    @Test
    void rejectsNullBuffer() {
        assertFalse(NbtMetadataReader.isValidGzipNbt(null));
    }

    @Test
    void acceptsExactlyTwoBytes() {
        byte[] data = {0x1F, (byte) 0x8B};
        assertTrue(NbtMetadataReader.isValidGzipNbt(data));
    }
}
