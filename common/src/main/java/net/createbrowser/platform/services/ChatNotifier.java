package net.createbrowser.platform.services;

import net.minecraft.network.chat.Component;

/** Sends a system message to the local player's chat (client-side only). */
public interface ChatNotifier {

    void notify(Component message);
}
