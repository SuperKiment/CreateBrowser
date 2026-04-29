package net.createbrowser;

/** Common bootstrap — invoked by each loader's entry point. */
public final class CreateBrowserMod {

    private CreateBrowserMod() {}

    public static void init() {
        Constants.LOG.info("CreateBrowser common bootstrap complete");
    }
}
