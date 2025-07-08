package gg.nextforge.protocol.export;

import gg.nextforge.protocol.packet.PacketContainer;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A class for exporting Minecraft packets in PCAP format.
 * This allows packet logs to be analyzed using tools like Wireshark.
 */
public class PCAPExporter {

    private static final int PCAP_MAGIC = 0xa1b2c3d4; // Magic number for PCAP files
    private static final short PCAP_VERSION_MAJOR = 2; // Major version of PCAP format
    private static final short PCAP_VERSION_MINOR = 4; // Minor version of PCAP format
    private static final int PCAP_SNAPLEN = 65535; // Maximum packet length
    private static final int PCAP_NETWORK = 147; // User-defined network type

    private final File outputFile; // Output file for the PCAP data
    private final DataOutputStream output; // Stream for writing PCAP data
    private final BlockingQueue<PacketEntry> queue; // Queue for storing packets to be written
    private final Thread writerThread; // Thread for writing packets to the file
    private volatile boolean running = true; // Flag to indicate if the exporter is running

    /**
     * Constructs a PCAPExporter instance and initializes the output file and writer thread.
     *
     * @param outputFile The file to write the PCAP data to.
     * @throws IOException If an error occurs while opening the file.
     */
    public PCAPExporter(File outputFile) throws IOException {
        this.outputFile = outputFile;
        this.output = new DataOutputStream(new FileOutputStream(outputFile));
        this.queue = new LinkedBlockingQueue<>();

        // Write the PCAP global header
        writePCAPHeader();

        // Start the writer thread
        this.writerThread = new Thread(this::writerLoop, "PCAP-Writer");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    /**
     * Writes the PCAP global header to the output file.
     *
     * @throws IOException If an error occurs while writing the header.
     */
    private void writePCAPHeader() throws IOException {
        output.writeInt(PCAP_MAGIC);
        output.writeShort(PCAP_VERSION_MAJOR);
        output.writeShort(PCAP_VERSION_MINOR);
        output.writeInt(0); // Timezone offset (GMT)
        output.writeInt(0); // Timestamp accuracy
        output.writeInt(PCAP_SNAPLEN); // Maximum packet length
        output.writeInt(PCAP_NETWORK); // Network type
    }

    /**
     * Adds a packet to the export queue.
     *
     * @param packet    The packet to export.
     * @param outgoing  Whether the packet is outgoing.
     * @param playerName The name of the player associated with the packet.
     */
    public void exportPacket(PacketContainer packet, boolean outgoing, String playerName) {
        try {
            byte[] data = serializePacket(packet, outgoing, playerName);
            queue.offer(new PacketEntry(System.currentTimeMillis(), data));
        } catch (Exception e) {
            // Ignore serialization errors
        }
    }

    /**
     * Serializes a packet into a byte array.
     *
     * @param packet   The packet to serialize.
     * @param outgoing Whether the packet is outgoing.
     * @param player   The name of the player associated with the packet.
     * @return A byte array representing the serialized packet.
     * @throws IOException If an error occurs during serialization.
     */
    private byte[] serializePacket(PacketContainer packet, boolean outgoing, String player)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Write custom header
        dos.writeUTF(player); // Player name
        dos.writeBoolean(outgoing); // Packet direction
        dos.writeUTF(packet.getType().name()); // Packet type

        // Write packet data
        try {
            byte[] rawData = packet.getRawBytes();
            dos.writeInt(rawData.length);
            dos.write(rawData);
        } catch (Exception e) {
            // Fallback to field data
            dos.writeInt(-1); // Indicate field data format

            // Write integer fields
            var integers = packet.getIntegers().getValues();
            dos.writeInt(integers.size());
            for (int i : integers) {
                dos.writeInt(i);
            }

            // Write string fields
            var strings = packet.getStrings().getValues();
            dos.writeInt(strings.size());
            for (String s : strings) {
                dos.writeUTF(s != null ? s : "");
            }

            // Write double fields
            var doubles = packet.getDoubles().getValues();
            dos.writeInt(doubles.size());
            for (double d : doubles) {
                dos.writeDouble(d);
            }
        }

        return baos.toByteArray();
    }

    /**
     * The main loop for the writer thread.
     * Consumes packets from the queue and writes them to the file.
     */
    private void writerLoop() {
        while (running) {
            try {
                PacketEntry entry = queue.take();
                writePacketRecord(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                // Stop the exporter on write failure
                running = false;
            }
        }
    }

    /**
     * Writes a single packet record to the output file.
     *
     * @param entry The packet entry to write.
     * @throws IOException If an error occurs while writing the record.
     */
    private void writePacketRecord(PacketEntry entry) throws IOException {
        // Write packet header
        int seconds = (int)(entry.timestamp / 1000);
        int microseconds = (int)((entry.timestamp % 1000) * 1000);

        output.writeInt(seconds);
        output.writeInt(microseconds);
        output.writeInt(entry.data.length); // Captured length
        output.writeInt(entry.data.length); // Original length

        // Write packet data
        output.write(entry.data);
        output.flush();
    }

    /**
     * Stops the exporter and closes the output file.
     */
    public void close() {
        running = false;
        writerThread.interrupt();

        try {
            writerThread.join(1000);
            output.close();
        } catch (Exception e) {
            // Ignore errors during close
        }
    }

    /**
     * Represents a packet entry with a timestamp and serialized data.
     */
    private record PacketEntry(long timestamp, byte[] data) {
    }
}