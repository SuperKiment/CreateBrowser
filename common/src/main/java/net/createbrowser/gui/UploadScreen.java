package net.createbrowser.gui;

import net.createbrowser.api.model.UploadRequest;
import net.createbrowser.api.model.UploadResult;
import net.createbrowser.api.source.SchematicSource;
import net.createbrowser.api.source.SourceRegistry;
import net.createbrowser.platform.Services;
import net.createbrowser.storage.LocalFile;
import net.createbrowser.util.AsyncExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Confirmation screen before uploading a local .nbt to createmod.com (anonymous endpoint).
 * The API takes only the file — no metadata fields.
 */
public final class UploadScreen extends Screen {

    private static final int MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB server limit

    private enum State { IDLE, UPLOADING, DONE, ERROR }

    private final LocalFile file;
    private final Screen parent;
    private final SchematicSource source = SourceRegistry.primary();

    private Button publishButton;
    private Button cancelButton;

    private State state = State.IDLE;
    private String statusMessage = "";
    private int statusColor = 0xCCCCCC;

    public UploadScreen(LocalFile file, Screen parent) {
        super(Component.translatable("createbrowser.screen.upload.title"));
        this.file = file;
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int cx = this.width / 2;
        int btnY = this.height / 2 + 20;

        publishButton = Button.builder(
            Component.translatable("createbrowser.screen.upload.publish"),
            b -> startUpload()
        ).bounds(cx - 105, btnY, 100, 20).build();
        addRenderableWidget(publishButton);

        cancelButton = Button.builder(
            Component.translatable("createbrowser.screen.upload.cancel"),
            b -> Minecraft.getInstance().setScreen(parent)
        ).bounds(cx + 5, btnY, 100, 20).build();
        addRenderableWidget(cancelButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;

        graphics.drawCenteredString(this.font, this.title, cx, cy - 40, 0xFFFFFF);

        String fileInfo = file.displayName() + ".nbt  (" + (file.sizeBytes() / 1024) + " KB)";
        graphics.drawCenteredString(this.font, Component.literal(fileInfo), cx, cy - 20, 0xCCCCCC);

        if (state == State.UPLOADING) {
            graphics.drawCenteredString(this.font,
                Component.translatable("createbrowser.screen.upload.uploading"), cx, cy, 0xFFCC44);
        }

        if (!statusMessage.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.literal(statusMessage),
                cx, cy + 5, statusColor);
        }
    }

    private void startUpload() {
        if (file.sizeBytes() > MAX_SIZE_BYTES) {
            statusMessage = Component.translatable("createbrowser.screen.upload.too_large").getString();
            statusColor = 0xFF5555;
            return;
        }

        state = State.UPLOADING;
        publishButton.active = false;
        cancelButton.active = false;
        statusMessage = "";

        UploadRequest req = new UploadRequest(
            file.path(), file.displayName(), "", List.of(), List.of(), List.of()
        );

        AsyncExecutor.run(
            () -> source.upload(req),
            result -> onUploadSuccess(result),
            err -> onUploadError(err)
        );
    }

    private void onUploadSuccess(UploadResult result) {
        state = State.DONE;
        String url = result.url() != null ? result.url()
            : ("https://createmod.com/u/" + result.token());
        Services.CHAT.notify(
            Component.translatable("createbrowser.chat.upload_success", file.displayName(), url));
        Minecraft.getInstance().setScreen(parent);
    }

    private void onUploadError(Throwable err) {
        net.createbrowser.Constants.LOG.error("[CreateBrowser] Upload failed: {}", err.getMessage(), err);
        state = State.ERROR;
        publishButton.active = true;
        cancelButton.active = true;
        String msg = err.getMessage() != null ? err.getMessage() : "unknown";
        if (msg.contains("409")) {
            statusMessage = Component.translatable("createbrowser.error.conflict").getString();
        } else if (msg.contains("429")) {
            statusMessage = Component.translatable("createbrowser.error.rate_limited").getString();
        } else {
            statusMessage = Component.translatable("createbrowser.error.network").getString();
        }
        statusColor = 0xFF5555;
        Services.CHAT.notify(
            Component.translatable("createbrowser.chat.upload_error", statusMessage));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
