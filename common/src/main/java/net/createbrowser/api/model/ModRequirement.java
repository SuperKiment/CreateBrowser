package net.createbrowser.api.model;

/** A required mod for a schematic, with an optional version constraint string from the API. */
public record ModRequirement(String id, String version) {
    public boolean hasVersion() {
        return version != null && !version.isBlank();
    }
}
