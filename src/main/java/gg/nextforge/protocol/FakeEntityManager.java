package gg.nextforge.protocol;

import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;
import gg.nextforge.version.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages fake entities that only exist in packets.
 * This class allows the creation, manipulation, and removal of fake entities
 * that exist only on the client side, providing efficient ways to simulate entities
 * without server-side overhead.
 */
public class FakeEntityManager {

    private final ProtocolManager protocol; // Protocol manager for sending packets.

    // Map tracking fake entities per player.
    private final Map<UUID, Set<Integer>> playerEntities = new ConcurrentHashMap<>();

    // Counter for generating unique entity IDs.
    private int nextEntityId = 100000;

    // Map storing entity data for respawning and manipulation.
    private final Map<Integer, FakeEntity> entities = new ConcurrentHashMap<>();

    /**
     * Constructs a FakeEntityManager instance.
     *
     * @param protocol The protocol manager used for sending packets.
     */
    public FakeEntityManager(ProtocolManager protocol) {
        this.protocol = protocol;
    }

    /**
     * Spawns a fake entity for specific players.
     *
     * @param location The location where the entity will be spawned.
     * @param type     The type of the entity to spawn.
     * @param viewers  The players who will see the entity.
     * @return The ID of the spawned fake entity.
     */
    public int spawnEntity(Location location, EntityType type, List<Player> viewers) {
        int entityId = nextEntityId++;

        // Create spawn packet based on entity type.
        PacketContainer packet = createSpawnPacket(entityId, location, type);

        // Send the packet to viewers and track the entity.
        for (Player viewer : viewers) {
            protocol.sendPacket(viewer, packet);
            playerEntities.computeIfAbsent(viewer.getUniqueId(), k -> new HashSet<>())
                    .add(entityId);
        }

        // Store entity data for future manipulation.
        entities.put(entityId, new FakeEntity(entityId, location, type, viewers));

        return entityId;
    }

    /**
     * Creates the appropriate spawn packet for an entity type.
     *
     * @param entityId The ID of the entity.
     * @param loc      The location of the entity.
     * @param type     The type of the entity.
     * @return The spawn packet for the entity.
     */
    private PacketContainer createSpawnPacket(int entityId, Location loc, EntityType type) {
        PacketContainer packet;

        if (type.isAlive()) {
            // Create spawn packet for living entities.
            packet = PacketContainer.createPacket(PacketType.PLAY_SERVER_SPAWN_ENTITY_LIVING);
            packet.getIntegers().write(0, entityId); // Entity ID.
            packet.getUUIDs().write(0, UUID.randomUUID()); // Entity UUID.
            packet.getIntegers().write(1, getEntityTypeId(type)); // Entity type ID.
            packet.getDoubles().write(0, loc.getX());
            packet.getDoubles().write(1, loc.getY());
            packet.getDoubles().write(2, loc.getZ());
        } else {
            // Create spawn packet for non-living entities.
            packet = PacketContainer.createPacket(PacketType.PLAY_SERVER_SPAWN_ENTITY);
            packet.getIntegers().write(0, entityId);
            packet.getUUIDs().write(0, UUID.randomUUID());
            packet.getIntegers().write(1, getEntityTypeId(type));
            packet.getDoubles().write(0, loc.getX());
            packet.getDoubles().write(1, loc.getY());
            packet.getDoubles().write(2, loc.getZ());
        }

        return packet;
    }

    /**
     * Retrieves the network ID for an entity type using reflection.
     *
     * @param type The type of the entity.
     * @return The network ID for the entity type.
     */
    private int getEntityTypeId(EntityType type) {
        try {
            // Use reflection to get the entity type ID.
            Class<?> registryClass = ReflectionUtil.getNMSClass("core.IRegistry");
            Class<?> entityTypesClass = ReflectionUtil.getNMSClass("world.entity.EntityTypes");

            Field registryField = ReflectionUtil.getField(registryClass, "ENTITY_TYPE");
            Object registry = registryField.get(null);

            Field entityTypeField = ReflectionUtil.getField(entityTypesClass, type.name());
            Object nmsEntityType = entityTypeField.get(null);

            Method getId = ReflectionUtil.getMethod(registry.getClass(), "getId", Object.class);
            return ReflectionUtil.invoke(getId, registry, nmsEntityType);
        } catch (Exception e) {
            // Fallback to ordinal if reflection fails.
            protocol.getPlugin().getLogger().warning(
                    "Failed to get proper entity type ID for " + type + ", using ordinal"
            );
            return type.ordinal();
        }
    }

    /**
     * Moves a fake entity to a new location.
     *
     * @param entityId     The ID of the entity to move.
     * @param newLocation  The new location of the entity.
     */
    public void moveEntity(int entityId, Location newLocation) {
        FakeEntity entity = entities.get(entityId);
        if (entity == null) return;

        Location oldLoc = entity.location;
        double dx = newLocation.getX() - oldLoc.getX();
        double dy = newLocation.getY() - oldLoc.getY();
        double dz = newLocation.getZ() - oldLoc.getZ();

        // Use teleport packet for large movements.
        if (Math.abs(dx) > 8 || Math.abs(dy) > 8 || Math.abs(dz) > 8) {
            PacketContainer packet = PacketContainer.createPacket(
                    PacketType.PLAY_SERVER_ENTITY_TELEPORT
            );
            packet.getIntegers().write(0, entityId);
            packet.getDoubles().write(0, newLocation.getX());
            packet.getDoubles().write(1, newLocation.getY());
            packet.getDoubles().write(2, newLocation.getZ());

            sendToViewers(entity, packet);
        } else {
            // Use relative move packet for small movements.
            PacketContainer packet = PacketContainer.createPacket(
                    PacketType.PLAY_SERVER_ENTITY_MOVE_LOOK
            );
            packet.getIntegers().write(0, entityId);
            packet.getShorts().write(0, (short)(dx * 4096));
            packet.getShorts().write(1, (short)(dy * 4096));
            packet.getShorts().write(2, (short)(dz * 4096));

            sendToViewers(entity, packet);
        }

        entity.location = newLocation;
    }

    /**
     * Makes a fake entity glow by setting the glowing flag in metadata.
     *
     * @param entityId The ID of the entity.
     * @param glowing  Whether the entity should glow.
     */
    public void setGlowing(int entityId, boolean glowing) {
        FakeEntity entity = entities.get(entityId);
        if (entity == null) return;

        PacketContainer packet = PacketContainer.createPacket(
                PacketType.PLAY_SERVER_ENTITY_METADATA
        );
        packet.getIntegers().write(0, entityId);

        sendToViewers(entity, packet);
    }

    /**
     * Removes a fake entity.
     *
     * @param entityId The ID of the entity to remove.
     */
    public void removeEntity(int entityId) {
        FakeEntity entity = entities.remove(entityId);
        if (entity == null) return;

        PacketContainer packet = PacketContainer.createPacket(
                PacketType.PLAY_SERVER_ENTITY_DESTROY
        );
        packet.getIntArrays().write(0, new int[]{entityId});

        sendToViewers(entity, packet);

        for (Player viewer : entity.viewers) {
            Set<Integer> viewerEntities = playerEntities.get(viewer.getUniqueId());
            if (viewerEntities != null) {
                viewerEntities.remove(entityId);
            }
        }
    }

    /**
     * Removes all fake entities for a specific player.
     *
     * @param player The player whose fake entities should be removed.
     */
    public void clearPlayer(Player player) {
        Set<Integer> entities = playerEntities.remove(player.getUniqueId());
        if (entities == null) return;

        PacketContainer packet = PacketContainer.createPacket(
                PacketType.PLAY_SERVER_ENTITY_DESTROY
        );
        packet.getIntArrays().write(0,
                entities.stream().mapToInt(Integer::intValue).toArray()
        );

        protocol.sendPacket(player, packet);
    }

    /**
     * Sends a packet to all viewers of a fake entity.
     *
     * @param entity The fake entity.
     * @param packet The packet to send.
     */
    private void sendToViewers(FakeEntity entity, PacketContainer packet) {
        for (Player viewer : entity.viewers) {
            if (viewer.isOnline()) {
                protocol.sendPacket(viewer, packet);
            }
        }
    }

    /**
     * Clears all fake entities for all players.
     * Typically used during server shutdown.
     */
    public void clearAll() {
        for (UUID playerId : playerEntities.keySet()) {
            Player player = protocol.getPlugin().getServer().getPlayer(playerId);
            if (player != null) {
                clearPlayer(player);
            }
        }
        playerEntities.clear();
    }

    /**
     * Internal class representing a fake entity.
     */
    private static class FakeEntity {
        final int id; // The ID of the entity.
        Location location; // The current location of the entity.
        final EntityType type; // The type of the entity.
        final List<Player> viewers; // The players who can see the entity.

        /**
         * Constructs a FakeEntity instance.
         *
         * @param id       The ID of the entity.
         * @param location The location of the entity.
         * @param type     The type of the entity.
         * @param viewers  The players who can see the entity.
         */
        FakeEntity(int id, Location location, EntityType type, List<Player> viewers) {
            this.id = id;
            this.location = location.clone();
            this.type = type;
            this.viewers = new ArrayList<>(viewers);
        }
    }
}