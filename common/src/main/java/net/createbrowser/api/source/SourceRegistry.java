package net.createbrowser.api.source;

import java.util.List;

/** Central registry of available schematic sources; the first entry is the default network source. */
public final class SourceRegistry {

    private static volatile List<SchematicSource> networkSources;

    private SourceRegistry() { }

    /** Returns all network-backed sources, lazily instantiated once. */
    public static List<SchematicSource> networkSources() {
        List<SchematicSource> local = networkSources;
        if (local == null) {
            synchronized (SourceRegistry.class) {
                local = networkSources;
                if (local == null) {
                    local = List.of(new CreateModComSource());
                    networkSources = local;
                }
            }
        }
        return local;
    }

    /** Returns the primary network source used by the browser screens. */
    public static SchematicSource primary() {
        return networkSources().get(0);
    }
}
