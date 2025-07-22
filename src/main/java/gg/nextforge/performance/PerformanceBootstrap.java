package gg.nextforge.performance;

import gg.nextforge.performance.listener.TickListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Initializes and wires up performance monitoring components.
 */
public class PerformanceBootstrap {

    private final Plugin plugin;

    public PerformanceBootstrap(Plugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        Bukkit.getPluginManager().registerEvents(new TickListener(plugin), plugin);
        plugin.getLogger().info("[Performance] TickListener enabled.");
    }

    public void disable() {
        PerformanceRegistry.clearAll();
        plugin.getLogger().info("[Performance] Registry cleared.");
    }
}
