package net.createbrowser.forge.mixin;

import net.createbrowser.gui.BrowserScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects a "Browse Online" button into Create's SchematicTableScreen.
 * required=false — fails silently if Create is absent or the class is renamed.
 * No refmap is generated (Create is not on the compile classpath), so the vanilla {@code init()} override is
 * targeted by both its Mojang name (dev runtime) and its SRG name {@code m_7856_} (Forge 1.20.1 production).
 */
@Mixin(targets = "com.simibubi.create.content.schematics.table.SchematicTableScreen", remap = false)
public abstract class SchematicTableScreenMixin extends Screen {

    protected SchematicTableScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"), remap = false)
    private void createbrowser$addBrowseButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(
            Component.translatable("createbrowser.button.browse"),
            btn -> Minecraft.getInstance().setScreen(new BrowserScreen())
        ).bounds(this.width / 2 + 60, this.height / 2 - 30, 80, 20).build());
    }
}
