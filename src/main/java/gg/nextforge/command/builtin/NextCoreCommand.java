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
        context.replyMini("<gold>╔════════════</gold> <yellow>NextCore v" + plugin.getPluginVersion() + "</yellow> <gold>═════════════</gold>");
        context.replyMini("<gold>║</gold> ");
        context.replyMini("<gold>║</gold> <gray>/nextcore <yellow>reload</yellow><dark_gray> - </dark_gray>Reloads the NextCore configuration");
        context.replyMini("<gold>║</gold> <gray>/nextcore <yellow>version</yellow><dark_gray> - </dark_gray>Displays the NextCore version");
        context.replyMini("<gold>║</gold> <gray>/nextcore <yellow>debug</yellow><dark_gray> - </dark_gray>Toggles debug mode for NextCore");
        context.replyMini("<gold>║</gold> <gray>/nextcore <yellow>info</yellow><dark_gray> - </dark_gray>Get server information");
        context.replyMini("<gold>║</gold> <gray>/nextcore <yellow>test</yellow><dark_gray> - </dark_gray>Run test commands");
        context.replyMini("<gold>║</gold> <gray>/nextcore <yellow>managers</yellow><dark_gray> - </dark_gray>See the managers doing their job");
        context.replyMini("<gold>║</gold> <gray>/nextcore <yellow>help</yellow><dark_gray> - </dark_gray>Shows this help message");
        context.replyMini("<gold>║</gold> ");
        context.replyMini("<gold>╚════════════</gold> <yellow>NextCore v" + plugin.getPluginVersion() + "</yellow> <gold>═════════════</gold>");
    }

    private void handleReloadCommand(CommandContext context) {
        long start = System.currentTimeMillis();
        try {
            plugin.reloadConfig();
            plugin.getConfigManager().reloadAll();

            long time = System.currentTimeMillis() - start;
            plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("reload.success", "%prefix% <green>Configuration reloaded successfully.</green> <gray>(%time%ms)</gray>").replace("%time%", time + ""));
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to reload configuration", e);
            plugin.getTextManager().send(context.sender(), plugin.getMessagesFile().getString("reload.error", "%prefix% <red>Failed to reload configuration: %error%").replace("%error%", e.getMessage()));
        }
    }

    private void handleInfoCommand(CommandContext context) {
        context.replyMini("<gold>╔═════════════</gold> <yellow>Server Information</yellow> <gold>═════════════</gold>");
        context.replyMini("<gold>║</gold>");
        context.replyMini("<gold>╚═════════════</gold> <yellow>End of Information</yellow> <gold>═════════════</gold>");
    }

}
