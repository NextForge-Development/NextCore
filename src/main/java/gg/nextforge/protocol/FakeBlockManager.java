package gg.nextforge.protocol;

import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages fake blocks for client-side world modifications.
 * This class allows the creation, manipulation, and removal of fake blocks
 * that exist only on the client side, providing visual changes without affecting
 * the server-side world state.
 */
public class FakeBlockManager {

    private final ProtocolManager protocol; // Protocol manager for sending packets.

    // Map storing fake blocks per player, indexed by player UUID and block location.
    private final Map<UUID, Map<Location, BlockData>> playerBlocks = new ConcurrentHashMap<>();

    /**
     * Constructs a FakeBlockManager instance.
     *
     * @param protocol The protocol manager used for sending packets.
     */
    public FakeBlockManager(ProtocolManager protocol) {
        this.protocol = protocol;
    }

    /**
     * Sets a fake block for specific players.
     * The block only exists on the client side for the specified players.
     *
     * @param location The location of the fake block.
     * @param material The material of the fake block.
     * @param players  The players who will see the fake block.
     */
    public void setBlock(Location location, Material material, Player... players) {
        BlockData data = material.createBlockData();
        setBlock(location, data, players);
    }

    /**
     * Sets a fake block with specific block data for specific players.
     *
     * @param location The location of the fake block.
     * @param data     The block data of the fake block.
     * @param players  The players who will see the fake block.
     */
    public void setBlock(Location location, BlockData data, Player... players) {
        PacketContainer packet = createBlockChangePacket(location, data);

        for (Player player : players) {
            // Send the fake block packet to the player.
            protocol.sendPacket(player, packet);

            // Track the fake block for restoration later.
            playerBlocks.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                    .put(location, data);
        }
    }

    /**
     * Sets multiple fake blocks at once for specific players.
     * This method is more efficient than setting individual blocks.
     *
     * @param blocks  A map of locations to block data representing the fake blocks.
     * @param players The players who will see the fake blocks.
     */
    public void setBlocks(Map<Location, BlockData> blocks, Player... players) {
        // Group blocks by chunk for multi-block change packets.
        Map<Long, List<Map.Entry<Location, BlockData>>> byChunk = new HashMap<>();

        for (Map.Entry<Location, BlockData> entry : blocks.entrySet()) {
            Location loc = entry.getKey();
            long chunkKey = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);

            byChunk.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(entry);
        }

        // Send multi-block change packets per chunk.
        for (List<Map.Entry<Location, BlockData>> chunkBlocks : byChunk.values()) {
            if (chunkBlocks.size() == 1) {
                // Single block, use regular block change packet.
                Map.Entry<Location, BlockData> entry = chunkBlocks.get(0);
                setBlock(entry.getKey(), entry.getValue(), players);
            } else {
                // Multiple blocks in chunk, use multi-block change packet.
                PacketContainer packet = createMultiBlockChangePacket(chunkBlocks);

                for (Player player : players) {
                    protocol.sendPacket(player, packet);

                    // Track all fake blocks for restoration later.
                    Map<Location, BlockData> playerMap = playerBlocks
                            .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());

                    for (Map.Entry<Location, BlockData> entry : chunkBlocks) {
                        playerMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    /**
     * Clears all fake blocks for a specific player.
     * Sends real block data to restore the player's world view.
     *
     * @param player The player whose fake blocks should be cleared.
     */
    public void clearPlayer(Player player) {
        Map<Location, BlockData> blocks = playerBlocks.remove(player.getUniqueId());
        if (blocks == null) return;

        // Send real block data to restore the player's world view.
        for (Location loc : blocks.keySet()) {
            BlockData realData = loc.getBlock().getBlockData();
            PacketContainer packet = createBlockChangePacket(loc, realData);
            protocol.sendPacket(player, packet);
        }
    }

    /**
     * Clears fake blocks in a specific region for a specific player.
     *
     * @param player The player whose fake blocks should be cleared.
     * @param min    The minimum corner of the region.
     * @param max    The maximum corner of the region.
     */
    public void clearRegion(Player player, Location min, Location max) {
        Map<Location, BlockData> blocks = playerBlocks.get(player.getUniqueId());
        if (blocks == null) return;

        Iterator<Map.Entry<Location, BlockData>> it = blocks.entrySet().iterator();
        while (it.hasNext()) {
            Location loc = it.next().getKey();

            if (loc.getX() >= min.getX() && loc.getX() <= max.getX() &&
                    loc.getY() >= min.getY() && loc.getY() <= max.getY() &&
                    loc.getZ() >= min.getZ() && loc.getZ() <= max.getZ()) {

                // Restore the real block data.
                BlockData realData = loc.getBlock().getBlockData();
                PacketContainer packet = createBlockChangePacket(loc, realData);
                protocol.sendPacket(player, packet);

                it.remove();
            }
        }
    }

    /**
     * Creates a block change packet for a specific block.
     * This method is version-specific and may require adjustments for different Minecraft versions.
     *
     * @param location The location of the block.
     * @param data     The block data of the block.
     * @return The block change packet.
     */
    private PacketContainer createBlockChangePacket(Location location, BlockData data) {
        PacketContainer packet = PacketContainer.createPacket(PacketType.PLAY_SERVER_BLOCK_CHANGE);

        // Set the block position using a packed long format.
        long packedPos = ((long) (location.getBlockX() & 0x3FFFFFF) << 38) |
                ((long) (location.getBlockZ() & 0x3FFFFFF) << 12) |
                (location.getBlockY() & 0xFFF);

        packet.getLongs().write(0, packedPos);

        // Set the block state (block data).
        packet.getModifier().write(1, data);

        return packet;
    }

    /**
     * Creates a multi-block change packet for a group of blocks.
     * This method is highly version-specific and may require adjustments for different Minecraft versions.
     *
     * @param blocks A list of locations and block data representing the fake blocks.
     * @return The multi-block change packet.
     */
    private PacketContainer createMultiBlockChangePacket(
            List<Map.Entry<Location, BlockData>> blocks) {
        if (blocks.isEmpty()) return null;

        try {
            PacketContainer packet = PacketContainer.createPacket(
                    PacketType.PLAY_SERVER_MULTI_BLOCK_CHANGE
            );

            // Get chunk coordinates from the first block.
            Location first = blocks.get(0).getKey();
            int chunkX = first.getBlockX() >> 4;
            int chunkZ = first.getBlockZ() >> 4;

            // Create section position using a packed long format.
            long sectionPos = ((long) (chunkX & 0x3FFFFF) << 42) |
                    ((long) (first.getBlockY() >> 4 & 0xFFFFF) << 20) |
                    ((long) (chunkZ & 0x3FFFFF));

            packet.getLongs().write(0, sectionPos);

            // Create arrays for block positions and states.
            short[] positions = new short[blocks.size()];
            Object[] states = new Object[blocks.size()];

            for (int i = 0; i < blocks.size(); i++) {
                Location loc = blocks.get(i).getKey();
                BlockData data = blocks.get(i).getValue();

                // Calculate relative position within the chunk section.
                int relX = loc.getBlockX() & 15;
                int relY = loc.getBlockY() & 15;
                int relZ = loc.getBlockZ() & 15;

                positions[i] = (short) ((relX << 8) | (relZ << 4) | relY);
                states[i] = data; // Let NMS handle conversion.
            }

            // Set the arrays in the packet.
            packet.getModifier().write(1, positions);
            packet.getModifier().write(2, states);

            return packet;

        } catch (Exception e) {
            // Fallback to individual block changes if multi-block change fails.
            return null;
        }
    }

    /**
     * Generates a chunk key for grouping blocks by chunk.
     *
     * @param x The chunk X coordinate.
     * @param z The chunk Z coordinate.
     * @return The chunk key as a long value.
     */
    private long getChunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Clears all fake blocks for all players.
     * Typically used during server shutdown.
     */
    public void clearAll() {
        playerBlocks.clear();
    }
}