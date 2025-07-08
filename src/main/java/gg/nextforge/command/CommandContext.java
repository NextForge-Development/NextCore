package gg.nextforge.command;

import gg.nextforge.plugin.NextForgePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CommandContext provides utilities for handling command execution in a Bukkit plugin. <br>
 * <br>
 * This class simplifies argument parsing, sender validation, and tab completion. <br>
 * It is designed to be passed to command executors for easier handling of commands. <br>
 * <br>
 * Features: <br>
 * - Type-safe argument parsing <br>
 * - Default values for arguments <br>
 * - Player lookup and validation <br>
 * - Common tab completion utilities <br>
 */
public record CommandContext(CommandSender sender, String label, String[] args) {

    /**
     * Constructs a CommandContext instance.
     *
     * @param sender The sender of the command.
     * @param label  The command label used.
     * @param args   The arguments passed to the command.
     */
    public CommandContext {
    }

    /**
     * Gets the command sender.
     *
     * @return The sender of the command.
     */
    @Override
    public CommandSender sender() {
        return sender;
    }

    /**
     * Checks if the sender is a player.
     *
     * @return True if the sender is a Player, false otherwise.
     */
    public boolean isPlayer() {
        return sender instanceof Player;
    }

    /**
     * Gets the sender as a Player.
     *
     * @return The sender cast to Player.
     * @throws IllegalStateException If the sender is not a Player.
     */
    public Player getPlayer() {
        if (!isPlayer()) {
            throw new IllegalStateException(
                    "Command sender is not a player. Check isPlayer() first."
            );
        }
        return (Player) sender;
    }

    /**
     * Gets the command label used.
     *
     * @return The command label.
     */
    @Override
    public String label() {
        return label;
    }

    /**
     * Gets all arguments passed to the command.
     *
     * @return An array of arguments.
     */
    @Override
    public String[] args() {
        return args;
    }

    /**
     * Gets the argument at the specified index.
     *
     * @param index The index of the argument.
     * @return The argument at the index, or null if out of bounds.
     */
    public String getArg(int index) {
        return index < args.length ? args[index] : null;
    }

    /**
     * Gets the argument as a string, with a default value.
     *
     * @param index        The index of the argument.
     * @param defaultValue The default value to return if the argument is null.
     * @return The argument as a string, or the default value.
     */
    public String getString(int index, String defaultValue) {
        String arg = getArg(index);
        return arg != null ? arg : defaultValue;
    }

    /**
     * Gets the argument as an integer, with a default value.
     *
     * @param index        The index of the argument.
     * @param defaultValue The default value to return if the argument is not a valid number.
     * @return The argument as an integer, or the default value.
     */
    public int getInt(int index, int defaultValue) {
        String arg = getArg(index);
        if (arg == null) return defaultValue;

        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets the argument as a double, with a default value.
     *
     * @param index        The index of the argument.
     * @param defaultValue The default value to return if the argument is not a valid number.
     * @return The argument as a double, or the default value.
     */
    public double getDouble(int index, double defaultValue) {
        String arg = getArg(index);
        if (arg == null) return defaultValue;

        try {
            return Double.parseDouble(arg);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets the argument as a boolean, with a default value.
     * Accepts: true/false, yes/no, on/off, 1/0.
     *
     * @param index        The index of the argument.
     * @param defaultValue The default value to return if the argument is null or invalid.
     * @return The argument as a boolean, or the default value.
     */
    public boolean getBoolean(int index, boolean defaultValue) {
        String arg = getArg(index);
        if (arg == null) return defaultValue;

        arg = arg.toLowerCase();
        return arg.equals("true") || arg.equals("yes") ||
                arg.equals("on") || arg.equals("1");
    }

    /**
     * Gets the argument as an online Player.
     *
     * @param index The index of the argument.
     * @return The Player object, or null if not found.
     */
    public Player getPlayer(int index) {
        String arg = getArg(index);
        if (arg == null) return null;

        Player player = Bukkit.getPlayerExact(arg);
        if (player == null) {
            //TODO: Send a message to the sender if player not found
        }
        return player;
    }

    /**
     * Joins arguments from the specified index to the end.
     *
     * @param startIndex The starting index.
     * @return A single string containing the joined arguments.
     */
    public String joinArgs(int startIndex) {
        if (startIndex >= args.length) return "";

        return String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
    }

    /**
     * Checks if the sender has the specified permission.
     * Sends an error message if the sender lacks the permission.
     *
     * @param permission The permission to check.
     * @return True if the sender has the permission, false otherwise.
     */
    public boolean checkPermission(String permission) {
        if (!sender.hasPermission(permission)) {
            // TODO: Send a message to the sender if they lack permission
            return false;
        }
        return true;
    }

    /**
     * Sends a message to the sender.
     *
     * @param message The message to send.
     */
    public void reply(String message) {
        sender.sendMessage(message);
    }

    /**
     * Gets the plugin instance.
     *
     * @return The NextForgePlugin instance.
     */
    public NextForgePlugin getPlugin() {
        return NextForgePlugin.getInstance();
    }

    /**
     * Gets the names of all online players for tab completion.
     *
     * @return A list of online player names.
     */
    public List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gets the names of online players that start with the specified prefix.
     *
     * @param prefix The prefix to filter player names.
     * @return A list of player names starting with the prefix.
     */
    public List<String> getOnlinePlayerNames(String prefix) {
        String lower = prefix.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(lower))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Filters a list of options by the specified prefix.
     *
     * @param options The list of options to filter.
     * @param prefix  The prefix to filter options.
     * @return A list of options that start with the prefix.
     */
    public List<String> filterOptions(List<String> options, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return options;
        }

        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(lower))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gets all arguments as a single string.
     *
     * @return A single string containing all arguments.
     */
    public String getAllArgs() {
        return String.join(" ", args);
    }
}