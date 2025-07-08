package gg.nextforge.protocol.util;

import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;
import gg.nextforge.version.ReflectionUtil;
import gg.nextforge.protocol.ProtocolManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating and sending packet-based effects.
 * <p>
 * This class provides methods to send sounds, particles, and other visual effects
 * directly via packets, bypassing Bukkit API limitations. It allows for custom
 * effects such as playing sounds at high volumes, spawning large numbers of particles,
 * and making entities glow for specific players.
 */
public class PacketEffects {

    private final ProtocolManager protocol;

    /**
     * Constructs a PacketEffects instance.
     *
     * @param protocol The ProtocolManager used to send packets.
     */
    public PacketEffects(ProtocolManager protocol) {
        this.protocol = protocol;
    }

    /**
     * Plays a sound via packets.
     *
     * @param sound    The sound to play.
     * @param location The location where the sound will be played.
     * @param volume   The volume of the sound (1.0 = normal, can go higher).
     * @param pitch    The pitch of the sound (1.0 = normal, 0.5 = deep, 2.0 = chipmunk).
     * @param players  The players who will hear the sound.
     */
    public void playSound(Sound sound, Location location, float volume, float pitch,
                          Player... players) {
        PacketContainer packet = PacketContainer.createPacket(
                PacketType.PLAY_SERVER_NAMED_SOUND_EFFECT
        );

        // Sound ID - version-specific handling
        packet.getModifier().write(0, sound);

        // Sound category (e.g., master, music, hostile)
        packet.getModifier().write(1, SoundCategory.MASTER);

        // Position (multiplied by 8 due to protocol requirements)
        packet.getIntegers().write(0, (int) (location.getX() * 8));
        packet.getIntegers().write(1, (int) (location.getY() * 8));
        packet.getIntegers().write(2, (int) (location.getZ() * 8));

        // Volume and pitch
        packet.getFloats().write(0, volume);
        packet.getFloats().write(1, pitch);

        for (Player player : players) {
            protocol.sendPacket(player, packet);
        }
    }

    /**
     * Plays a custom sound via packets.
     *
     * @param soundName The name of the custom sound file.
     * @param location  The location where the sound will be played.
     * @param volume    The volume of the sound.
     * @param pitch     The pitch of the sound.
     * @param players   The players who will hear the sound.
     */
    public void playCustomSound(String soundName, Location location,
                                float volume, float pitch, Player... players) {
        PacketContainer packet = PacketContainer.createPacket(
                PacketType.PLAY_SERVER_NAMED_SOUND_EFFECT
        );

        // Custom sound name
        packet.getStrings().write(0, soundName);

        // Sound category
        packet.getModifier().write(1, SoundCategory.MASTER);

        // Position (multiplied by 8)
        packet.getIntegers().write(0, (int) (location.getX() * 8));
        packet.getIntegers().write(1, (int) (location.getY() * 8));
        packet.getIntegers().write(2, (int) (location.getZ() * 8));

        // Volume and pitch
        packet.getFloats().write(0, volume);
        packet.getFloats().write(1, pitch);

        for (Player player : players) {
            protocol.sendPacket(player, packet);
        }
    }

    /**
     * Spawns particles via packets.
     *
     * @param particle The type of particle to spawn.
     * @param location The location where the particles will be spawned.
     * @param count    The number of particles to spawn.
     * @param offsetX  The random offset range for the X-axis.
     * @param offsetY  The random offset range for the Y-axis.
     * @param offsetZ  The random offset range for the Z-axis.
     * @param speed    The speed or spread of the particles.
     * @param data     Extra data for the particles (e.g., color or block type).
     * @param players  The players who will see the particles.
     */
    public void spawnParticle(Particle particle, Location location, int count,
                              double offsetX, double offsetY, double offsetZ,
                              double speed, Object data, Player... players) {
        PacketContainer packet = PacketContainer.createPacket(
                PacketType.PLAY_SERVER_WORLD_PARTICLES
        );

        // Particle type
        packet.getModifier().write(0, particle);

        // Long distance flag (render from far away)
        packet.getBooleans().write(0, true);

        // Position
        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, location.getY());
        packet.getDoubles().write(2, location.getZ());

        // Offsets
        packet.getFloats().write(0, (float) offsetX);
        packet.getFloats().write(1, (float) offsetY);
        packet.getFloats().write(2, (float) offsetZ);

        // Speed
        packet.getFloats().write(3, (float) speed);

        // Count
        packet.getIntegers().write(0, count);

        // Particle data (e.g., color or block type)
        if (data != null) {
            packet.getModifier().write(10, data);
        }

        for (Player player : players) {
            protocol.sendPacket(player, packet);
        }
    }

    /**
     * Creates a particle explosion effect.
     *
     * @param center        The center location of the explosion.
     * @param radius        The radius of the explosion.
     * @param particleCount The number of particles in the explosion.
     * @param players       The players who will see the explosion.
     */
    public void createExplosion(Location center, float radius, int particleCount,
                                Player... players) {
        // Explosion particles in a sphere
        for (int i = 0; i < particleCount; i++) {
            double angle1 = Math.random() * Math.PI * 2;
            double angle2 = Math.random() * Math.PI;

            double x = center.getX() + radius * Math.sin(angle2) * Math.cos(angle1);
            double y = center.getY() + radius * Math.sin(angle2) * Math.sin(angle1);
            double z = center.getZ() + radius * Math.cos(angle2);

            Location particleLoc = new Location(center.getWorld(), x, y, z);

            spawnParticle(Particle.EXPLOSION_NORMAL, particleLoc, 1,
                    0, 0, 0, 0.1, null, players);
        }

        // Explosion sound
        playSound(Sound.ENTITY_GENERIC_EXPLODE, center, 2.0f, 1.0f, players);
    }

    /**
     * Makes an entity glow for specific players.
     *
     * @param entityId The ID of the entity to make glow.
     * @param glowing  Whether the entity should glow.
     * @param players  The players who will see the glowing effect.
     */
    public void setGlowing(int entityId, boolean glowing, Player... players) {
        PacketContainer packet = PacketContainer.createPacket(
                PacketType.PLAY_SERVER_ENTITY_METADATA
        );

        packet.getIntegers().write(0, entityId);

        // The glowing flag is in byte 0, bit 6
        byte flags = glowing ? (byte) 0x40 : (byte) 0x00;

        // Create metadata list
        List<Object> metadataList = new ArrayList<>();

        try {
            // Get metadata classes
            Class<?> dataWatcherClass = ReflectionUtil.getNMSClass("network.syncher.SynchedEntityData");
            Class<?> dataWatcherItemClass = ReflectionUtil.getNMSClass("network.syncher.SynchedEntityData$DataValue");
            Class<?> dataWatcherObjectClass = ReflectionUtil.getNMSClass("network.syncher.EntityDataAccessor");
            Class<?> dataWatcherSerializerClass = ReflectionUtil.getNMSClass("network.syncher.EntityDataSerializer");
            Class<?> dataWatcherRegistryClass = ReflectionUtil.getNMSClass("network.syncher.EntityDataSerializers");

            // Get the BYTE serializer
            Field byteSerializerField = ReflectionUtil.getField(dataWatcherRegistryClass, "BYTE");
            Object byteSerializer = byteSerializerField.get(null);

            // Create EntityDataAccessor for index 0 (flags)
            Constructor<?> accessorConstructor = ReflectionUtil.getConstructor(
                    dataWatcherObjectClass, int.class, dataWatcherSerializerClass
            );
            Object accessor = ReflectionUtil.newInstance(accessorConstructor, 0, byteSerializer);

            // Create DataValue
            Constructor<?> itemConstructor = ReflectionUtil.getConstructor(
                    dataWatcherItemClass, dataWatcherObjectClass, Object.class
            );
            Object dataValue = ReflectionUtil.newInstance(itemConstructor, accessor, flags);

            metadataList.add(dataValue);

            // Set the metadata list in the packet
            packet.getModifier().write(1, metadataList);
        } catch (Exception e) {
            // Log version-specific issues
            protocol.getPlugin().getLogger().warning(
                    "Failed to set glowing effect - version incompatibility: " + e.getMessage()
            );
        }

        for (Player player : players) {
            protocol.sendPacket(player, packet);
        }
    }
}