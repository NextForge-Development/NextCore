package gg.nextforge.protocol.listener;

import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;

/**
 * Base class for packet listeners.<br>
 * <br>
 * This abstract class provides a framework for listening to packets in a Bukkit plugin.<br>
 * It includes methods for handling incoming and outgoing packets, filtering packet types,<br>
 * and managing the listener's enabled state.<br>
 * <br>
 * Features:<br>
 * - Packet filtering for specific types<br>
 * - Thread-safe packet handling<br>
 * - Hot-reloadable listener support<br>
 */
public abstract class PacketListener implements HotReloadable {

    /**
     * -- GETTER --
     *  Gets the plugin instance associated with this listener.
     *
     * @return The plugin instance.
     */
    @Getter
    private final Plugin plugin; // The plugin instance associated with this listener
    /**
     * -- GETTER --
     *  Gets the priority of the listener.
     *
     * @return The listener priority.
     */
    @Getter
    private final ListenerPriority priority; // The priority of the listener
    private final Set<PacketType> sendingTypes; // Packet types to listen for when sending
    private final Set<PacketType> receivingTypes; // Packet types to listen for when receiving
    private boolean enabled = true; // Whether the listener is enabled

    /**
     * Constructs a PacketListener instance.
     *
     * @param plugin         The plugin instance.
     * @param priority       The priority of the listener.
     * @param sendingTypes   The packet types to listen for when sending.
     * @param receivingTypes The packet types to listen for when receiving.
     */
    public PacketListener(Plugin plugin, ListenerPriority priority,
                          Set<PacketType> sendingTypes, Set<PacketType> receivingTypes) {
        this.plugin = plugin;
        this.priority = priority;
        this.sendingTypes = sendingTypes;
        this.receivingTypes = receivingTypes;
    }

    /**
     * Called when a packet is being sent to a player.
     * Return false to cancel the packet.
     * <p>
     * This method runs on Netty's thread pool. Avoid blocking operations and direct Bukkit API calls.
     *
     * @param player The player receiving the packet.
     * @param packet The packet being sent.
     * @return true to allow the packet, false to cancel it.
     */
    public boolean onPacketSending(Player player, PacketContainer packet) {
        if (!shouldProcess()) return true;
        return true;
    }

    /**
     * Called when a packet is received from a player.
     * Return false to cancel the packet.
     * <p>
     * This method runs on Netty's thread pool. Avoid blocking operations and direct Bukkit API calls.
     *
     * @param player The player sending the packet.
     * @param packet The packet being received.
     * @return true to allow the packet, false to cancel it.
     */
    public boolean onPacketReceiving(Player player, PacketContainer packet) {
        if (!shouldProcess()) return true;
        return true;
    }

    /**
     * Checks if this listener is interested in a specific outgoing packet type.
     *
     * @param type The packet type to check.
     * @return true if the listener is interested, false otherwise.
     */
    public boolean isListeningForSending(PacketType type) {
        return sendingTypes.isEmpty() || sendingTypes.contains(type);
    }

    /**
     * Checks if this listener is interested in a specific incoming packet type.
     *
     * @param type The packet type to check.
     * @return true if the listener is interested, false otherwise.
     */
    public boolean isListeningForReceiving(PacketType type) {
        return receivingTypes.isEmpty() || receivingTypes.contains(type);
    }

    /**
     * Enables the listener.
     */
    @Override
    public void enable() {
        this.enabled = true;
    }

    /**
     * Disables the listener.
     */
    @Override
    public void disable() {
        this.enabled = false;
    }

    /**
     * Reloads the listener.
     * Override this method in subclasses if additional reload behavior is needed.
     */
    @Override
    public void reload() {
        // Override in subclasses if needed
    }

    /**
     * Checks if the listener is enabled.
     *
     * @return true if the listener is enabled, false otherwise.
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Internal check for the enabled state.
     * Called before processing packets.
     *
     * @return true if the listener is enabled, false otherwise.
     */
    protected final boolean shouldProcess() {
        return enabled;
    }
}