package gg.nextforge.command;

import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a command with all its properties.<br>
 * This class handles the execution and tab completion of commands.<br>
 * <br>
 * Features:<br>
 * - Permission checking<br>
 * - Subcommand support<br>
 * - Custom executor and tab completer<br>
 */
public class CoreCommand {

    private final String name; // The name of the command
    private final Map<String, CommandExecutor> subcommands = new ConcurrentHashMap<>(); // Subcommands mapped by their names
    private String permission = null; // The permission required to execute the command
    private String description = ""; // A brief description of the command
    private String usage = ""; // Usage information for the command
    private List<String> aliases = new ArrayList<>(); // Aliases for the command
    private CommandExecutor executor = null; // The main executor for the command
    private TabCompleter tabCompleter = null; // The tab completer for the command

    /**
     * Constructs a CoreCommand instance with the specified name.
     *
     * @param name The name of the command.
     */
    CoreCommand(String name) {
        this.name = name.toLowerCase();
    }

    /**
     * Executes the command.
     * Handles permission checking and subcommand routing.
     *
     * @param sender The sender of the command.
     * @param label  The command label used.
     * @param args   The arguments passed to the command.
     * @return True if the command was successfully executed, false otherwise.
     */
    public boolean execute(CommandSender sender, String label, String[] args) {
        // Permission check
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        // Check for subcommands
        if (args.length > 0 && subcommands.containsKey(args[0].toLowerCase())) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            CommandContext context = new CommandContext(sender, label, subArgs);
            subcommands.get(args[0].toLowerCase()).execute(context);
            return true;
        }

        // Execute main command
        if (executor != null) {
            CommandContext context = new CommandContext(sender, label, args);
            executor.execute(context);
            return true;
        }

        // No executor set, show usage
        // TODO: show usage message
        return true;
    }

    /**
     * Handles tab completion for the command.
     * Provides suggestions based on the current input.
     *
     * @param sender The sender of the command.
     * @param alias  The alias used for the command.
     * @param args   The arguments passed to the command.
     * @return A list of suggestions for tab completion.
     */
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        // Permission check
        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }

        // Subcommand completion
        if (args.length == 1 && !subcommands.isEmpty()) {
            return subcommands.keySet().stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .sorted()
                    .toList();
        }

        // Custom tab completer
        if (tabCompleter != null) {
            CommandContext context = new CommandContext(sender, alias, args);
            return tabCompleter.complete(context);
        }

        return Collections.emptyList();
    }

    // Getters and setters for the builder

    /**
     * Gets the name of the command.
     *
     * @return The name of the command.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the permission required to execute the command.
     *
     * @return The permission string, or null if no permission is required.
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Sets the permission required to execute the command.
     *
     * @param permission The permission string.
     */
    void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * Gets the description of the command.
     *
     * @return The description of the command.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the command.
     *
     * @param description The description string.
     */
    void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the usage information for the command.
     *
     * @return The usage string.
     */
    public String getUsage() {
        return usage;
    }

    /**
     * Sets the usage information for the command.
     *
     * @param usage The usage string.
     */
    void setUsage(String usage) {
        this.usage = usage;
    }

    /**
     * Gets the aliases for the command.
     *
     * @return A list of aliases.
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * Sets the aliases for the command.
     *
     * @param aliases A list of aliases.
     */
    void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    /**
     * Sets the executor for the command.
     *
     * @param executor The CommandExecutor instance.
     */
    void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }

    /**
     * Sets the tab completer for the command.
     *
     * @param completer The TabCompleter instance.
     */
    void setTabCompleter(TabCompleter completer) {
        this.tabCompleter = completer;
    }

    /**
     * Adds a subcommand to the command.
     *
     * @param name     The name of the subcommand.
     * @param executor The executor for the subcommand.
     */
    void addSubcommand(String name, CommandExecutor executor) {
        subcommands.put(name.toLowerCase(), executor);
    }
}