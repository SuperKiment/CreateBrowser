package net.createbrowser.platform;

import net.createbrowser.Constants;
import net.createbrowser.compat.CreateBridge;
import net.createbrowser.config.BrowserConfig;
import net.createbrowser.platform.services.ChatNotifier;
import net.createbrowser.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

/** Loads platform-specific service implementations via Java SPI. */
public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final BrowserConfig CONFIG = loadOrNoop(BrowserConfig.class, BrowserConfig.DEFAULT);
    public static final ChatNotifier CHAT = load(ChatNotifier.class);
    public static final CreateBridge CREATE = loadOrNoop(CreateBridge.class, CreateBridge.NOOP);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    public static <T> T loadOrNoop(Class<T> clazz, T fallback) {
        return ServiceLoader.load(clazz).findFirst().orElseGet(() -> {
            Constants.LOG.debug("No service found for {} — using fallback", clazz.getName());
            return fallback;
        });
    }
}
