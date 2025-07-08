package gg.nextforge;

import gg.nextforge.plugin.NextForgePlugin;

import java.util.UUID;

public class NextCorePlugin extends NextForgePlugin {
    @Override
    public int getMetricsId() {
        return 0; // Replace with actual metrics ID if applicable
    }

    @Override
    public UUID getPluginId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    @Override
    public void enable(boolean isReload) {
        if (isReload) {
            getSLF4JLogger().info("[NextForge] Our plugins are not designed to be reloaded. Please restart the server or use /nextforge reload <pluginName> to reload it's configuration.");
        }
    }

    @Override
    public void disable() {

    }
}
