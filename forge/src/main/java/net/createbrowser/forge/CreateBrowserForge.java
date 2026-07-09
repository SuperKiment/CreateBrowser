package net.createbrowser.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.createbrowser.Constants;
import net.createbrowser.CreateBrowserMod;
import net.createbrowser.forge.config.ForgeBrowserConfig;
import net.createbrowser.gui.BrowserScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge mod entry point — registers config, keybind, and wires common bootstrap. */
@Mod(Constants.MOD_ID)
public class CreateBrowserForge {

    public static final KeyMapping OPEN_BROWSER = new KeyMapping(
        "key.createbrowser.open",
        InputConstants.KEY_N,
        "key.categories.createbrowser"
    );

    public CreateBrowserForge() {
        Constants.LOG.info("CreateBrowser Forge bootstrap");

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ForgeBrowserConfig.SPEC);

        CreateBrowserMod.init();

        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onRegisterKeyMappings);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BROWSER);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) return;
        while (OPEN_BROWSER.consumeClick()) {
            Minecraft.getInstance().setScreen(new BrowserScreen());
        }
    }
}
