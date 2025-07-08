package gg.nextforge.protocol;

import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class for detecting client protocol capabilities.
 * This class analyzes handshake, login, and plugin channel packets to determine
 * the version, mods, and capabilities of each client.
 */
public class ClientCapabilityDetector {

    // A map storing client information indexed by player UUID.
    private final Map<UUID, ClientInfo> clientInfo = new ConcurrentHashMap<>();

    /**
     * Processes a handshake packet to detect client information.
     * Extracts protocol version, mod loader type, and next state from the packet.
     *
     * @param playerUuid The UUID of the player.
     * @param packet     The handshake packet to process.
     */
    public void processHandshake(UUID playerUuid, PacketContainer packet) {
        if (packet.getType() != PacketType.HANDSHAKE_CLIENT_SET_PROTOCOL) {
            return;
        }

        ClientInfo info = clientInfo.computeIfAbsent(playerUuid, k -> new ClientInfo());

        // Extract protocol version from the packet.
        info.protocolVersion = packet.getIntegers().read(0);

        // Detect mod loader type based on server address.
        String serverAddress = packet.getStrings().read(0);
        if (serverAddress.contains("\0FML\0")) {
            info.modded = true;
            info.modLoader = "Forge";
        } else if (serverAddress.contains("fabric")) {
            info.modded = true;
            info.modLoader = "Fabric";
        }

        // Extract the next state (1=status, 2=login).
        info.nextState = packet.getIntegers().read(1);
    }

    /**
     * Processes login packets to gather additional client information.
     * Detects custom payload channels and flags suspicious clients.
     *
     * @param playerUuid The UUID of the player.
     * @param packet     The login packet to process.
     */
    public void processLogin(UUID playerUuid, PacketContainer packet) {
        ClientInfo info = clientInfo.get(playerUuid);
        if (info == null) return;

        if (packet.getType() == PacketType.LOGIN_CLIENT_CUSTOM_PAYLOAD) {
            // Extract custom payload channel name.
            String channel = packet.getStrings().read(0);
            info.customChannels.add(channel);

            // Flag suspicious clients based on known hacked client signatures.
            if (channel.contains("wdl") || channel.contains("5zig") ||
                    channel.contains("liteloader")) {
                info.suspiciousClient = true;
            }
        }
    }

    /**
     * Processes plugin channel registrations to detect registered channels.
     *
     * @param playerUuid The UUID of the player.
     * @param packet     The plugin channel registration packet to process.
     */
    public void processChannelRegistration(UUID playerUuid, PacketContainer packet) {
        ClientInfo info = clientInfo.get(playerUuid);
        if (info == null) return;

        if (packet.getType() == PacketType.PLAY_CLIENT_CUSTOM_PAYLOAD) {
            String channel = packet.getStrings().read(0);
            if (channel.equals("minecraft:register") || channel.equals("REGISTER")) {
                // Parse registered channels from the packet data.
                byte[] data = packet.getByteArrays().read(0);
                String channels = new String(data);
                info.registeredChannels.addAll(Arrays.asList(channels.split("\0")));
            }
        }
    }

    /**
     * Marks compression as enabled for a client and sets the compression threshold.
     *
     * @param playerUuid The UUID of the player.
     * @param threshold  The compression threshold.
     */
    public void compressionEnabled(UUID playerUuid, int threshold) {
        ClientInfo info = clientInfo.get(playerUuid);
        if (info != null) {
            info.compressionEnabled = true;
            info.compressionThreshold = threshold;
        }
    }

    /**
     * Retrieves the client information for a specific player.
     *
     * @param playerUuid The UUID of the player.
     * @return The client information, or null if not found.
     */
    public ClientInfo getClientInfo(UUID playerUuid) {
        return clientInfo.get(playerUuid);
    }

    /**
     * Removes a player's information from the detector when they disconnect.
     *
     * @param playerUuid The UUID of the player to remove.
     */
    public void removePlayer(UUID playerUuid) {
        clientInfo.remove(playerUuid);
    }

    /**
     * A class representing client information gathered by the detector.
     * Stores protocol version, mod loader type, compression settings, and other details.
     */
    public static class ClientInfo {
        public int protocolVersion = -1; // The protocol version of the client.
        public int nextState = -1; // The next state (1=status, 2=login).
        public boolean modded = false; // Whether the client is modded.
        public String modLoader = "Vanilla"; // The mod loader type (e.g., Forge, Fabric).
        public boolean compressionEnabled = false; // Whether compression is enabled.
        public int compressionThreshold = -1; // The compression threshold.
        public boolean suspiciousClient = false; // Whether the client is flagged as suspicious.
        public final Set<String> customChannels = new HashSet<>(); // Custom payload channels.
        public final Set<String> registeredChannels = new HashSet<>(); // Registered plugin channels.

        /**
         * Retrieves the Minecraft version corresponding to the protocol version.
         *
         * @return The Minecraft version as a string, or "Unknown" if not recognized.
         */
        public String getMinecraftVersion() {
            // Map protocol versions to Minecraft versions.
            return PROTOCOL_VERSION_MAP.getOrDefault(protocolVersion, "Unknown (" + protocolVersion + ")");
        }

        private static final Map<Integer, String> PROTOCOL_VERSION_MAP = Map.ofEntries(
            Map.entry(772, "1.21.7"),
            Map.entry(771, "1.21.6"),
            Map.entry(770, "1.21.5"),
            Map.entry(769, "1.21.4"),
            Map.entry(768, "1.21.3 / 1.21.2"),
            Map.entry(767, "1.21.1 / 1.21"),
            Map.entry(766, "1.20.6 / 1.20.5"),
            Map.entry(765, "1.20.4 / 1.20.3"),
            Map.entry(764, "1.20.2"),
            Map.entry(763, "1.20.1 / 1.20"),
            Map.entry(762, "1.19.4"),
            Map.entry(761, "1.19.3"),
            Map.entry(760, "1.19.2"),
            Map.entry(759, "1.19")
        );
    }
}