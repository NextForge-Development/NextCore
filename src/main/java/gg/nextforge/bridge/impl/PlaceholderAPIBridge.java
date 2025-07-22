package gg.nextforge.bridge.impl;

import gg.nextforge.bridge.PluginBridge;
import gg.nextforge.bridge.annotations.Bridge;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Integration bridge for PlaceholderAPI.
 */
@Bridge(description = "Integration with PlaceholderAPI")
public class PlaceholderAPIBridge extends PluginBridge {

    @Override
    public String getPluginName() {
        return "PlaceholderAPI";
    }

    @Override
    public void onEnable(Plugin plugin) {
        // Optional setup if needed
    }

    public String setPlaceholder(Player player, String placeholder) {
        return PlaceholderAPI.setPlaceholders(player, placeholder);
    }

    public String setPlaceholder(OfflinePlayer offlinePlayer, String placeholder) {
        return PlaceholderAPI.setPlaceholders(offlinePlayer, placeholder);
    }
}
