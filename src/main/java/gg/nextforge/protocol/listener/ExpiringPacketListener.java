package gg.nextforge.protocol.listener;

import gg.nextforge.plugin.NextForgePlugin;
import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A packet listener that automatically expires after a specified time or packet count.
 *
 * This class is useful for temporary debugging or one-time packet interception.
 * It supports time-based expiration, packet count-based expiration, and custom
 * expiration callbacks.
 */
public class ExpiringPacketListener extends PacketListener {

    private final long expiryTime; // The time at which the listener expires
    private final int maxPackets; // The maximum number of packets before expiration
    private int packetCount = 0; // The current count of processed packets
    private final Runnable onExpire; // Callback to execute upon expiration

    /**
     * Constructs a time-based expiring listener.
     *
     * @param plugin The plugin instance associated with this listener.
     * @param duration The duration before the listener expires.
     * @param unit The time unit for the duration.
     * @param sendingTypes The packet types to listen for when sending.
     * @param receivingTypes The packet types to listen for when receiving.
     */
    public ExpiringPacketListener(Plugin plugin, long duration, TimeUnit unit,
                                  Set<PacketType> sendingTypes, Set<PacketType> receivingTypes) {
        super(plugin, ListenerPriority.NORMAL, sendingTypes, receivingTypes);
        this.expiryTime = System.currentTimeMillis() + unit.toMillis(duration);
        this.maxPackets = Integer.MAX_VALUE;
        this.onExpire = null;
    }

    /**
     * Constructs a count-based expiring listener.
     *
     * @param plugin The plugin instance associated with this listener.
     * @param maxPackets The maximum number of packets before the listener expires.
     * @param sendingTypes The packet types to listen for when sending.
     * @param receivingTypes The packet types to listen for when receiving.
     */
    public ExpiringPacketListener(Plugin plugin, int maxPackets,
                                  Set<PacketType> sendingTypes, Set<PacketType> receivingTypes) {
        super(plugin, ListenerPriority.NORMAL, sendingTypes, receivingTypes);
        this.expiryTime = Long.MAX_VALUE;
        this.maxPackets = maxPackets;
        this.onExpire = null;
    }

    /**
     * Constructs a listener with a custom expiration callback.
     *
     * @param plugin The plugin instance associated with this listener.
     * @param duration The duration before the listener expires.
     * @param unit The time unit for the duration.
     * @param types The packet types to listen for (both sending and receiving).
     * @param onExpire The callback to execute upon expiration.
     */
    public ExpiringPacketListener(Plugin plugin, long duration, TimeUnit unit,
                                  Set<PacketType> types, Runnable onExpire) {
        super(plugin, ListenerPriority.NORMAL, types, types);
        this.expiryTime = System.currentTimeMillis() + unit.toMillis(duration);
        this.maxPackets = Integer.MAX_VALUE;
        this.onExpire = onExpire;
    }

    /**
     * Handles outgoing packets.
     *
     * @param player The player receiving the packet.
     * @param packet The packet being sent.
     * @return true to allow the packet, false to cancel it.
     */
    @Override
    public boolean onPacketSending(Player player, PacketContainer packet) {
        if (checkExpired()) return true;

        packetCount++;
        return handlePacket(player, packet, true);
    }

    /**
     * Handles incoming packets.
     *
     * @param player The player sending the packet.
     * @param packet The packet being received.
     * @return true to allow the packet, false to cancel it.
     */
    @Override
    public boolean onPacketReceiving(Player player, PacketContainer packet) {
        if (checkExpired()) return true;

        packetCount++;
        return handlePacket(player, packet, false);
    }

    /**
     * Handles packets. Override this method to implement custom packet handling logic.
     *
     * @param player The player associated with the packet.
     * @param packet The packet being handled.
     * @param sending true if the packet is being sent, false if it is being received.
     * @return true to allow the packet, false to cancel it.
     */
    protected boolean handlePacket(Player player, PacketContainer packet, boolean sending) {
        return true;
    }

    /**
     * Checks if the listener has expired. If expired, unregisters itself and executes
     * the expiration callback if provided.
     *
     * @return true if the listener has expired, false otherwise.
     */
    private boolean checkExpired() {
        boolean expired = System.currentTimeMillis() > expiryTime ||
                packetCount >= maxPackets;

        if (expired) {
            // Unregister the listener on the main thread
            getPlugin().getServer().getScheduler().runTask(getPlugin(), () -> {
                NextForgePlugin.getInstance()
                        .getProtocolManager().unregisterListener(this);

                if (onExpire != null) {
                    onExpire.run();
                }
            });
        }

        return expired;
    }
}