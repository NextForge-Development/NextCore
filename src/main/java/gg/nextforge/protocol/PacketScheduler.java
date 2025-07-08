package gg.nextforge.protocol;

import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.scheduler.CoreScheduler;
import gg.nextforge.scheduler.ScheduledTask;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Scheduled packet injector.
 *
 * This class provides functionality for scheduling packets to be sent to players
 * with delays, repeating intervals, or as bundles. It uses the server scheduler
 * for timing operations.
 */
public class PacketScheduler {

    private final ProtocolManager protocol;

    // Queued packets waiting to be sent
    private final Queue<ScheduledPacket> queue = new ConcurrentLinkedQueue<>();

    // Packet bundles - sent together in one tick
    private final Map<String, PacketBundle> bundles = new HashMap<>();

    // Active tasks for cancellation
    private final Map<Integer, ScheduledTask> activeTasks = new HashMap<>();
    private final Map<Integer, RepeatTracker> repeatTrackers = new HashMap<>();

    /**
     * Constructs a PacketScheduler instance.
     *
     * @param protocol The ProtocolManager used to send packets.
     */
    public PacketScheduler(ProtocolManager protocol) {
        this.protocol = protocol;

        // Process the packet queue every tick
        CoreScheduler.runTimer(this::processQueue, 1L, 1L);
    }

    /**
     * Sends a packet to a player after a specified delay.
     *
     * @param player The player to send the packet to.
     * @param packet The packet to send.
     * @param delay  The delay in ticks (20 ticks = 1 second).
     */
    public void sendLater(Player player, PacketContainer packet, long delay) {
        queue.offer(new ScheduledPacket(
                player,
                packet,
                System.currentTimeMillis() + (delay * 50),
                false,
                0
        ));
    }

    /**
     * Sends a packet to a player repeatedly.
     *
     * @param player The player to send the packet to.
     * @param packet The packet to send.
     * @param delay  The initial delay in ticks.
     * @param period The repeat period in ticks.
     * @param count  The number of times to send the packet (0 = infinite).
     * @return The task ID for cancellation.
     */
    public int sendRepeating(Player player, PacketContainer packet,
                             long delay, long period, int count) {
        int taskId = new Random().nextInt(Integer.MAX_VALUE);

        var task = CoreScheduler.runTimer(() -> {
            if (!player.isOnline()) {
                cancel(taskId); // Cancel if player leaves
                return;
            }

            protocol.sendPacket(player, packet);

            // Track count and cancel when done
            if (count > 0) {
                RepeatTracker tracker = repeatTrackers.computeIfAbsent(
                        taskId, k -> new RepeatTracker(count)
                );
                tracker.sent++;

                if (tracker.sent >= tracker.total) {
                    cancel(taskId);
                }
            }
        }, delay, period);

        activeTasks.put(taskId, task);
        return taskId;
    }

    /**
     * Cancels a scheduled packet task.
     *
     * @param taskId The ID of the task to cancel.
     */
    public void cancel(int taskId) {
        var task = activeTasks.remove(taskId);
        if (task != null) {
            task.cancel();
        }
        repeatTrackers.remove(taskId);
    }

    /**
     * Cancels all scheduled packets for a specific player.
     *
     * @param player The player whose scheduled packets should be canceled.
     */
    public void cancelAll(Player player) {
        // Remove packets from the queue
        queue.removeIf(p -> p.player.equals(player));

        // Note: Active tasks for this player cannot be easily tracked
        // without additional tracking mechanisms.
    }

    /**
     * Creates a packet bundle.
     * Bundles allow multiple packets to be sent together in the same tick.
     *
     * @param name The name of the bundle.
     * @return The created PacketBundle instance.
     */
    public PacketBundle createBundle(String name) {
        PacketBundle bundle = new PacketBundle(name);
        bundles.put(name, bundle);
        return bundle;
    }

    /**
     * Sends a packet bundle to specified players.
     *
     * @param bundleName The name of the bundle to send.
     * @param players    The players to send the bundle to.
     */
    public void sendBundle(String bundleName, Player... players) {
        PacketBundle bundle = bundles.get(bundleName);
        if (bundle == null) {
            throw new IllegalArgumentException("Unknown bundle: " + bundleName);
        }

        // Send all packets in rapid succession
        for (Player player : players) {
            for (PacketContainer packet : bundle.packets) {
                protocol.sendPacket(player, packet);
            }
        }
    }

    /**
     * Sends a packet bundle to specified players after a delay.
     *
     * @param bundleName The name of the bundle to send.
     * @param delay      The delay in ticks before sending the bundle.
     * @param players    The players to send the bundle to.
     */
    public void sendBundleLater(String bundleName, long delay, Player... players) {
        PacketBundle bundle = bundles.get(bundleName);
        if (bundle == null) {
            throw new IllegalArgumentException("Unknown bundle: " + bundleName);
        }

        CoreScheduler.runLater(() -> {
            sendBundle(bundleName, players);
        }, delay);
    }

    /**
     * Processes the packet queue.
     * This method is called every tick to check and send packets that are due.
     */
    private void processQueue() {
        long now = System.currentTimeMillis();

        Iterator<ScheduledPacket> it = queue.iterator();
        while (it.hasNext()) {
            ScheduledPacket scheduled = it.next();

            if (scheduled.sendTime <= now) {
                // Time to send this packet
                if (scheduled.player.isOnline()) {
                    protocol.sendPacket(scheduled.player, scheduled.packet);
                }

                it.remove();
            }
        }
    }

    /**
     * Clears all scheduled packets and bundles.
     * This method also cancels all active tasks.
     */
    public void clear() {
        queue.clear();
        bundles.clear();

        // Cancel all active tasks
        for (var task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        repeatTrackers.clear();
    }

    /**
     * Represents a scheduled packet entry.
     */
    private static class ScheduledPacket {
        final Player player;
        final PacketContainer packet;
        final long sendTime;
        final boolean repeat;
        final long period;

        /**
         * Constructs a ScheduledPacket instance.
         *
         * @param player   The player to send the packet to.
         * @param packet   The packet to send.
         * @param sendTime The time (in milliseconds) when the packet should be sent.
         * @param repeat   Whether the packet is part of a repeating task.
         * @param period   The repeat period in ticks (if applicable).
         */
        ScheduledPacket(Player player, PacketContainer packet, long sendTime,
                        boolean repeat, long period) {
            this.player = player;
            this.packet = packet;
            this.sendTime = sendTime;
            this.repeat = repeat;
            this.period = period;
        }
    }

    /**
     * Tracks the count of packets sent in a repeating task.
     */
    private static class RepeatTracker {
        final int total;
        int sent = 0;

        /**
         * Constructs a RepeatTracker instance.
         *
         * @param total The total number of packets to send.
         */
        RepeatTracker(int total) {
            this.total = total;
        }
    }

    /**
     * Represents a packet bundle - multiple packets sent together.
     * Useful for complex visual effects requiring multiple packets.
     */
    public static class PacketBundle {
        private final String name;
        private final List<PacketContainer> packets = new ArrayList<>();

        /**
         * Constructs a PacketBundle instance.
         *
         * @param name The name of the bundle.
         */
        PacketBundle(String name) {
            this.name = name;
        }

        /**
         * Adds a packet to the bundle.
         *
         * @param packet The packet to add.
         * @return The current PacketBundle instance.
         */
        public PacketBundle add(PacketContainer packet) {
            packets.add(packet);
            return this;
        }

        /**
         * Clears all packets from the bundle.
         *
         * @return The current PacketBundle instance.
         */
        public PacketBundle clear() {
            packets.clear();
            return this;
        }

        /**
         * Gets the name of the bundle.
         *
         * @return The name of the bundle.
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the number of packets in the bundle.
         *
         * @return The size of the bundle.
         */
        public int size() {
            return packets.size();
        }
    }
}