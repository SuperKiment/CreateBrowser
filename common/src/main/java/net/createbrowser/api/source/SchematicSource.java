package net.createbrowser.api.source;

import net.createbrowser.api.model.SchematicDetail;
import net.createbrowser.api.model.SearchFilters;
import net.createbrowser.api.model.SearchResult;
import net.createbrowser.api.model.UploadRequest;
import net.createbrowser.api.model.UploadResult;

import java.io.IOException;

/** Abstraction for any repository that provides schematics (createmod.com, local, etc.). */
public interface SchematicSource {

    /** Short identifier, e.g. "createmod.com". */
    String id();

    /** Human-readable name shown in the UI. */
    String displayName();

    SearchResult search(String query, int page, int pageSize) throws IOException, InterruptedException;

    /** Filtered search; sources may override for native support. Default ignores filters. */
    default SearchResult search(String query, int page, int pageSize, SearchFilters filters)
            throws IOException, InterruptedException {
        return search(query, page, pageSize);
    }

    SchematicDetail detail(String name) throws IOException, InterruptedException;

    /**
     * Downloads the raw .nbt bytes for the given schematic name.
     * @throws IOException if the download fails or the secret is not configured
     */
    byte[] download(String name) throws Exception;

    boolean supportsDownload();

    /** Returns true if this source supports uploading schematics. */
    default boolean supportsUpload() { return false; }

    /**
     * Uploads a schematic to this source.
     * @throws UnsupportedOperationException if the source does not support uploads
     */
    default UploadResult upload(UploadRequest request) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("upload not supported by " + id());
    }
}
