package gg.nextforge.protocol.listener;

import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;
import gg.nextforge.protocol.ProtocolManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages all packet listeners in the protocol system.<br>
 * <br>
 * This class keeps track of registered packet listeners, organizes them by priority,<br>
 * and ensures they are called in the correct order when handling incoming or outgoing packets.<br>
 * <br>
 * Features:<br>
 * - Thread-safe management of listeners<br>
 * - Priority-based listener execution<br>
 * - Efficient packet type lookup caching<br>
 */
public class PacketListenerManager {

    private final ProtocolManager manager;

    // Listeners sorted by priority
    private final Map<ListenerPriority, List<PacketListener>> listenersByPriority =
            new ConcurrentHashMap<>();

    // Quick lookup for packet types
    private final Map<PacketType, List<PacketListener>> sendingListeners =
            new ConcurrentHashMap<>();
    private final Map<PacketType, List<PacketListener>> receivingListeners =
            new ConcurrentHashMap<>();

    /**
     * Constructs a PacketListenerManager instance.
     *
     * @param manager The ProtocolManager instance associated with this manager.
     */
    public PacketListenerManager(ProtocolManager manager) {
        this.manager = manager;

        // Initialize priority lists
        for (ListenerPriority priority : ListenerPriority.values()) {
            listenersByPriority.put(priority, new CopyOnWriteArrayList<>());
        }
    }

    /**
     * Registers a packet listener.
     * <p>
     * This method adds the listener to the appropriate priority list and rebuilds
     * the packet type lookup cache.
     *
     * @param listener The PacketListener to register.
     */
    public void register(PacketListener listener) {
        listenersByPriority.get(listener.getPriority()).add(listener);
        rebuildCache();
    }

    /**
     * Unregisters a packet listener.
     * <p>
     * This method removes the listener from the priority list and rebuilds
     * the packet type lookup cache.
     *
     * @param listener The PacketListener to unregister.
     */
    public void unregister(PacketListener listener) {
        listenersByPriority.get(listener.getPriority()).remove(listener);
        rebuildCache();
    }

    /**
     * Clears all registered listeners.
     * <p>
     * This method removes all listeners and clears the packet type lookup caches.
     */
    public void clear() {
        listenersByPriority.values().forEach(List::clear);
        sendingListeners.clear();
        receivingListeners.clear();
    }

    /**
     * Retrieves listeners by priority.
     *
     * @param priority The ListenerPriority to filter by.
     * @return A list of PacketListeners with the specified priority.
     */
    public List<PacketListener> getListenersByPriority(ListenerPriority priority) {
        return new ArrayList<>(listenersByPriority.get(priority));
    }

    /**
     * Gets the total number of registered listeners.
     *
     * @return The total count of listeners.
     */
    public int getTotalListeners() {
        return listenersByPriority.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Rebuilds the packet type lookup cache.
     * <p>
     * This method updates the sending and receiving listener caches based on
     * the registered listeners and their packet type interests.
     */
    private void rebuildCache() {
        sendingListeners.clear();
        receivingListeners.clear();

        // Go through all listeners by priority order
        for (ListenerPriority priority : ListenerPriority.values()) {
            for (PacketListener listener : listenersByPriority.get(priority)) {
                // Add to sending cache
                for (PacketType type : PacketType.values()) {
                    if (listener.isListeningForSending(type)) {
                        sendingListeners.computeIfAbsent(type, k -> new ArrayList<>())
                                .add(listener);
                    }
                    if (listener.isListeningForReceiving(type)) {
                        receivingListeners.computeIfAbsent(type, k -> new ArrayList<>())
                                .add(listener);
                    }
                }
            }
        }
    }

    /**
     * Handles an incoming packet from a player.
     * <p>
     * This method processes the packet through all relevant listeners in priority order.
     * If any listener cancels the packet, processing stops.
     *
     * @param player The player sending the packet.
     * @param packet The PacketContainer representing the packet.
     * @return true to allow the packet, false to cancel it.
     */
    public boolean handleIncoming(Player player, PacketContainer packet) {
        List<PacketListener> listeners = receivingListeners.get(packet.getType());
        if (listeners == null || listeners.isEmpty()) {
            return true; // No listeners for this packet type
        }

        // Call listeners in order
        for (PacketListener listener : listeners) {
            try {
                if (!listener.onPacketReceiving(player, packet)) {
                    return false; // Packet cancelled
                }
            } catch (Exception e) {
                // Log listener errors and continue
                manager.getPlugin().getLogger().severe(
                        "Error in packet listener " + listener.getClass().getName() +
                                ": " + e.getMessage()
                );
                e.printStackTrace();
            }
        }

        return true;
    }

    /**
     * Handles an outgoing packet to a player.
     * <p>
     * This method processes the packet through all relevant listeners in priority order.
     * If any listener cancels the packet, processing stops.
     *
     * @param player The player receiving the packet.
     * @param packet The PacketContainer representing the packet.
     * @return true to allow the packet, false to cancel it.
     */
    public boolean handleOutgoing(Player player, PacketContainer packet) {
        List<PacketListener> listeners = sendingListeners.get(packet.getType());
        if (listeners == null || listeners.isEmpty()) {
            return true; // No listeners for this packet type
        }

        // Call listeners in order
        for (PacketListener listener : listeners) {
            try {
                if (!listener.onPacketSending(player, packet)) {
                    return false; // Packet cancelled
                }
            } catch (Exception e) {
                // Log listener errors and continue
                manager.getPlugin().getLogger().severe(
                        "Error in packet listener " + listener.getClass().getName() +
                                ": " + e.getMessage()
                );
                e.printStackTrace();
            }
        }

        return true;
    }
}