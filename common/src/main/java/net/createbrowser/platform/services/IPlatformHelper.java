package net.createbrowser.platform.services;

public interface IPlatformHelper {

    /** Gets the name of the current platform (e.g. "Forge"). */
    String getPlatformName();

    /** Returns true if the mod with the given id is loaded. */
    boolean isModLoaded(String modId);

    /** Returns true when running in a development environment. */
    boolean isDevelopmentEnvironment();

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
