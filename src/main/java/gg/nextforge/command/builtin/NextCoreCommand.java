package gg.nextforge.command.builtin;

import gg.nextforge.NextCorePlugin;
import gg.nextforge.command.CommandContext;

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
        context.replyMini("<gold>╔═════════════</gold> <yellow>Server Information</yellow> <gold>═════════════</gold>");
        context.replyMini("<gold>║</gold>");
        context.replyMini("<gold>╚═════════════</gold> <yellow>End of Information</yellow> <gold>═════════════</gold>");
    }

}
