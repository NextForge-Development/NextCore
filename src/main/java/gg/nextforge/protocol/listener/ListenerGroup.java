package gg.nextforge.protocol.listener;

import gg.nextforge.plugin.NextForgePlugin;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a group of packet listeners for organizational purposes.<br>
 *<br>
 * ListenerGroup allows grouping packet listeners by feature, plugin, or other<br>
 * arbitrary categorizations. Groups can be enabled, disabled, reloaded, or cleared,<br>
 * providing flexibility in managing packet listeners.<br>
 *<br>
 * Features:<br>
 * - Grouping listeners for better organization<br>
 * - Enable/disable functionality for all listeners in a group<br>
 * - Hot-reloading support<br>
 * - Static methods for global group management<br>
 */
public class ListenerGroup {

    private final String name; // The name of the listener group
    private final Plugin plugin; // The plugin associated with this group
    private final List<PacketListener> listeners = new ArrayList<>(); // List of listeners in this group
    private boolean enabled = true; // Whether the group is enabled

    // Global registry of all listener groups
    private static final Map<String, ListenerGroup> GROUPS = new ConcurrentHashMap<>();

    /**
     * Constructs a ListenerGroup instance.
     *
     * @param name The name of the group.
     * @param plugin The plugin associated with this group.
     */
    public ListenerGroup(String name, Plugin plugin) {
        this.name = name;
        this.plugin = plugin;
        GROUPS.put(name, this);
    }

    /**
     * Adds a listener to this group.
     *
     * @param listener The PacketListener to add.
     */
    public void addListener(PacketListener listener) {
        listeners.add(listener);

        // Register the listener if the group is enabled
        if (enabled) {
            NextForgePlugin.getInstance()
                    .getProtocolManager().registerListener(listener);
        }
    }

    /**
     * Removes a listener from this group.
     *
     * @param listener The PacketListener to remove.
     */
    public void removeListener(PacketListener listener) {
        listeners.remove(listener);

        // Always unregister the listener
        NextForgePlugin.getInstance()
                .getProtocolManager().unregisterListener(listener);
    }

    /**
     * Enables this group.
     * Registers all listeners in the group.
     */
    public void enable() {
        if (enabled) return;

        enabled = true;
        var protocol = NextForgePlugin.getInstance()
                .getProtocolManager();

        listeners.forEach(protocol::registerListener);
    }

    /**
     * Disables this group.
     * Unregisters all listeners in the group.
     */
    public void disable() {
        if (!enabled) return;

        enabled = false;
        var protocol = NextForgePlugin.getInstance()
                .getProtocolManager();

        listeners.forEach(protocol::unregisterListener);
    }

    /**
     * Reloads this group.
     * Disables and then enables the group, effectively reloading it.
     */
    public void reload() {
        disable();
        enable();
    }

    /**
     * Clears all listeners in this group.
     * Disables the group and removes all listeners.
     */
    public void clear() {
        disable();
        listeners.clear();
    }

    // Static methods for global group management

    /**
     * Retrieves a ListenerGroup by its name.
     *
     * @param name The name of the group.
     * @return The ListenerGroup instance, or null if not found.
     */
    public static ListenerGroup getGroup(String name) {
        return GROUPS.get(name);
    }

    /**
     * Retrieves all ListenerGroups associated with a specific plugin.
     *
     * @param plugin The plugin to filter by.
     * @return A list of ListenerGroups associated with the plugin.
     */
    public static List<ListenerGroup> getGroups(Plugin plugin) {
        return GROUPS.values().stream()
                .filter(g -> g.plugin.equals(plugin))
                .toList();
    }

    /**
     * Disables all ListenerGroups associated with a specific plugin.
     * Typically called when the plugin is disabled.
     *
     * @param plugin The plugin whose groups should be disabled.
     */
    public static void disablePlugin(Plugin plugin) {
        getGroups(plugin).forEach(ListenerGroup::disable);
    }

    /**
     * Removes all ListenerGroups associated with a specific plugin.
     * Clears and unregisters all groups for permanent cleanup.
     *
     * @param plugin The plugin whose groups should be removed.
     */
    public static void removePlugin(Plugin plugin) {
        getGroups(plugin).forEach(group -> {
            group.clear();
            GROUPS.remove(group.name);
        });
    }

    /**
     * Gets the name of this ListenerGroup.
     *
     * @return The name of the group.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the plugin associated with this ListenerGroup.
     *
     * @return The plugin instance.
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Checks if this ListenerGroup is enabled.
     *
     * @return true if the group is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Retrieves all listeners in this ListenerGroup.
     *
     * @return An unmodifiable list of PacketListeners in the group.
     */
    public List<PacketListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }
}