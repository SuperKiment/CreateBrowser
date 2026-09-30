package net.createbrowser.api.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parses payloads shaped like createmod.com's models.Schematic (source: uberswe/createmod.com internal/models). */
class RealApiShapeTest {

    private static final String SCHEMATIC = """
        {
          "id": "abc123",
          "name": "windmill-farm",
          "title": "Windmill Farm",
          "author": {"id": "u1", "username": "builder123", "avatar": "", "hasAvatar": false},
          "content": "A beautiful windmill farm.",
          "excerpt": "Windmill",
          "featuredImage": "cover image.png",
          "gallery": ["cover image.png", "side.png"],
          "categories": [{"id": "c1", "key": "farms", "name": "Farms"}],
          "tags": [{"id": "t1", "key": "windmill", "name": "Windmill"}],
          "views": 1500,
          "downloads": 320,
          "rating": "4.5",
          "blockCount": 450,
          "dimX": 15, "dimY": 20, "dimZ": 16,
          "materials": "[{\\"block_id\\":\\"create:shaft\\",\\"count\\":12}]",
          "mods": ["create"]
        }
        """;

    private static final String LIST = "{\"items\": [" + SCHEMATIC + "], \"page\": 1, \"pageSize\": 24,"
        + " \"hasPrev\": false, \"hasNext\": false, \"total\": 1, \"totalPages\": 1}";

    private final Gson gson = new Gson();

    @Test
    void parsesListEntry() {
        SchematicEntry e = gson.fromJson(LIST, SearchResult.class).items().get(0);
        assertEquals("builder123", e.author());
        assertEquals(4.5f, e.rating(), 0.01f);
        assertEquals(450, e.blockCount());
        assertEquals(List.of("Farms"), e.categories());
        assertEquals(List.of(
            "https://createmod.com/api/files/schematics/abc123/cover%20image.png",
            "https://createmod.com/api/files/schematics/abc123/side.png"), e.images());
    }

    @Test
    void parsesDetail() {
        SchematicDetail d = gson.fromJson(SCHEMATIC, SchematicDetail.class);
        assertEquals("A beautiful windmill farm.", d.description());
        assertNotNull(d.dimensions());
        assertEquals(15, d.dimensions().x());
        assertEquals(16, d.dimensions().z());
        assertEquals(450, d.blockCount());
        assertEquals(1, d.materials().size());
        assertEquals("create:shaft", d.materials().get(0).name());
        assertEquals("create", d.mods().get(0).id());
    }

    @Test
    void unknownDimensionsAreNull() {
        SchematicDetail d = gson.fromJson("{\"name\": \"x\"}", SchematicDetail.class);
        assertEquals(null, d.dimensions());
        assertTrue(d.images().isEmpty());
    }

    @Test
    void sizeBucketsIgnoreUnknownCounts() {
        assertTrue(SearchFilters.SizeBucket.ANY.matches(0));
        assertTrue(!SearchFilters.SizeBucket.SMALL.matches(0));
        assertTrue(SearchFilters.SizeBucket.MEDIUM.matches(500));
        assertTrue(SearchFilters.SizeBucket.LARGE.matches(5000));
    }
}
