package net.createbrowser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Guards against Constants drifting from gradle.properties (version is sent in the User-Agent and HMAC). */
class ConstantsTest {

    @Test
    void modVersionMatchesGradle() {
        assertEquals(System.getProperty("createbrowser.version"), Constants.MOD_VERSION);
    }

    @Test
    void mcVersionMatchesGradle() {
        assertEquals(System.getProperty("createbrowser.mcVersion"), Constants.MC_VERSION);
    }
}
