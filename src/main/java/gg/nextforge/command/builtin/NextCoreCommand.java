package gg.nextforge.command.builtin;

import gg.nextforge.NextCorePlugin;
import gg.nextforge.command.CommandContext;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.stream.Collectors;

public class NextCoreCommand {

    private final NextCorePlugin plugin;

    public NextCoreCommand(NextCorePlugin plugin) {
        this.plugin = plugin;
        registerCommands();
    }

    private void registerCommands() {
        plugin.getCommandManager().command("nextcore")
                .permission("nextcore.command.nextcore")
                .description("NextForge Core Management Command")
                .executor(this::handleRootCommand)
                .subcommand("reload", this::handleReloadCommand)
                .subcommand("version", this::handleVersionCommand)
                //TODO: implement /nextcore debug command
                .subcommand("update", this::handleUpdateCommand)
                .subcommand("info", this::handleInfoCommand)
                .register();
    }

    private void handleRootCommand(CommandContext context) {
        plugin.getMessagesFile().getStringList("commands.nextcore.help").forEach(line -> {
            line = line.replace("%prefix%", plugin.getMessagesFile().getString("general.prefix", "<dark_gray>[<gradient:aqua:dark_aqua>ɴᴇxᴛᴄᴏʀᴇ<dark_gray>]</gradient></dark_gray>"));
            line = line.replace("%version%", plugin.getPluginVersion());
            line = line.replace("%author%", "NextForge Team");
            line = line.replace("%website%", "https://nextforge.gg");
            context.replyMini(line);
        });
    }

    private void handleVersionCommand(CommandContext context) {
        String version = plugin.getPluginVersion();
        plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.version.message", "%prefix% <green>NextCore Version: %version%</green> <gray>(%commit%)</gray>").replace("%version%", version).replace("%commit%", plugin.getUpdater().getCurrentGitCommit() != null ? plugin.getUpdater().getCurrentGitCommit() : "Unknown"));
    }

    private void handleUpdateCommand(CommandContext context) {

        if (context.args().length >= 1 && "download".equalsIgnoreCase(context.args()[1])) {
            try {
                if (plugin.getUpdater().isLatestVersion()) {
                    plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.update.up_to_date", "%prefix% <green>You are using the latest version of NextCore.</green>"));
                    return;
                }
                plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.update.download.start", "%prefix% <yellow>Starting download of NextCore update...</yellow>"));
                plugin.getUpdater().downloadLatestJar("dev".equalsIgnoreCase(plugin.getConfigFile().getString("updater.update_branch", "master"))).thenAccept(file -> {
                    plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.update.download.success", "%prefix% <green>Update downloaded successfully. Restart the server to apply the update.</green>"));
                    plugin.getSLF4JLogger().info("NextCore updated to the latest version: {}", file.getName());
                }).exceptionally(e -> {
                    plugin.getSLF4JLogger().error("Failed to download the latest version", e);
                    plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.update.download.error", "%prefix% <red>Error downloading update: %error%</red>").replace("%error%", e.getMessage()));
                    return null;
                });
            } catch (IOException e) {
                plugin.getSLF4JLogger().error("Failed to download the latest version", e);
                plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.update.error", "%prefix% <red>Error checking for updates: %error%</red>").replace("%error%", e.getMessage()));
            }
            return;
        }

        plugin.getUpdater().checkForUpdates().thenAccept(updateAvailable -> {
            if (updateAvailable) {
                plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.update.available", "%prefix% <green>An update is available! Version: %version% - Do you want to download it? Run /nextcore update download</green>").replace("%version%", plugin.getUpdater().getLatestVersion()));
            } else {
                plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.update.up_to_date", "%prefix% <green>You are using the latest version of NextCore.</green>"));
            }
        }).exceptionally(e -> {
            plugin.getSLF4JLogger().error("Failed to check for updates", e);
            plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.update.error", "%prefix% <red>Error checking for updates: %error%").replace("%error%", e.getMessage()));
            return null;
        });
    }

    private void handleReloadCommand(CommandContext context) {
        long start = System.currentTimeMillis();
        try {
            plugin.reloadConfig();
            plugin.getConfigManager().reloadAll();

            long time = System.currentTimeMillis() - start;
            plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.reload.success", "%prefix% <green>Configuration reloaded successfully.</green> <gray>(%time%ms)</gray>").replace("%time%", time + ""));
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to reload configuration", e);
            plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.reload.error", "%prefix% <red>Failed to reload configuration: %error%").replace("%error%", e.getMessage()));
        }
    }

    private void handleInfoCommand(CommandContext context) {
        File serverProperties = plugin.getDataFolder().getParentFile().toPath().resolve("server.properties").toFile();
        if (!serverProperties.exists()) {
            plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.info.error", "%prefix% <red>Unable to retrieve server information. %error%</red>").replace("%error%", "server.properties file not found."));
            return;
        }
        Properties properties = new Properties();
        try {
            properties.load(new java.io.FileInputStream(serverProperties));
        } catch (java.io.IOException e) {
            plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("commands.nextcore.info.error", "%prefix% <red>Unable to retrieve server information. %error%</red>").replace("%error%", e.getMessage()));
            return;
        }
        String serverName = properties.getProperty("server-name", "Unknown Server");
        String serverVersion = Bukkit.getBukkitVersion();
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        int sizeOfWorlds = Bukkit.getWorlds().size();
        int pluginSize = Bukkit.getPluginManager().getPlugins().length;
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        long uptime = System.currentTimeMillis() - plugin.getServerUptime();
        long memoryUsedMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long tps = MinecraftServer.getServer().recentTps[0] > 0 ? (long) MinecraftServer.getServer().recentTps[0] : 20;
        String formattedTps = formatTps(tps);
        boolean debugMode = plugin.getConfigFile().getBoolean("debug_mode", false); //TODO: implement debug mode
        String debugStatus = debugMode ? "<green>Enabled</green>" : "<red>Disabled</red>";
        String version = plugin.getUpdater().getCurrentVersion();
        String authors = String.join(", ", plugin.getDescription().getAuthors());

        plugin.getMessagesFile().getStringList("commands.nextcore.info.message").forEach(line -> {
            line = line.replace("%prefix%", plugin.getMessagesFile().getString("general.prefix", "<dark_gray>[<gradient:aqua:dark_aqua>ɴᴇxᴛᴄᴏʀᴇ<dark_gray>]</gradient></dark_gray>"));
            line = line.replace("%server_name%", serverName);
            line = line.replace("%server_version%", serverVersion);
            line = line.replace("%online_players%", String.valueOf(onlinePlayers));
            line = line.replace("%max_players%", String.valueOf(maxPlayers));
            line = line.replace("%worlds%", String.valueOf(sizeOfWorlds));
            line = line.replace("%plugins%", String.valueOf(pluginSize));
            line = line.replace("%java_version%", javaVersion);
            line = line.replace("%os%", osName);
            line = line.replace("%uptime%", String.format("%d days, %d hours, %d minutes, %d seconds", uptime / (1000 * 60 * 60 * 24), (uptime / (1000 * 60 * 60)) % 24, (uptime / (1000 * 60)) % 60, (uptime / 1000) % 60));
            line = line.replace("%memory_usage%", String.valueOf(memoryUsedMB));
            line = line.replace("%tps%", formattedTps);
            line = line.replace("%debug_mode%", debugStatus);
            line = line.replace("%nextcore_version%", version);
            line = line.replace("%authors%", authors);
            context.replyMini(line);
        });
    }

    private String formatTps(long tps) {
        String[] tpsColors = {"<red>", "<yellow>", "<green>"};
        if (tps < 0) {
            return "<red>Invalid TPS";
        }
        if (tps < 10) {
            return tpsColors[0] + String.format("%.2f", tps);
        } else if (tps < 18) {
            return tpsColors[1] + String.format("%.2f", tps);
        } else {
            return tpsColors[2] + String.format("%.2f", tps);
        }
    }

}
