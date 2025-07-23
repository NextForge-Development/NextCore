package gg.nextforge.debug;

import gg.nextforge.NextCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Utility for printing debug messages to players or console.
 */
public class DebugPrinter {

    public static void print(Player player, String message) {
        if (DebugManager.isEnabled(player)) {
            player.sendMessage(format(message));
        }
    }

    public static void console(String message) {
        NextCorePlugin.getInstance().getComponentLogger().info(format(message));
    }

    public static void broadcast(String message) {
        Component msg = format(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (DebugManager.isEnabled(player)) {
                player.sendMessage(msg);
            }
        }
    }

    public static Component format(String message) {
        return Component.text()
                .append(Component.text("[Debug] ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.YELLOW))
                .build();
    }

    public static void printRaw(Player player, Component component) {
        if (DebugManager.isEnabled(player)) {
            player.sendMessage(component);
        }
    }
}
