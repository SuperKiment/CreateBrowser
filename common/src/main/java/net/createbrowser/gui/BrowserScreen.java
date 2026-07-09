package net.createbrowser.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.createbrowser.api.model.SchematicEntry;
import net.createbrowser.api.model.SearchFilters;
import net.createbrowser.api.model.SearchResult;
import net.createbrowser.api.source.CreateModComSource;
import net.createbrowser.api.source.LocalSource;
import net.createbrowser.api.source.SchematicSource;
import net.createbrowser.gui.widget.FilterDropdown;
import net.createbrowser.gui.widget.PaginationWidget;
import net.createbrowser.gui.widget.SchematicListWidget;
import net.createbrowser.gui.widget.SearchBarWidget;
import net.createbrowser.gui.widget.TabBar;
import net.createbrowser.gui.widget.ThumbnailWidget;
import net.createbrowser.platform.Services;
import net.createbrowser.storage.CacheManager;
import net.createbrowser.storage.FavoritesStore;
import net.createbrowser.storage.HistoryStore;
import net.createbrowser.storage.LocalFile;
import net.createbrowser.storage.SchematicFileManager;
import net.minecraft.Util;
import net.createbrowser.util.AsyncExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Main schematic browser screen with tabs (search/favorites/history/local) and filters. */
public class BrowserScreen extends Screen {

    private enum State { IDLE, LOADING, RESULTS, EMPTY, ERROR }
    public enum Tab { SEARCH, FAVORITES, HISTORY, LOCAL }

    private static final int TAB_BAR_TOP = 24;
    private static final int TAB_BAR_HEIGHT = 18;
    private static final int LIST_TOP = 100;
    private static final int LIST_BOTTOM_MARGIN = 32;
    private static final int PAGINATION_HEIGHT = 28;
    private static final int ITEM_HEIGHT = 36;
    private static final Gson GSON = new Gson();

    private final SchematicSource source;
    private final Component createBadge;
    private final LocalSource localSource = new LocalSource();
    private final Map<String, LocalFile> localFilesByName = new HashMap<>();

    private SearchBarWidget searchBar;
    private SchematicListWidget listWidget;
    private PaginationWidget pagination;
    private Button searchButton;
    private TabBar<Tab> tabBar;
    private FilterDropdown<SearchFilters.SortMode> sortDropdown;
    private Button openFolderButton;

    private State state = State.IDLE;
    private Tab activeTab = Tab.SEARCH;
    private String errorMessage = "";
    private List<SchematicEntry> currentResults = List.of();
    private int currentPage = 1;
    private int totalPages = 1;
    private boolean hasPrev = false;
    private boolean hasNext = false;
    private String currentQuery = "";
    private SearchFilters.SortMode currentSort = SearchFilters.SortMode.RECENT;
    private List<SchematicEntry> rawResults = List.of();

    public BrowserScreen() {
        super(Component.translatable("createbrowser.screen.title"));
        this.source = new CreateModComSource();
        this.createBadge = Services.CREATE.isCreateLoaded()
            ? Component.translatable("createbrowser.screen.create_version",
                Services.CREATE.getCreateVersion() != null ? Services.CREATE.getCreateVersion() : "?")
            : null;
    }

    @Override
    protected void init() {
        int tabBarWidth = Math.min(560, this.width - 40);
        int tabBarX = (this.width - tabBarWidth) / 2;
        tabBar = new TabBar<>(tabBarX, TAB_BAR_TOP, tabBarWidth, TAB_BAR_HEIGHT,
            List.of(
                new TabBar.Tab<>(Tab.SEARCH, Component.translatable("createbrowser.screen.tabs.search")),
                new TabBar.Tab<>(Tab.FAVORITES, Component.translatable("createbrowser.screen.tabs.favorites")),
                new TabBar.Tab<>(Tab.HISTORY, Component.translatable("createbrowser.screen.tabs.history")),
                new TabBar.Tab<>(Tab.LOCAL, Component.translatable("createbrowser.screen.tabs.local"))
            ),
            activeTab,
            this::switchTab
        );

        int searchX = this.width / 2 - 190;
        int searchY = 50;
        searchBar = new SearchBarWidget(this.font, searchX, searchY, 340, 20,
            q -> performSearch(q, 1));
        addRenderableWidget(searchBar.editBox);

        searchButton = Button.builder(
            Component.translatable("createbrowser.screen.search.button"),
            b -> searchBar.triggerSearch()
        ).bounds(searchX + 344, searchY, 36, 20).build();
        addRenderableWidget(searchButton);

        int filtersY = searchY + 24;
        sortDropdown = new FilterDropdown<>(
            searchX, filtersY, 160, 18,
            List.of(SearchFilters.SortMode.RECENT, SearchFilters.SortMode.DOWNLOADS,
                SearchFilters.SortMode.RATING, SearchFilters.SortMode.VIEWS),
            currentSort,
            sortLabel(),
            v -> {
                currentSort = v;
                applyClientSort();
                if (listWidget != null) listWidget.setEntries(currentResults);
            }
        );

        openFolderButton = Button.builder(
            Component.translatable("createbrowser.screen.local.open_folder"),
            b -> openSchematicsFolder()
        ).bounds(searchX, filtersY, 160, 18).build();
        addRenderableWidget(openFolderButton);

        int listBottom = this.height - LIST_BOTTOM_MARGIN - PAGINATION_HEIGHT;
        listWidget = new SchematicListWidget(
            this.minecraft, this.width, this.height,
            LIST_TOP, listBottom, ITEM_HEIGHT,
            this::openDetail
        );
        listWidget.setEntries(currentResults);
        addRenderableWidget(listWidget);

        int paginationY = this.height - PAGINATION_HEIGHT - 6;
        int paginationWidth = Math.min(300, this.width - 40);
        int paginationX = this.width / 2 - paginationWidth / 2;
        pagination = new PaginationWidget(paginationX, paginationY, paginationWidth,
            page -> performSearch(currentQuery, page));
        addRenderableWidget(pagination.prevButton);
        addRenderableWidget(pagination.nextButton);

        applyTabUiVisibility();
        if (activeTab != Tab.SEARCH) {
            loadTabData(activeTab);
        }
        pagination.update(currentPage, totalPages, hasPrev, hasNext);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        if (createBadge != null) {
            graphics.drawString(this.font, createBadge, 6, this.height - 12, 0x55CC77);
        }
        if (tabBar != null) tabBar.render(graphics, mouseX, mouseY);

        if (activeTab == Tab.SEARCH) {
            sortDropdown.render(graphics, mouseX, mouseY);
        }

        int listCenterX = this.width / 2;
        int listCenterY = (LIST_TOP + (this.height - LIST_BOTTOM_MARGIN - PAGINATION_HEIGHT)) / 2;

        switch (state) {
            case IDLE -> graphics.drawCenteredString(this.font,
                idleHint(), listCenterX, listCenterY, 0x888888);
            case LOADING -> graphics.drawCenteredString(this.font,
                Component.translatable("createbrowser.screen.search.loading"),
                listCenterX, listCenterY, 0xCCCCCC);
            case EMPTY -> graphics.drawCenteredString(this.font,
                emptyMessage(), listCenterX, listCenterY, 0xAAAAAA);
            case ERROR -> graphics.drawCenteredString(this.font,
                Component.translatable("createbrowser.screen.search.error", errorMessage),
                listCenterX, listCenterY, 0xFF5555);
            default -> {}
        }

        if (state == State.RESULTS && activeTab == Tab.SEARCH) {
            pagination.renderLabel(graphics, this.font);
        }

        if (activeTab == Tab.SEARCH) {
            sortDropdown.renderOverlay(graphics, mouseX, mouseY);
        }
    }

    @Override
    public void tick() {
        if (searchBar != null) searchBar.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tabBar != null && tabBar.mouseClicked(mouseX, mouseY, button)) return true;
        if (activeTab == Tab.SEARCH) {
            if (sortDropdown.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (sortDropdown != null && sortDropdown.isOpen()) { sortDropdown.close(); return true; }
        }
        if (keyCode == 257 || keyCode == 335) {
            if (searchBar.editBox.isFocused()) {
                searchBar.triggerSearch();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        ThumbnailWidget.disposeAll();
    }

    private void switchTab(Tab tab) {
        this.activeTab = tab;
        applyTabUiVisibility();
        loadTabData(tab);
    }

    private void applyTabUiVisibility() {
        boolean isSearch = activeTab == Tab.SEARCH;
        boolean isLocal = activeTab == Tab.LOCAL;
        if (searchBar != null) {
            searchBar.editBox.visible = isSearch;
            searchBar.editBox.active = isSearch;
        }
        if (searchButton != null) searchButton.visible = isSearch;
        if (openFolderButton != null) openFolderButton.visible = isLocal;
        if (pagination != null) {
            pagination.prevButton.visible = isSearch && state == State.RESULTS;
            pagination.nextButton.visible = isSearch && state == State.RESULTS;
        }
    }

    private void openSchematicsFolder() {
        try {
            java.nio.file.Path dir = SchematicFileManager.schematicsDir();
            Util.getPlatform().openFile(dir.toFile());
        } catch (Exception e) {
            net.createbrowser.Constants.LOG.error("[CreateBrowser] Cannot open schematics folder", e);
        }
    }

    private void applyClientSort() {
        if (rawResults.isEmpty()) return;
        List<SchematicEntry> sorted = new ArrayList<>(rawResults);
        Comparator<SchematicEntry> cmp = switch (currentSort) {
            case DOWNLOADS -> Comparator.comparingInt(SchematicEntry::downloads).reversed();
            case RATING -> Comparator.comparingDouble((SchematicEntry e) -> e.rating()).reversed();
            case VIEWS -> Comparator.comparingInt(SchematicEntry::views).reversed();
            case RECENT -> null;
        };
        if (cmp != null) sorted.sort(cmp);
        currentResults = sorted;
    }

    private void loadTabData(Tab tab) {
        switch (tab) {
            case SEARCH -> {
                if (currentQuery.isBlank()) {
                    currentResults = List.of();
                    setState(State.IDLE);
                } else {
                    setState(State.RESULTS);
                }
                if (listWidget != null) listWidget.setEntries(currentResults);
            }
            case FAVORITES -> {
                List<SchematicEntry> fav = adaptFavorites(FavoritesStore.get().list());
                currentResults = fav;
                if (listWidget != null) listWidget.setEntries(fav);
                setState(fav.isEmpty() ? State.EMPTY : State.RESULTS);
            }
            case HISTORY -> {
                List<SchematicEntry> hist = adaptHistory(HistoryStore.get().list());
                currentResults = hist;
                if (listWidget != null) listWidget.setEntries(hist);
                setState(hist.isEmpty() ? State.EMPTY : State.RESULTS);
            }
            case LOCAL -> loadLocalTab();
        }
    }

    private void performSearch(String query, int page) {
        if (query.isBlank()) return;
        this.currentQuery = query;
        this.currentPage = page;
        this.activeTab = Tab.SEARCH;
        if (tabBar != null) tabBar.setActive(Tab.SEARCH);
        applyTabUiVisibility();

        SearchFilters filters = new SearchFilters(null, null, currentSort);
        String cacheKey = cacheKey(query, page, filters);

        setState(State.LOADING);
        int pageSize = Services.CONFIG.pageSize();

        AsyncExecutor.run(
            () -> {
                var cached = CacheManager.get().getApi(cacheKey, Services.CONFIG.searchTtlMin());
                if (cached.isPresent()) {
                    try {
                        SearchResult r = GSON.fromJson(cached.get(), SearchResult.class);
                        if (r != null) return r;
                    } catch (Exception ignore) { }
                }
                SearchResult fresh = source.search(query, page, pageSize, filters);
                try { CacheManager.get().putApi(cacheKey, GSON.toJson(fresh)); } catch (Exception ignore) { }
                return fresh;
            },
            this::onSearchResults,
            this::onSearchError
        );
    }

    private void onSearchResults(SearchResult result) {
        if (result.items().isEmpty()) {
            rawResults = List.of();
            currentResults = List.of();
            setState(State.EMPTY);
        } else {
            rawResults = result.items();
            applyClientSort();
            totalPages = result.totalPages();
            hasPrev = result.hasPrev();
            hasNext = result.hasNext();
            setState(State.RESULTS);
        }
        if (listWidget != null) listWidget.setEntries(currentResults);
        if (pagination != null) pagination.update(currentPage, totalPages, hasPrev, hasNext);
    }

    private void onSearchError(Throwable ex) {
        net.createbrowser.Constants.LOG.error("[CreateBrowser] Search failed for query='{}' page={}: {} — {}",
            currentQuery, currentPage, ex.getClass().getName(), ex.getMessage(), ex);
        errorMessage = friendlyError(ex.getMessage());
        setState(State.ERROR);
    }

    private void openDetail(SchematicEntry entry) {
        if (activeTab == Tab.LOCAL) {
            LocalFile lf = localFilesByName.get(entry.name());
            if (lf != null) {
                Minecraft.getInstance().setScreen(new LocalManagerScreen(lf, this));
                return;
            }
        }
        Minecraft.getInstance().setScreen(new DetailScreen(entry, this));
    }

    private void loadLocalTab() {
        setState(State.LOADING);
        AsyncExecutor.run(
            localSource::listAll,
            files -> {
                localFilesByName.clear();
                List<SchematicEntry> mapped = new ArrayList<>(files.size());
                for (LocalFile f : files) {
                    localFilesByName.put(f.displayName(), f);
                    mapped.add(buildLocalEntry(f.displayName(), f.displayName()));
                }
                currentResults = mapped;
                if (listWidget != null) listWidget.setEntries(mapped);
                setState(mapped.isEmpty() ? State.EMPTY : State.RESULTS);
            },
            err -> {
                net.createbrowser.Constants.LOG.error("[CreateBrowser] Local tab load failed", err);
                errorMessage = "local";
                setState(State.ERROR);
            }
        );
    }

    private void setState(State newState) {
        this.state = newState;
        boolean showList = newState == State.RESULTS;
        if (listWidget != null) listWidget.visible = showList;
        if (pagination != null) {
            boolean showPagination = showList && activeTab == Tab.SEARCH;
            pagination.prevButton.visible = showPagination;
            pagination.nextButton.visible = showPagination;
            if (!showPagination) {
                pagination.prevButton.active = false;
                pagination.nextButton.active = false;
            }
        }
    }

    private Component idleHint() {
        return switch (activeTab) {
            case SEARCH -> Component.translatable("createbrowser.screen.search.hint");
            case FAVORITES -> Component.translatable("createbrowser.screen.favorites.empty");
            case HISTORY -> Component.translatable("createbrowser.screen.history.empty");
            case LOCAL -> Component.translatable("createbrowser.screen.local.empty");
        };
    }

    private Component emptyMessage() {
        return switch (activeTab) {
            case SEARCH -> Component.translatable("createbrowser.screen.search.no_results");
            case FAVORITES -> Component.translatable("createbrowser.screen.favorites.empty");
            case HISTORY -> Component.translatable("createbrowser.screen.history.empty");
            case LOCAL -> Component.translatable("createbrowser.screen.local.empty");
        };
    }

    private static String friendlyError(String rawMsg) {
        if (rawMsg == null) return "unknown";
        if (rawMsg.contains("no_api_key") || rawMsg.contains("http.401")) return "no_api_key";
        if (rawMsg.contains("http.429")) return "rate_limited";
        if (rawMsg.contains("http.")) return rawMsg;
        return "network";
    }

    private static String cacheKey(String query, int page, SearchFilters f) {
        return "search:" + query + "|p=" + page
            + "|sort=" + (f.sort() != null ? f.sort().name() : "-")
            + "|size=" + (f.size() != null ? f.size().name() : "-")
            + "|cat=" + (f.category() != null ? f.category() : "-");
    }

    private static List<SchematicEntry> adaptFavorites(List<FavoritesStore.FavoriteEntry> list) {
        List<SchematicEntry> result = new ArrayList<>(list.size());
        for (FavoritesStore.FavoriteEntry f : list) {
            result.add(buildLocalEntry(f.name(), f.title()));
        }
        return result;
    }

    private static List<SchematicEntry> adaptHistory(List<HistoryStore.HistoryEntry> list) {
        List<SchematicEntry> result = new ArrayList<>(list.size());
        for (HistoryStore.HistoryEntry h : list) {
            result.add(buildLocalEntry(h.name(), h.title()));
        }
        return result;
    }

    /** Builds a synthetic SchematicEntry by deserializing minimal JSON (no public ctor on the model). */
    private static SchematicEntry buildLocalEntry(String name, String title) {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("title", title != null ? title : name);
        return GSON.fromJson(o, SchematicEntry.class);
    }

    private static java.util.function.Function<SearchFilters.SortMode, Component> sortLabel() {
        return s -> switch (s) {
            case RECENT -> Component.translatable("createbrowser.filter.sort.recent");
            case DOWNLOADS -> Component.translatable("createbrowser.filter.sort.downloads");
            case RATING -> Component.translatable("createbrowser.filter.sort.rating");
            case VIEWS -> Component.translatable("createbrowser.filter.sort.views");
        };
    }

}
