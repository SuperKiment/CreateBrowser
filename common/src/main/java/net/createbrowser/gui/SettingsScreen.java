package net.createbrowser.gui;

import net.createbrowser.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** In-game settings editor for the createmod.com credentials and UI toggles. */
public class SettingsScreen extends Screen {

    private final Screen parent;

    private EditBox apiKeyBox;
    private EditBox modSecretBox;
    private Button thumbnailsButton;
    private boolean showThumbnails;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("createbrowser.screen.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.showThumbnails = Services.CONFIG.showThumbnails();

        int fieldWidth = Math.min(300, this.width - 60);
        int x = (this.width - fieldWidth) / 2;
        int y = 50;

        apiKeyBox = new EditBox(this.font, x, y, fieldWidth, 20,
            Component.translatable("createbrowser.settings.api_key"));
        apiKeyBox.setMaxLength(128);
        apiKeyBox.setValue(Services.CONFIG.apiKey());
        addRenderableWidget(apiKeyBox);
        y += 50;

        modSecretBox = new EditBox(this.font, x, y, fieldWidth, 20,
            Component.translatable("createbrowser.settings.mod_secret"));
        modSecretBox.setMaxLength(128);
        modSecretBox.setValue(Services.CONFIG.modSecret());
        addRenderableWidget(modSecretBox);
        y += 40;

        thumbnailsButton = Button.builder(thumbnailsLabel(), b -> {
            showThumbnails = !showThumbnails;
            b.setMessage(thumbnailsLabel());
        }).bounds(x, y, fieldWidth, 20).build();
        addRenderableWidget(thumbnailsButton);

        int btnY = this.height - 28;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> saveAndClose())
            .bounds(this.width / 2 - 104, btnY, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> close())
            .bounds(this.width / 2 + 4, btnY, 100, 20).build());
    }

    private Component thumbnailsLabel() {
        return Component.translatable("createbrowser.settings.show_thumbnails")
            .append(": ")
            .append(showThumbnails ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    private void saveAndClose() {
        String apiKey = apiKeyBox.getValue().trim();
        String modSecret = modSecretBox.getValue().trim();
        boolean thumbs = showThumbnails;
        // ForgeConfigSpec.set() writes the TOML file — keep it off the game thread.
        net.createbrowser.util.AsyncExecutor.run(
            () -> {
                Services.CONFIG.setApiKey(apiKey);
                Services.CONFIG.setModSecret(modSecret);
                Services.CONFIG.setShowThumbnails(thumbs);
                return true;
            },
            ok -> close(),
            err -> {
                net.createbrowser.Constants.LOG.error("[CreateBrowser] Settings save failed", err);
                close();
            }
        );
    }

    private void close() {
        if (Minecraft.getInstance().screen == this) {
            Minecraft.getInstance().setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        graphics.drawString(this.font,
            Component.translatable("createbrowser.settings.api_key"),
            apiKeyBox.getX(), apiKeyBox.getY() - 11, 0xAAAAAA);
        graphics.drawString(this.font,
            Component.translatable("createbrowser.settings.api_key_hint"),
            apiKeyBox.getX(), apiKeyBox.getY() + 24, 0x777777);
        graphics.drawString(this.font,
            Component.translatable("createbrowser.settings.mod_secret"),
            modSecretBox.getX(), modSecretBox.getY() - 11, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
