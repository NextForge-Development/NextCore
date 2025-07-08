package gg.nextforge.protocol.listener;

import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;
import org.bukkit.plugin.Plugin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Convenience adapter for packet listeners.
 * <p>
 * This class simplifies the implementation of packet listeners by allowing developers
 * to override only the methods they need. It provides multiple constructors for different
 * configurations and a builder for creating adapters with custom behavior.
 */
public abstract class PacketAdapter extends PacketListener {

    /**
     * Creates a PacketAdapter for specific packet types.
     *
     * @param plugin   The plugin instance.
     * @param priority The priority of the listener.
     * @param types    The packet types to listen for (both sending and receiving).
     */
    public PacketAdapter(Plugin plugin, ListenerPriority priority, PacketType... types) {
        this(plugin, priority, new HashSet<>(Arrays.asList(types)));
    }

    /**
     * Creates a PacketAdapter with separate sending and receiving types.
     *
     * @param plugin   The plugin instance.
     * @param priority The priority of the listener.
     * @param types    The packet types to listen for (both sending and receiving).
     */
    public PacketAdapter(Plugin plugin, ListenerPriority priority, Set<PacketType> types) {
        super(plugin, priority, types, types);
    }

    /**
     * Creates a PacketAdapter with fully custom sending and receiving type sets.
     *
     * @param plugin         The plugin instance.
     * @param priority       The priority of the listener.
     * @param sendingTypes   The packet types to listen for when sending.
     * @param receivingTypes The packet types to listen for when receiving.
     */
    public PacketAdapter(Plugin plugin, ListenerPriority priority,
                         Set<PacketType> sendingTypes, Set<PacketType> receivingTypes) {
        super(plugin, priority, sendingTypes, receivingTypes);
    }

    /**
     * Creates a builder for constructing PacketAdapter instances.
     *
     * @param plugin The plugin instance.
     * @return A new Builder instance.
     */
    public static Builder builder(Plugin plugin) {
        return new Builder(plugin);
    }

    /**
     * Functional interface for handling packets.
     * Allows the use of lambda expressions for defining packet behavior.
     */
    @FunctionalInterface
    public interface PacketHandler {
        /**
         * Handles a packet.
         *
         * @param player The player associated with the packet.
         * @param packet The packet being handled.
         * @return true to allow the packet, false to cancel it.
         */
        boolean handle(org.bukkit.entity.Player player, PacketContainer packet);
    }

    /**
     * Builder class for creating PacketAdapter instances with custom configurations.
     */
    public static class Builder {
        private final Plugin plugin;
        private final Set<PacketType> sendingTypes = new HashSet<>();
        private final Set<PacketType> receivingTypes = new HashSet<>();
        private ListenerPriority priority = ListenerPriority.NORMAL;
        private PacketHandler sendingHandler = null;
        private PacketHandler receivingHandler = null;

        /**
         * Constructs a Builder instance.
         *
         * @param plugin The plugin instance.
         */
        Builder(Plugin plugin) {
            this.plugin = plugin;
        }

        /**
         * Sets the priority of the listener.
         *
         * @param priority The listener priority.
         * @return The current Builder instance for chaining.
         */
        public Builder priority(ListenerPriority priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Adds packet types to listen for when sending.
         *
         * @param types The packet types to add.
         * @return The current Builder instance for chaining.
         */
        public Builder sending(PacketType... types) {
            sendingTypes.addAll(Arrays.asList(types));
            return this;
        }

        /**
         * Adds packet types to listen for when receiving.
         *
         * @param types The packet types to add.
         * @return The current Builder instance for chaining.
         */
        public Builder receiving(PacketType... types) {
            receivingTypes.addAll(Arrays.asList(types));
            return this;
        }

        /**
         * Sets the handler for outgoing packets.
         *
         * @param handler The PacketHandler instance.
         * @return The current Builder instance for chaining.
         */
        public Builder onSending(PacketHandler handler) {
            this.sendingHandler = handler;
            return this;
        }

        /**
         * Sets the handler for incoming packets.
         *
         * @param handler The PacketHandler instance.
         * @return The current Builder instance for chaining.
         */
        public Builder onReceiving(PacketHandler handler) {
            this.receivingHandler = handler;
            return this;
        }

        /**
         * Builds and returns a PacketAdapter instance.
         *
         * @return A new PacketAdapter instance.
         */
        public PacketAdapter build() {
            return new PacketAdapter(plugin, priority, sendingTypes, receivingTypes) {
                @Override
                public boolean onPacketSending(org.bukkit.entity.Player player,
                                               PacketContainer packet) {
                    return sendingHandler == null || sendingHandler.handle(player, packet);
                }

                @Override
                public boolean onPacketReceiving(org.bukkit.entity.Player player,
                                                 PacketContainer packet) {
                    return receivingHandler == null || receivingHandler.handle(player, packet);
                }
            };
        }
    }
}