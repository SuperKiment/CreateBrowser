package net.createbrowser.compat;

import java.util.List;

/** Abstraction layer for all interactions with the Create mod. Loaded via SPI; falls back to NOOP. */
public interface CreateBridge {

    /** Returns true if the Create mod is loaded in this instance. */
    boolean isCreateLoaded();

    /** Returns the installed Create version string (e.g. "0.5.1.j"), or null if not loaded. */
    String getCreateVersion();

    /** Returns the mod IDs of Create add-ons detected in this instance. */
    List<String> getInstalledCreateAddons();

    /** No-op implementation used when Create is absent or the platform bridge is unavailable. */
    CreateBridge NOOP = new CreateBridge() {
        @Override public boolean isCreateLoaded() { return false; }
        @Override public String getCreateVersion() { return null; }
        @Override public List<String> getInstalledCreateAddons() { return List.of(); }
    };
}
