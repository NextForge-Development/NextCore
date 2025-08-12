package gg.nextforge.core;

import org.bukkit.plugin.java.JavaPlugin;

public class NextCore extends JavaPlugin {

    @Override
    public void onEnable() {
        // This method is called when the plugin is enabled
        getLogger().info("NextCore has been enabled!");
    }

    @Override
    public void onDisable() {
        // This method is called when the plugin is disabled
        getLogger().info("NextCore has been disabled!");
    }
}
