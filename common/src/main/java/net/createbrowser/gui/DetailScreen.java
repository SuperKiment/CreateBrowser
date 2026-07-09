package net.createbrowser.gui;

import com.google.gson.Gson;
import net.createbrowser.api.model.SchematicDetail;
import net.createbrowser.api.model.SchematicEntry;
import net.createbrowser.api.source.SchematicSource;
import net.createbrowser.api.source.SourceRegistry;
import net.createbrowser.gui.widget.ThumbnailWidget;
import net.createbrowser.platform.Services;
import net.createbrowser.storage.CacheManager;
import net.createbrowser.storage.FavoritesStore;
import net.createbrowser.storage.HistoryStore;
import net.createbrowser.storage.SchematicFileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.stream.Collectors;

import net.createbrowser.util.AsyncExecutor;

/** Shows the full detail of a schematic and allows downloading it. */
public class DetailScreen extends Screen {

    private enum DownloadState { IDLE, DOWNLOADING, DONE, ERROR }

    private static final Gson GSON = new Gson();

    private final SchematicEntry entry;
    private final Screen parent;
    private final SchematicSource source;

    private SchematicDetail detail;
    private boolean detailLoading = true;
    private String detailError = null;

    private Button downloadButton;
    private Button backButton;
    private Button favoriteButton;
    private DownloadState downloadState = DownloadState.IDLE;
    private String downloadError = "";
    private boolean viewRecorded = false;

    public DetailScreen(SchematicEntry entry, Screen parent) {
        super(Component.translatable("createbrowser.screen.detail.title"));
        this.entry = entry;
        this.parent = parent;
        this.source = SourceRegistry.primary();
    }

    @Override
    protected void init() {
        int btnY = this.height - 28;

        backButton = Button.builder(
            Component.translatable("createbrowser.screen.detail.back"),
            b -> Minecraft.getInstance().setScreen(parent)
        ).bounds(this.width / 2 - 160, btnY, 80, 20).build();
        addRenderableWidget(backButton);

        downloadButton = Button.builder(
            downloadButtonLabel(),
            b -> startDownload()
        ).bounds(this.width / 2 - 60, btnY, 120, 20).build();
        addRenderableWidget(downloadButton);

        favoriteButton = Button.builder(
            favoriteLabel(false),
            b -> toggleFavorite()
        ).bounds(this.width / 2 + 80, btnY, 80, 20).build();
        addRenderableWidget(favoriteButton);

        addRenderableWidget(Button.builder(
            Component.translatable("createbrowser.screen.detail.share"),
            b -> shareLink()
        ).bounds(this.width / 2 + 80, btnY - 24, 80, 20).build());

        AsyncExecutor.run(
            () -> FavoritesStore.get().isFavorite(entry.name()),
            fav -> favoriteButton.setMessage(favoriteLabel(fav)),
            err -> { }
        );

        if (!viewRecorded) {
            viewRecorded = true;
            String displayTitle = entry.title() != null ? entry.title() : entry.name();
            AsyncExecutor.run(
                () -> {
                    HistoryStore.get().recordView(entry.name(), displayTitle, source.id());
                    return true;
                },
                r -> { },
                err -> net.createbrowser.Constants.LOG.warn("[CreateBrowser] recordView failed", err)
            );
        }

        loadDetail();
    }

    private void toggleFavorite() {
        favoriteButton.active = false;
        FavoritesStore.FavoriteEntry fe = new FavoritesStore.FavoriteEntry(
            entry.name(),
            entry.title() != null ? entry.title() : entry.name(),
            source.id(),
            System.currentTimeMillis()
        );
        AsyncExecutor.run(
            () -> FavoritesStore.get().toggle(fe),
            nowFav -> {
                favoriteButton.active = true;
                favoriteButton.setMessage(favoriteLabel(nowFav));
            },
            err -> {
                net.createbrowser.Constants.LOG.warn("[CreateBrowser] Favorite toggle failed", err);
                favoriteButton.active = true;
            }
        );
    }

    private void shareLink() {
        String url = "https://createmod.com/schematics/" + entry.name();
        Minecraft.getInstance().keyboardHandler.setClipboard(url);
        Services.CHAT.notify(Component.translatable("createbrowser.chat.share_copied"));
    }

    private static Component favoriteLabel(boolean isFav) {
        return Component.translatable(isFav
            ? "createbrowser.screen.detail.unfavorite"
            : "createbrowser.screen.detail.favorite");
    }

    private void loadDetail() {
        detailLoading = true;
        String cacheKey = "detail:" + source.id() + ":" + entry.name();
        AsyncExecutor.run(
            () -> {
                var cached = CacheManager.get().getApi(cacheKey, Services.CONFIG.detailTtlMin());
                if (cached.isPresent()) {
                    try {
                        SchematicDetail d = GSON.fromJson(cached.get(), SchematicDetail.class);
                        if (d != null) return d;
                    } catch (Exception ignore) { }
                }
                SchematicDetail fresh = source.detail(entry.name());
                try { CacheManager.get().putApi(cacheKey, GSON.toJson(fresh)); } catch (Exception ignore) { }
                return fresh;
            },
            result -> {
                this.detail = result;
                this.detailLoading = false;
            },
            err -> {
                net.createbrowser.Constants.LOG.error("[CreateBrowser] Detail load failed for name='{}': {} — {}",
                    entry.name(), err.getClass().getName(), err.getMessage(), err);
                this.detailError = err.getMessage();
                this.detailLoading = false;
            }
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = this.width / 2 - 180;
        int y = 15;
        int contentWidth = 360;

        if (Services.CONFIG.showThumbnails() && entry.images() != null && !entry.images().isEmpty()) {
            int thumbSize = 96;
            ThumbnailWidget.draw(graphics, x + contentWidth - thumbSize, y, thumbSize, thumbSize,
                entry.images().get(0));
        }

        // Title
        String title = entry.title() != null ? entry.title() : entry.name();
        graphics.drawString(this.font, title, x, y, 0xFFFFFF);
        y += 12;

        // Author
        if (entry.author() != null) {
            graphics.drawString(this.font, "by " + entry.author(), x, y, 0xAAAAAA);
        }
        y += 14;

        // Rating / downloads inline
        String meta = String.format("★ %.1f   ↓ %d", entry.rating(), entry.downloads());
        graphics.drawString(this.font, meta, x, y, 0xFFCC44);
        y += 16;

        graphics.fill(x, y, x + contentWidth, y + 1, 0x55FFFFFF);
        y += 6;

        if (detailLoading) {
            graphics.drawString(this.font,
                Component.translatable("createbrowser.screen.search.loading"),
                x, y, 0xCCCCCC);
            return;
        }

        if (detailError != null) {
            graphics.drawString(this.font,
                Component.translatable("createbrowser.screen.search.error", detailError),
                x, y, 0xFF5555);
            return;
        }

        if (detail == null) return;

        // Dimensions
        if (detail.dimensions() != null) {
            var d = detail.dimensions();
            graphics.drawString(this.font,
                Component.translatable("createbrowser.screen.detail.dimensions", d.x(), d.y(), d.z()),
                x, y, 0xCCCCCC);
            y += 11;
        }

        // Block count
        if (detail.blockCount() > 0) {
            graphics.drawString(this.font,
                Component.translatable("createbrowser.screen.detail.blocks", detail.blockCount()),
                x, y, 0xCCCCCC);
            y += 11;
        }

        y += 4;

        // Description (up to 4 lines)
        String desc = detail.description();
        if (!desc.isEmpty()) {
            var lines = this.font.split(Component.literal(desc), contentWidth);
            int maxLines = 4;
            for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
                graphics.drawString(this.font, lines.get(i), x, y, 0xDDDDDD);
                y += 10;
            }
            y += 4;
        }

        // Required mods
        if (!detail.mods().isEmpty()) {
            graphics.drawString(this.font,
                Component.translatable("createbrowser.screen.detail.mods_required"),
                x, y, 0xFFFFFF);
            y += 11;

            // Global compat summary banner
            long missingCount = detail.mods().stream()
                .filter(m -> !Services.PLATFORM.isModLoaded(m.id()))
                .count();
            if (missingCount == 0) {
                graphics.drawString(this.font,
                    Component.translatable("createbrowser.compat.all_mods_present"),
                    x + 8, y, 0x55FF55);
            } else {
                String missingList = detail.mods().stream()
                    .filter(m -> !Services.PLATFORM.isModLoaded(m.id()))
                    .map(m -> m.id())
                    .collect(java.util.stream.Collectors.joining(", "));
                graphics.drawString(this.font,
                    Component.translatable("createbrowser.compat.missing_mods", missingList),
                    x + 8, y, 0xFFAA00);
            }
            y += 12;

            for (var mod : detail.mods()) {
                boolean installed = Services.PLATFORM.isModLoaded(mod.id());
                String badge = installed ? "✓ " : "✗ ";
                int color = installed ? 0x55FF55 : 0xFF5555;
                String label = mod.hasVersion() ? mod.id() + " " + mod.version() : mod.id();
                graphics.drawString(this.font, badge + label, x + 8, y, color);
                y += 10;
            }
            y += 4;
        }

        // Download error
        if (downloadState == DownloadState.ERROR && !downloadError.isEmpty()) {
            graphics.drawString(this.font,
                Component.translatable("createbrowser.screen.detail.download_error"),
                x, this.height - 48, 0xFF5555);
        }

        if (downloadState == DownloadState.DONE) {
            graphics.drawString(this.font,
                Component.translatable("createbrowser.screen.detail.downloaded"),
                x, this.height - 48, 0x55FF55);
        }
    }

    private void startDownload() {
        if (downloadState == DownloadState.DOWNLOADING || downloadState == DownloadState.DONE) return;

        downloadState = DownloadState.DOWNLOADING;
        downloadButton.setMessage(Component.translatable("createbrowser.screen.detail.downloading"));
        downloadButton.active = false;

        String displayTitle = entry.title() != null ? entry.title() : entry.name();
        AsyncExecutor.run(
            () -> {
                byte[] data = source.download(entry.name());
                var path = SchematicFileManager.saveSchematic(entry.name(), data);
                HistoryStore.get().recordDownload(new HistoryStore.HistoryEntry(
                    entry.name(),
                    displayTitle,
                    source.id(),
                    System.currentTimeMillis(),
                    path != null ? path.toString() : ""
                ));
                return path;
            },
            path -> {
                downloadState = DownloadState.DONE;
                downloadButton.setMessage(Component.translatable("createbrowser.screen.detail.downloaded"));
                Services.CHAT.notify(
                    Component.translatable("createbrowser.chat.downloaded", displayTitle));
            },
            err -> {
                net.createbrowser.Constants.LOG.error("[CreateBrowser] Download failed for name='{}': {} — {}",
                    entry.name(), err.getClass().getName(), err.getMessage(), err);
                downloadState = DownloadState.ERROR;
                downloadError = friendlyDownloadError(err.getMessage());
                downloadButton.setMessage(Component.translatable("createbrowser.screen.detail.download_error"));
                downloadButton.active = true;
            }
        );
    }

    private Component downloadButtonLabel() {
        return switch (downloadState) {
            case DOWNLOADING -> Component.translatable("createbrowser.screen.detail.downloading");
            case DONE -> Component.translatable("createbrowser.screen.detail.downloaded");
            case ERROR -> Component.translatable("createbrowser.screen.detail.download_error");
            default -> Component.translatable("createbrowser.screen.detail.download");
        };
    }

    private static String friendlyDownloadError(String raw) {
        if (raw == null) return "unknown";
        if (raw.contains("no_mod_secret")) return "no_mod_secret";
        if (raw.contains("no_api_key") || raw.contains("401")) return "no_api_key";
        return "network";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        if (!(parent instanceof BrowserScreen)) {
            ThumbnailWidget.disposeAll();
        }
    }
}
