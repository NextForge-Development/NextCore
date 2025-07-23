package gg.nextforge.ui;

import gg.nextforge.ui.context.UIContextManager;
import gg.nextforge.ui.support.ChatPromptUI;
import gg.nextforge.ui.support.HotbarUI;
import org.bukkit.plugin.Plugin;

/**
 * Central UI bootstrapper and manager.
 */
public class UIManager {

    private Plugin plugin;

    public void init(Plugin pl) {
        plugin = pl;

        ChatPromptUI.registerListener(plugin);
        HotbarUI.registerListener(plugin);

        // Additional: Register Inventory click listeners etc.
        // e.g. InventoryInteractionHandler.register(plugin);
    }

    public void shutdown() {
        UIContextManager.clearAll();
    }
}
