package net.createbrowser.forge;

import net.createbrowser.platform.services.ChatNotifier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Sends system messages to the player's chat via the Minecraft client. */
public final class ForgeChatNotifier implements ChatNotifier {

    @Override
    public void notify(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(message);
        }
    }
}
