package net.createbrowser.gui;

import net.createbrowser.Constants;
import net.createbrowser.storage.LocalFile;
import net.createbrowser.storage.SchematicFileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;

/** Manage a single local .nbt: rename or delete. Pushed from the BrowserScreen LOCAL tab. */
public final class LocalManagerScreen extends Screen {

    private enum Mode { ACTIONS, RENAMING, CONFIRM_DELETE }

    private final LocalFile file;
    private final Screen parent;

    private Mode mode = Mode.ACTIONS;
    private EditBox renameInput;
    private String message = "";
    private int messageColor = 0xCCCCCC;

    public LocalManagerScreen(LocalFile file, Screen parent) {
        super(Component.translatable("createbrowser.screen.local.rename_title"));
        this.file = file;
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int cx = this.width / 2;
        int cy = this.height / 2;

        switch (mode) {
            case ACTIONS -> {
                addRenderableWidget(Button.builder(
                    Component.translatable("createbrowser.screen.local.rename"),
                    b -> { mode = Mode.RENAMING; init(); }
                ).bounds(cx - 145, cy - 10, 90, 20).build());

                addRenderableWidget(Button.builder(
                    Component.translatable("createbrowser.screen.local.delete"),
                    b -> { mode = Mode.CONFIRM_DELETE; init(); }
                ).bounds(cx - 45, cy - 10, 90, 20).build());

                addRenderableWidget(Button.builder(
                    Component.translatable("createbrowser.screen.local.publish"),
                    b -> Minecraft.getInstance().setScreen(new UploadScreen(file, this))
                ).bounds(cx + 55, cy - 10, 90, 20).build());

                addRenderableWidget(Button.builder(
                    Component.translatable("createbrowser.screen.detail.back"),
                    b -> Minecraft.getInstance().setScreen(parent)
                ).bounds(cx - 50, cy + 30, 100, 20).build());
            }
            case RENAMING -> {
                renameInput = new EditBox(this.font, cx - 150, cy - 10, 300, 20,
                    Component.translatable("createbrowser.screen.local.rename_title"));
                renameInput.setMaxLength(80);
                renameInput.setValue(file.displayName());
                addRenderableWidget(renameInput);
                this.setInitialFocus(renameInput);

                addRenderableWidget(Button.builder(
                    Component.translatable("createbrowser.screen.local.rename_save"),
                    b -> performRename()
                ).bounds(cx - 100, cy + 20, 90, 20).build());

                addRenderableWidget(Button.builder(
                    Component.translatable("createbrowser.screen.local.rename_cancel"),
                    b -> { mode = Mode.ACTIONS; init(); }
                ).bounds(cx + 10, cy + 20, 90, 20).build());
            }
            case CONFIRM_DELETE -> {
                addRenderableWidget(Button.builder(
                    Component.translatable("createbrowser.screen.local.delete_yes"),
                    b -> performDelete()
                ).bounds(cx - 100, cy + 20, 90, 20).build());

                addRenderableWidget(Button.builder(
                    Component.translatable("createbrowser.screen.local.delete_no"),
                    b -> { mode = Mode.ACTIONS; init(); }
                ).bounds(cx + 10, cy + 20, 90, 20).build());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
            Component.literal(file.displayName() + ".nbt"),
            this.width / 2, 38, 0xCCCCCC);

        if (mode == Mode.CONFIRM_DELETE) {
            graphics.drawCenteredString(this.font,
                Component.translatable("createbrowser.screen.local.delete_confirm", file.displayName()),
                this.width / 2, this.height / 2 - 20, 0xFFAA55);
        }

        if (!message.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.literal(message),
                this.width / 2, this.height - 40, messageColor);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void performRename() {
        String newName = renameInput.getValue().trim();
        if (newName.isEmpty()) return;
        net.createbrowser.util.AsyncExecutor.run(
            () -> SchematicFileManager.rename(file.path(), newName),
            renamed -> {
                message = "Renamed to " + renamed.getFileName();
                messageColor = 0x55FF55;
                if (Minecraft.getInstance().screen == this) {
                    Minecraft.getInstance().setScreen(parent);
                }
            },
            e -> {
                Constants.LOG.error("[CreateBrowser] Rename failed: {}", e.getMessage(), e);
                message = "Rename failed: " + e.getMessage();
                messageColor = 0xFF5555;
            }
        );
    }

    private void performDelete() {
        net.createbrowser.util.AsyncExecutor.run(
            () -> {
                SchematicFileManager.delete(file.path());
                return true;
            },
            ok -> {
                if (Minecraft.getInstance().screen == this) {
                    Minecraft.getInstance().setScreen(parent);
                }
            },
            e -> {
                Constants.LOG.error("[CreateBrowser] Delete failed: {}", e.getMessage(), e);
                message = "Delete failed: " + e.getMessage();
                messageColor = 0xFF5555;
            }
        );
    }
}
