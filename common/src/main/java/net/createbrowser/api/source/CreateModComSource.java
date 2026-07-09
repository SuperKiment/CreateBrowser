package net.createbrowser.api.source;

import net.createbrowser.api.ApiClient;
import net.createbrowser.api.ApiConfig;
import net.createbrowser.api.ModDownloadClient;
import net.createbrowser.api.model.SchematicDetail;
import net.createbrowser.api.model.SearchFilters;
import net.createbrowser.api.model.SearchResult;
import net.createbrowser.api.model.UploadRequest;
import net.createbrowser.api.model.UploadResult;
import net.createbrowser.platform.Services;

import java.io.IOException;

/** SchematicSource backed by the createmod.com public API. */
public final class CreateModComSource implements SchematicSource {

    private final ApiClient apiClient;
    private final ModDownloadClient downloadClient;

    public CreateModComSource() {
        ApiConfig config = ApiConfig.fromBrowserConfig(Services.CONFIG);
        this.apiClient = new ApiClient(config);
        this.downloadClient = new ModDownloadClient(config);
    }

    @Override
    public String id() { return "createmod.com"; }

    @Override
    public String displayName() { return "createmod.com"; }

    @Override
    public SearchResult search(String query, int page, int pageSize) throws IOException, InterruptedException {
        return apiClient.searchSchematics(query, page, pageSize);
    }

    @Override
    public SearchResult search(String query, int page, int pageSize, SearchFilters filters)
            throws IOException, InterruptedException {
        return apiClient.searchSchematics(query, page, pageSize, filters);
    }

    @Override
    public SchematicDetail detail(String name) throws IOException, InterruptedException {
        return apiClient.getSchematicDetail(name);
    }

    @Override
    public byte[] download(String name) throws Exception {
        return downloadClient.downloadSchematic(name);
    }

    @Override
    public boolean supportsDownload() { return true; }

    @Override
    public boolean supportsUpload() { return true; }

    @Override
    public UploadResult upload(UploadRequest request) throws IOException, InterruptedException {
        return apiClient.uploadSchematic(request);
    }
}
