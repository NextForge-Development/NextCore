package gg.nextforge.protocol;

import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Tracks packet history for debugging purposes.
 * <p>
 * This class acts as a circular buffer to store incoming and outgoing packets,
 * allowing developers to analyze packet flow and debug issues such as lag or
 * unexpected behavior. It maintains a limited history to avoid excessive memory usage.
 */
public class PacketHistory {

    private final int maxSize; // Maximum number of packets to store in history.
    private final Deque<PacketEntry> incoming; // History of incoming packets.
    private final Deque<PacketEntry> outgoing; // History of outgoing packets.

    /**
     * Constructs a PacketHistory instance with a specified maximum size.
     *
     * @param maxSize The maximum number of packets to store in history.
     */
    public PacketHistory(int maxSize) {
        this.maxSize = maxSize;
        this.incoming = new ConcurrentLinkedDeque<>();
        this.outgoing = new ConcurrentLinkedDeque<>();
    }

    /**
     * Adds an incoming packet to the history.
     * Removes the oldest packet if the history exceeds the maximum size.
     *
     * @param packet The incoming packet to add.
     */
    public void addIncoming(PacketContainer packet) {
        incoming.addLast(new PacketEntry(packet, System.currentTimeMillis()));

        // Ensure the history does not exceed the maximum size.
        while (incoming.size() > maxSize) {
            incoming.removeFirst();
        }
    }

    /**
     * Adds an outgoing packet to the history.
     * Removes the oldest packet if the history exceeds the maximum size.
     *
     * @param packet The outgoing packet to add.
     */
    public void addOutgoing(PacketContainer packet) {
        outgoing.addLast(new PacketEntry(packet, System.currentTimeMillis()));

        // Ensure the history does not exceed the maximum size.
        while (outgoing.size() > maxSize) {
            outgoing.removeFirst();
        }
    }

    /**
     * Retrieves the most recent incoming packets, up to the specified limit.
     * The packets are returned in chronological order (oldest first).
     *
     * @param limit The maximum number of packets to retrieve.
     * @return A list of recent incoming packets.
     */
    public List<PacketEntry> getIncoming(int limit) {
        List<PacketEntry> result = new ArrayList<>();
        Iterator<PacketEntry> it = incoming.descendingIterator();

        int count = 0;
        while (it.hasNext() && count++ < limit) {
            result.add(0, it.next()); // Add to the front to maintain order.
        }

        return result;
    }

    /**
     * Retrieves the most recent outgoing packets, up to the specified limit.
     * The packets are returned in chronological order (oldest first).
     *
     * @param limit The maximum number of packets to retrieve.
     * @return A list of recent outgoing packets.
     */
    public List<PacketEntry> getOutgoing(int limit) {
        List<PacketEntry> result = new ArrayList<>();
        Iterator<PacketEntry> it = outgoing.descendingIterator();

        int count = 0;
        while (it.hasNext() && count++ < limit) {
            result.add(0, it.next());
        }

        return result;
    }

    /**
     * Clears all packet history, removing both incoming and outgoing packets.
     */
    public void clear() {
        incoming.clear();
        outgoing.clear();
    }

    /**
     * Represents a single packet entry with a timestamp and summary.
     * This class is immutable for thread safety.
     */
    public static class PacketEntry {
        private final PacketType type; // The type of the packet.
        private final long timestamp; // The timestamp when the packet was recorded.
        private final String summary; // A summary of the packet's contents.

        /**
         * Constructs a PacketEntry instance.
         *
         * @param packet    The packet to record.
         * @param timestamp The timestamp when the packet was recorded.
         */
        PacketEntry(PacketContainer packet, long timestamp) {
            this.type = packet.getType();
            this.timestamp = timestamp;
            this.summary = generateSummary(packet);
        }

        /**
         * Generates a summary of the packet for debugging purposes.
         * Includes the first few fields of the packet for quick analysis.
         *
         * @param packet The packet to summarize.
         * @return A string summary of the packet.
         */
        private String generateSummary(PacketContainer packet) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(type.name()).append(" [");

                // Include integer fields in the summary.
                if (packet.getIntegers().size() > 0) {
                    sb.append("ints:").append(packet.getIntegers().getValues()).append(" ");
                }

                // Include string fields in the summary.
                if (packet.getStrings().size() > 0) {
                    sb.append("strings:").append(packet.getStrings().getValues()).append(" ");
                }

                sb.append("]");
                return sb.toString();
            } catch (Exception e) {
                return type.name() + " [error reading]";
            }
        }

        /**
         * Retrieves the type of the packet.
         *
         * @return The packet type.
         */
        public PacketType getType() {
            return type;
        }

        /**
         * Retrieves the timestamp of the packet.
         *
         * @return The timestamp in milliseconds.
         */
        public long getTimestamp() {
            return timestamp;
        }

        /**
         * Retrieves the summary of the packet.
         *
         * @return The packet summary.
         */
        public String getSummary() {
            return summary;
        }
    }
}