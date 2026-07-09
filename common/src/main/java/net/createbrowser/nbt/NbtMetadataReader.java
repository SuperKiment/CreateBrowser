package net.createbrowser.nbt;

/** Minimal NBT file validator. Full metadata extraction (dimensions, palette) is Phase 2. */
public final class NbtMetadataReader {

    private NbtMetadataReader() {}

    /** Returns true if the bytes start with the GZip magic number 0x1F 0x8B. */
    public static boolean isValidGzipNbt(byte[] data) {
        return data != null && data.length >= 2
            && (data[0] & 0xFF) == 0x1F
            && (data[1] & 0xFF) == 0x8B;
    }
}
