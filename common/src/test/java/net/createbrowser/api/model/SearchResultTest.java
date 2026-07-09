package net.createbrowser.api.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchResultTest {

    private static final String FIXTURE = """
        {
          "items": [
            {
              "name": "windmill-farm",
              "title": "Windmill Farm",
              "author": "builder123",
              "views": 1500,
              "downloads": 320,
              "rating": 4.5,
              "images": ["https://example.com/img.png"],
              "categories": ["automation"],
              "tags": ["windmill", "farm"]
            }
          ],
          "page": 1,
          "pageSize": 24,
          "hasPrev": false,
          "hasNext": true,
          "total": 48,
          "term": "windmill"
        }
        """;

    private static final String EMPTY_FIXTURE = """
        {
          "items": [],
          "page": 1,
          "pageSize": 24,
          "hasPrev": false,
          "hasNext": false,
          "total": 0,
          "term": "xyz"
        }
        """;

    private static final String NULL_FIELDS_FIXTURE = """
        {
          "items": [
            {
              "name": "test-schematic",
              "title": null,
              "author": null,
              "views": 0,
              "downloads": 0,
              "rating": 0.0,
              "images": null,
              "categories": null,
              "tags": null
            }
          ],
          "page": 2,
          "pageSize": 12,
          "hasPrev": true,
          "hasNext": false,
          "total": 15,
          "term": "test"
        }
        """;

    private final Gson gson = new Gson();

    @Test
    void deserializesFullResult() {
        SearchResult result = gson.fromJson(FIXTURE, SearchResult.class);
        assertEquals(1, result.page());
        assertEquals(24, result.pageSize());
        assertFalse(result.hasPrev());
        assertTrue(result.hasNext());
        assertEquals(48, result.total());
        assertEquals("windmill", result.term());
        assertEquals(1, result.items().size());
    }

    @Test
    void deserializesSchematicEntry() {
        SearchResult result = gson.fromJson(FIXTURE, SearchResult.class);
        SchematicEntry entry = result.items().get(0);
        assertEquals("windmill-farm", entry.name());
        assertEquals("Windmill Farm", entry.title());
        assertEquals("builder123", entry.author());
        assertEquals(1500, entry.views());
        assertEquals(320, entry.downloads());
        assertEquals(4.5f, entry.rating(), 0.01f);
        assertEquals(1, entry.images().size());
        assertEquals(1, entry.categories().size());
        assertEquals(2, entry.tags().size());
    }

    @Test
    void totalPagesCalculation() {
        SearchResult result = gson.fromJson(FIXTURE, SearchResult.class);
        assertEquals(2, result.totalPages()); // 48 total / 24 per page = 2 pages
    }

    @Test
    void handlesEmptyItems() {
        SearchResult result = gson.fromJson(EMPTY_FIXTURE, SearchResult.class);
        assertTrue(result.items().isEmpty());
        assertEquals(0, result.total());
        assertEquals(1, result.totalPages()); // min 1 page
    }

    @Test
    void handlesNullFieldsGracefully() {
        SearchResult result = gson.fromJson(NULL_FIELDS_FIXTURE, SearchResult.class);
        SchematicEntry entry = result.items().get(0);
        assertEquals("test-schematic", entry.name());
        assertNull(entry.title());
        assertNull(entry.author());
        assertTrue(entry.images().isEmpty());
        assertTrue(entry.categories().isEmpty());
        assertTrue(entry.tags().isEmpty());
        assertTrue(result.hasPrev());
        assertFalse(result.hasNext());
        assertEquals(2, result.totalPages()); // ceil(15/12) = 2
    }
}
