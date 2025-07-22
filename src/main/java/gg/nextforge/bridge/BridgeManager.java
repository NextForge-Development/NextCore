package gg.nextforge.bridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Logger;

/**
 * Central manager for all plugin bridges.
 */
public class BridgeManager {

    private static final Logger LOGGER = Logger.getLogger("BridgeManager");

    private final Map<Class<? extends PluginBridge>, PluginBridge> bridges = new HashMap<>();

    /**
     * Registers and tries to initialize the bridge.
     */
    public void register(PluginBridge bridge) {
        String pluginName = bridge.getPluginName();

        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin != null && plugin.isEnabled()) {
            try {
                bridge.onEnable(plugin);
                bridge.setStatus(BridgeStatus.ENABLED);
                LOGGER.info("Bridge enabled: " + pluginName);
            } catch (Exception e) {
                bridge.setStatus(BridgeStatus.FAILED);
                LOGGER.warning("Failed to enable bridge for " + pluginName + ": " + e.getMessage());
            }
        } else {
            bridge.setStatus(BridgeStatus.MISSING);
            LOGGER.info("Bridge missing: " + pluginName);
        }

        bridges.put(bridge.getClass(), bridge);
    }

    /**
     * Returns true if a bridge is available and enabled.
     */
    public boolean isAvailable(String pluginName) {
        return bridges.values().stream()
                .anyMatch(b -> b.getPluginName().equalsIgnoreCase(pluginName) && b.isEnabled());
    }

    /**
     * Gets the bridge by class if registered.
     */
    @SuppressWarnings("unchecked")
    public <T extends PluginBridge> Optional<T> get(Class<T> type) {
        return Optional.ofNullable((T) bridges.get(type));
    }

    /**
     * Calls onDisable on all loaded bridges.
     */
    public void shutdown() {
        for (PluginBridge bridge : bridges.values()) {
            if (bridge.isEnabled()) {
                bridge.onDisable();
            }
        }
        bridges.clear();
    }
}
