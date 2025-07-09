package gg.nextforge.text;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TextManager is responsible for handling text formatting, parsing, and placeholder replacement.
 * It utilizes the Adventure API for modern text handling and supports legacy color codes for compatibility.
 */
public class TextManager {

    private final Plugin plugin; // The plugin instance
    private final MiniMessage miniMessage; // MiniMessage parser for modern text formatting
    private final Map<String, Function<Player, String>> placeholders = new HashMap<>(); // Placeholder resolvers
    private final Pattern placeholderPattern = Pattern.compile("%([^%]+)%"); // Regex pattern for placeholders

    // Legacy serializer for converting Components to legacy color codes
    private final LegacyComponentSerializer legacySerializer =
            LegacyComponentSerializer.legacyAmpersand();

    /**
     * Constructs a TextManager instance.
     *
     * @param plugin The plugin instance.
     */
    public TextManager(Plugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();

        // Register default placeholders
        registerDefaultPlaceholders();
    }

    /**
     * Registers default placeholders for player, world, server, and time-related data.
     */
    private void registerDefaultPlaceholders() {
        // Player placeholders
        placeholder("player", Player::getName);
        placeholder("displayname", Player::getDisplayName);
        placeholder("uuid", p -> p.getUniqueId().toString());
        placeholder("health", p -> String.valueOf((int) p.getHealth()));
        placeholder("maxhealth", p -> String.valueOf((int) p.getMaxHealth()));
        placeholder("level", p -> String.valueOf(p.getLevel()));
        placeholder("exp", p -> String.valueOf(p.getTotalExperience()));
        placeholder("food", p -> String.valueOf(p.getFoodLevel()));

        // World placeholders
        placeholder("world", p -> p.getWorld().getName());
        placeholder("x", p -> String.valueOf(p.getLocation().getBlockX()));
        placeholder("y", p -> String.valueOf(p.getLocation().getBlockY()));
        placeholder("z", p -> String.valueOf(p.getLocation().getBlockZ()));

        // Server placeholders
        placeholder("online", p -> String.valueOf(Bukkit.getOnlinePlayers().size()));
        placeholder("maxplayers", p -> String.valueOf(Bukkit.getMaxPlayers()));
        placeholder("tps", p -> {
            // Get TPS from server
            double[] tps = Bukkit.getTPS();
            return String.format("%.1f", tps[0]);
        });

        // Time placeholders
        placeholder("time", p -> {
            long time = p.getWorld().getTime();
            int hours = (int) ((time / 1000 + 6) % 24);
            int minutes = (int) ((time % 1000) * 60 / 1000);
            return String.format("%02d:%02d", hours, minutes);
        });
    }

    /**
     * Registers a custom placeholder.
     *
     * @param key      The placeholder key.
     * @param resolver The function to resolve the placeholder value.
     */
    public void placeholder(String key, Function<Player, String> resolver) {
        placeholders.put(key.toLowerCase(), resolver);
    }

    /**
     * Parses a message with MiniMessage and placeholders.
     *
     * @param message The message containing MiniMessage tags and placeholders.
     * @param player  The player for placeholder context (can be null).
     * @return A parsed Component ready to send.
     */
    public Component parse(String message, Player player) {
        // Replace placeholders if a player is provided
        if (player != null) {
            message = replacePlaceholders(message, player);
        }

        // Parse the message using MiniMessage
        return miniMessage.deserialize(message);
    }

    /**
     * Parses a message with MiniMessage and placeholders.
     *
     * @param message The message containing MiniMessage tags and placeholders.
     * @param player  The player for placeholder context (can be null).
     * @return A parsed Component ready to send.
     */
    public Component parse(String message, Audience player) {
        // Replace placeholders if a player is provided
        if (player != null) {
            message = replacePlaceholders(message, player);
        }

        // Parse the message using MiniMessage
        return miniMessage.deserialize(message);
    }

    /**
     * Parses a message and converts it to a legacy string.
     *
     * @param message The message containing MiniMessage tags and placeholders.
     * @param player  The player for placeholder context (can be null).
     * @return A legacy string representation of the parsed message.
     */
    public String parseLegacy(String message, Player player) {
        Component component = parse(message, player);
        return legacySerializer.serialize(component);
    }

    /**
     * Parses a message with legacy color codes and MiniMessage tags.
     *
     * @param message The message containing legacy color codes and MiniMessage tags.
     * @param player  The player for placeholder context (can be null).
     * @return A parsed Component ready to send.
     */
    public Component parseMixed(String message, Player player) {
        // Convert legacy color codes to MiniMessage format
        message = convertLegacyToMiniMessage(message);

        // Parse the message using MiniMessage
        return parse(message, player);
    }

    /**
     * Sends a parsed message to a player.
     *
     * @param player  The player to send the message to.
     * @param message The message to parse and send.
     */
    public void send(Player player, String message) {
        player.sendMessage(parse(message, player));
    }

    /**
     * Sends a parsed message to a player.
     *
     * @param player  The player to send the message to.
     * @param message The message to parse and send.
     */
    public void send(Audience player, String message) {
        player.sendMessage(parse(message, player));
    }

    /**
     * Broadcasts a parsed message to all online players.
     *
     * @param message The message to parse and broadcast.
     */
    public void broadcast(String message) {
        Bukkit.getOnlinePlayers().forEach(p ->
                p.sendMessage(parse(message, p))
        );
    }

    /**
     * Replaces placeholders in a message.
     *
     * @param message The message containing placeholders.
     * @param player  The player for placeholder context.
     * @return The message with placeholders replaced.
     */
    public String replacePlaceholders(String message, Player player) {
        if (player == null) return message;

        Matcher matcher = placeholderPattern.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            Function<Player, String> resolver = placeholders.get(placeholder);

            if (resolver != null) {
                try {
                    String replacement = resolver.apply(player);
                    matcher.appendReplacement(result,
                            Matcher.quoteReplacement(replacement));
                } catch (Exception e) {
                    // Use placeholder as-is if resolver fails
                    matcher.appendReplacement(result, matcher.group(0));
                }
            } else {
                // Leave unknown placeholders as-is
                matcher.appendReplacement(result, matcher.group(0));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Replaces placeholders in a message.
     *
     * @param message The message containing placeholders.
     * @param player  The player for placeholder context.
     * @return The message with placeholders replaced.
     */
    public String replacePlaceholders(String message, Audience player) {
        if (player == null) return message;

        Matcher matcher = placeholderPattern.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            Function<Player, String> resolver = placeholders.get(placeholder);

            if (resolver != null) {
                try {
                    String replacement = resolver.apply(null);
                    matcher.appendReplacement(result,
                            Matcher.quoteReplacement(replacement));
                } catch (Exception e) {
                    // Use placeholder as-is if resolver fails
                    matcher.appendReplacement(result, matcher.group(0));
                }
            } else {
                // Leave unknown placeholders as-is
                matcher.appendReplacement(result, matcher.group(0));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Converts legacy color codes to MiniMessage format.
     *
     * @param message The message containing legacy color codes.
     * @return The message converted to MiniMessage format.
     */
    private String convertLegacyToMiniMessage(String message) {
        // Simplified conversion of legacy color codes to MiniMessage tags
        message = message.replace("&0", "<black>");
        message = message.replace("&1", "<dark_blue>");
        message = message.replace("&2", "<dark_green>");
        message = message.replace("&3", "<dark_aqua>");
        message = message.replace("&4", "<dark_red>");
        message = message.replace("&5", "<dark_purple>");
        message = message.replace("&6", "<gold>");
        message = message.replace("&7", "<gray>");
        message = message.replace("&8", "<dark_gray>");
        message = message.replace("&9", "<blue>");
        message = message.replace("&a", "<green>");
        message = message.replace("&b", "<aqua>");
        message = message.replace("&c", "<red>");
        message = message.replace("&d", "<light_purple>");
        message = message.replace("&e", "<yellow>");
        message = message.replace("&f", "<white>");

        message = message.replace("&k", "<obfuscated>");
        message = message.replace("&l", "<bold>");
        message = message.replace("&m", "<strikethrough>");
        message = message.replace("&n", "<underlined>");
        message = message.replace("&o", "<italic>");
        message = message.replace("&r", "<reset>");

        return message;
    }

    /**
     * Strips all formatting from a message.
     *
     * @param message The message containing formatting.
     * @return The plain text representation of the message.
     */
    public String stripFormatting(String message) {
        // Parse the message and convert it to plain text
        Component component = miniMessage.deserialize(message);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Colorizes a string with legacy color codes and hex colors.
     *
     * @param text The text to colorize.
     * @return The colorized text.
     */
    public String colorize(String text) {
        if (text == null) return null;

        // Convert & codes to §
        text = ChatColor.translateAlternateColorCodes('&', text);

        // Handle hex colors like &#FF0000
        text = text.replaceAll("&#([A-Fa-f0-9]{6})", "§x§$1");

        // Handle spaced hex like &x&F&F&0&0&0&0
        text = text.replaceAll("&x&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])",
                "§x§$1§$2§$3§$4§$5§$6");

        return text;
    }

    /**
     * Creates a blank Component builder for advanced formatting.
     *
     * @return A blank Component.
     */
    public Component componentBuilder() {
        return Component.text("");
    }

    /**
     * Parses a string into a Component.
     *
     * @param text The text to parse.
     * @return The parsed Component.
     */
    public Component parseComponent(String text) {
        return miniMessage.deserialize(text);
    }

    /**
     * Serializes a Component to MiniMessage format.
     *
     * @param component The Component to serialize.
     * @return The MiniMessage string representation of the Component.
     */
    public String serializeComponent(Component component) {
        return miniMessage.serialize(component);
    }
}