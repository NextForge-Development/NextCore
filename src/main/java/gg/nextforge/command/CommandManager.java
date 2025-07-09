package gg.nextforge.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CommandManager provides a modernized framework for managing commands in Bukkit plugins.<br>
 * <br>
 * This class simplifies the creation, registration, and execution of commands, offering:<br>
 * - Builder pattern for command creation<br>
 * - Automatic tab completion<br>
 * - Subcommand support<br>
 * - Permission checking<br>
 * - Improved argument parsing<br>
 */
public class CommandManager {

    private final Plugin plugin;
    private final Map<String, CoreCommand> commands = new ConcurrentHashMap<>();

    /**
     * Constructs a CommandManager instance.
     *
     * @param plugin The plugin instance associated with this command manager.
     */
    public CommandManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a new command builder for defining and registering commands.
     *
     * @param name The name of the command.
     * @return A CommandBuilder instance for fluent command creation.
     */
    public CommandBuilder command(String name) {
        return new CommandBuilder(name);
    }

    /**
     * Retrieves all registered commands.
     *
     * @return A map of command names to their corresponding CoreCommand instances.
     */
    public Map<String, CoreCommand> getRegisteredCommands() {
        return new HashMap<>(commands);
    }

    /**
     * Registers a command with Bukkit.
     * This method is for internal use and is called by CommandBuilder.
     *
     * @param command The CoreCommand instance to register.
     */
    void registerCommand(CoreCommand command) {
        commands.put(command.getName(), command);

        try {
            // Access Bukkit's command map via reflection
            var commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            // Create a Bukkit command that delegates to the custom command system
            Command bukkitCommand = getCommand(command);

            // Register the command with Bukkit
            commandMap.register(plugin.getName().toLowerCase(), bukkitCommand);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register command: " + command.getName());
            e.printStackTrace();
        }
    }

    private static @NotNull Command getCommand(CoreCommand command) {
        Command bukkitCommand = new Command(command.getName()) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return command.execute(sender, label, args);
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                return command.tabComplete(sender, alias, args);
            }
        };

        // Set command properties
        bukkitCommand.setDescription(command.getDescription());
        bukkitCommand.setUsage(command.getUsage());
        bukkitCommand.setPermission(command.getPermission());
        bukkitCommand.setAliases(command.getAliases());
        return bukkitCommand;
    }

    /**
     * CommandBuilder provides a fluent API for creating and registering commands.
     */
    public class CommandBuilder {
        private final CoreCommand command;

        /**
         * Constructs a CommandBuilder instance.
         *
         * @param name The name of the command.
         */
        CommandBuilder(String name) {
            this.command = new CoreCommand(name);
        }

        /**
         * Sets the permission required to execute the command.
         *
         * @param permission The permission string.
         * @return The current CommandBuilder instance for chaining.
         */
        public CommandBuilder permission(String permission) {
            command.setPermission(permission);
            return this;
        }

        /**
         * Sets the description of the command.
         *
         * @param description The command description.
         * @return The current CommandBuilder instance for chaining.
         */
        public CommandBuilder description(String description) {
            command.setDescription(description);
            return this;
        }

        /**
         * Sets the usage information for the command.
         *
         * @param usage The usage string.
         * @return The current CommandBuilder instance for chaining.
         */
        public CommandBuilder usage(String usage) {
            command.setUsage(usage);
            return this;
        }

        /**
         * Sets aliases for the command.
         *
         * @param aliases The aliases for the command.
         * @return The current CommandBuilder instance for chaining.
         */
        public CommandBuilder aliases(String... aliases) {
            command.setAliases(Arrays.asList(aliases));
            return this;
        }

        /**
         * Sets the executor for the command.
         *
         * @param executor The CommandExecutor instance.
         * @return The current CommandBuilder instance for chaining.
         */
        public CommandBuilder executor(CommandExecutor executor) {
            command.setExecutor(executor);
            return this;
        }

        /**
         * Sets the tab completer for the command.
         *
         * @param completer The TabCompleter instance.
         * @return The current CommandBuilder instance for chaining.
         */
        public CommandBuilder tabCompleter(TabCompleter completer) {
            command.setTabCompleter(completer);
            return this;
        }

        /**
         * Adds a subcommand to the command.
         *
         * @param name     The name of the subcommand.
         * @param executor The executor for the subcommand.
         * @return The current CommandBuilder instance for chaining.
         */
        public CommandBuilder subcommand(String name, CommandExecutor executor) {
            command.addSubcommand(name, executor);
            return this;
        }

        /**
         * Registers the command with Bukkit.
         * This is the final step in the command creation process.
         *
         * @return The registered CoreCommand instance.
         */
        public CoreCommand register() {
            registerCommand(command);
            return command;
        }
    }
}