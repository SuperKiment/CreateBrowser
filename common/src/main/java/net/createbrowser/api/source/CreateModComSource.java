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

/**
 * SchematicSource backed by the createmod.com public API.
 * Clients are rebuilt whenever the config changes, so keys edited in SettingsScreen apply without a restart.
 */
public final class CreateModComSource implements SchematicSource {

    private record Clients(ApiConfig config, ApiClient api, ModDownloadClient mod) {}

    private Clients clients;

    private synchronized Clients clients() {
        ApiConfig current = ApiConfig.fromBrowserConfig(Services.CONFIG);
        if (clients == null || !current.equals(clients.config())) {
            clients = new Clients(current, new ApiClient(current), new ModDownloadClient(current));
        }
        return clients;
    }

    @Override
    public String id() { return "createmod.com"; }

    @Override
    public String displayName() { return "createmod.com"; }

    @Override
    public SearchResult search(String query, int page, int pageSize) throws IOException, InterruptedException {
        return clients().api().searchSchematics(query, page, pageSize);
    }

    @Override
    public SearchResult search(String query, int page, int pageSize, SearchFilters filters)
            throws IOException, InterruptedException {
        return clients().api().searchSchematics(query, page, pageSize, filters);
    }

    @Override
    public SchematicDetail detail(String name) throws IOException, InterruptedException {
        return clients().api().getSchematicDetail(name);
    }

    /** Uses the API key endpoint; falls back to the HMAC mod endpoint only when just a mod secret is set. */
    @Override
    public byte[] download(String name) throws Exception {
        Clients c = clients();
        if (c.config().apiKey().isEmpty() && !c.config().modSecret().isEmpty()) {
            return c.mod().downloadSchematic(name);
        }
        return c.api().downloadSchematic(name);
    }

    @Override
    public boolean supportsDownload() { return true; }

    @Override
    public boolean supportsUpload() { return true; }

    @Override
    public UploadResult upload(UploadRequest request) throws IOException, InterruptedException {
        return clients().api().uploadSchematic(request);
    }
}
