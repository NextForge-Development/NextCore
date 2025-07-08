package gg.nextforge;

import gg.nextforge.config.ConfigFile;
import gg.nextforge.config.ConfigManager;
import gg.nextforge.console.ConsoleHeader;
import gg.nextforge.plugin.NextForgePlugin;
import gg.nextforge.scheduler.CoreScheduler;
import gg.nextforge.scheduler.ScheduledTask;
import gg.nextforge.updater.CoreAutoUpdater;

import java.io.IOException;
import java.util.UUID;

public class NextCorePlugin extends NextForgePlugin {

    ConfigFile configFile;
    ScheduledTask updateCheckTask;

    @Override
    public int getMetricsId() {
        return 26427; // Replace with actual metrics ID if applicable
    }

    @Override
    public UUID getPluginId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    @Override
    public void enable(boolean isReload) {
        if (isReload) {
            getSLF4JLogger().info("[NextForge] Our plugins are not designed to be reloaded. Please restart the server or use /nextforge reload <pluginName> to reload it's configuration.");
            return;
        }

        configFile = getConfigManager().loadConfig("config.yml");

        ConsoleHeader.send(this);

        getSLF4JLogger().info("[NextForge] {} v{} is running as the core plugin.", getName(), getPluginVersion());

        getSLF4JLogger().info("[NextForge] Initializing {} v{}...", getName(), getPluginVersion());

        CoreAutoUpdater updater = new CoreAutoUpdater(getDataFolder().getParentFile());

        int checkMillis = configFile.getInt("updater.check_interval", 7200000);
        int checkTicks = checkMillis / 50;
        if (configFile.getBoolean("updater.auto_update", true)) {
            updateCheckTask = CoreScheduler.runAsyncTimer(() -> {
                try {
                    if (configFile.getBoolean("updater.disable_update_message", false)) {
                        if (updater.isLatestVersion()) {
                            getSLF4JLogger().info("[NextForge] You are running the latest version of NextCore.");
                        } else {
                            getSLF4JLogger().warn("[NextForge] A new version of NextForge is available! Please update to the latest version for the best experience.");
                            getSLF4JLogger().warn("[NextForge] Current version: {}, Latest version: {}", getPluginVersion(), updater.fetchLatestVersion());
                            getSLF4JLogger().warn("[NextForge] TIP: By changing the updater settings in config.yml, you can disable this message or change the update channel to 'dev'.");
                            getSLF4JLogger().warn("[NextForge] TIP: By changing the updater settings in config.yml, you can automatically download the newest version.");
                        }
                    }
                    if (configFile.getBoolean("updater.auto_update", true)) {
                        if (!updater.isLatestVersion()) {
                            getSLF4JLogger().info("[NextForge] Downloading the latest version of NextCore...");
                            updater.downloadLatestJar(configFile.getString("updater.update_branch", "master").equalsIgnoreCase("dev")).thenAccept((file -> {
                                getSLF4JLogger().info("[NextForge] Download complete. Please restart the server to apply the update.");
                            }));
                        } else {
                            getSLF4JLogger().info("[NextForge] No updates available. You are already running the latest version.");
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 1, checkTicks);
        }

        getSLF4JLogger().info("[NextForge] {} v{} has been successfully enabled.", getName(), getPluginVersion());
    }

    @Override
    public void disable() {
        if (updateCheckTask != null) {
            updateCheckTask.cancel();
        }

        getSLF4JLogger().info("[NextForge] {} v{} is shutting down.", getName(), getPluginVersion());

        getSLF4JLogger().info("[NextForge] {} v{} has been successfully disabled.", getName(), getPluginVersion());
    }
}
