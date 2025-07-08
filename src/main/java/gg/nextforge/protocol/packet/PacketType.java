package gg.nextforge.protocol.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enum representing different types of Minecraft packets.
 * Provides mappings between packet classes and their types, protocols, and directions.
 * Includes caching mechanisms for efficient lookup and handling of unknown packet types.
 */
public enum PacketType {

    // Handshake packets
    HANDSHAKE_CLIENT_SET_PROTOCOL("PacketHandshakingInSetProtocol", Protocol.HANDSHAKE, Direction.SERVERBOUND),

    // Status packets
    STATUS_CLIENT_START("PacketStatusInStart", Protocol.STATUS, Direction.SERVERBOUND),
    STATUS_CLIENT_PING("PacketStatusInPing", Protocol.STATUS, Direction.SERVERBOUND),
    STATUS_SERVER_INFO("PacketStatusOutServerInfo", Protocol.STATUS, Direction.CLIENTBOUND),
    STATUS_SERVER_PONG("PacketStatusOutPong", Protocol.STATUS, Direction.CLIENTBOUND),

    // Login packets
    LOGIN_CLIENT_START("PacketLoginInStart", Protocol.LOGIN, Direction.SERVERBOUND),
    LOGIN_CLIENT_ENCRYPTION_BEGIN("PacketLoginInEncryptionBegin", Protocol.LOGIN, Direction.SERVERBOUND),
    LOGIN_CLIENT_CUSTOM_PAYLOAD("PacketLoginInCustomPayload", Protocol.LOGIN, Direction.SERVERBOUND),
    LOGIN_SERVER_DISCONNECT("PacketLoginOutDisconnect", Protocol.LOGIN, Direction.CLIENTBOUND),
    LOGIN_SERVER_ENCRYPTION_BEGIN("PacketLoginOutEncryptionBegin", Protocol.LOGIN, Direction.CLIENTBOUND),
    LOGIN_SERVER_SUCCESS("PacketLoginOutSuccess", Protocol.LOGIN, Direction.CLIENTBOUND),
    LOGIN_SERVER_SET_COMPRESSION("PacketLoginOutSetCompression", Protocol.LOGIN, Direction.CLIENTBOUND),
    LOGIN_SERVER_CUSTOM_PAYLOAD("PacketLoginOutCustomPayload", Protocol.LOGIN, Direction.CLIENTBOUND),

    // Play packets - Client -> Server
    PLAY_CLIENT_KEEP_ALIVE("ServerboundKeepAlivePacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_CHAT("ServerboundChatPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_CHAT_COMMAND("ServerboundChatCommandPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_USE_ENTITY("ServerboundInteractPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_POSITION("ServerboundMovePlayerPosPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_POSITION_LOOK("ServerboundMovePlayerPosRotPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_LOOK("ServerboundMovePlayerRotPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_FLYING("ServerboundMovePlayerStatusOnlyPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_BLOCK_DIG("ServerboundPlayerActionPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_BLOCK_PLACE("ServerboundUseItemOnPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_HELD_ITEM_SLOT("ServerboundSetCarriedItemPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_ARM_ANIMATION("ServerboundSwingPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_ENTITY_ACTION("ServerboundPlayerCommandPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_CLOSE_WINDOW("ServerboundContainerClosePacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_WINDOW_CLICK("ServerboundContainerClickPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_CUSTOM_PAYLOAD("ServerboundCustomPayloadPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_USE_ITEM("ServerboundUseItemPacket", Protocol.PLAY, Direction.SERVERBOUND),

    // Play packets - Server -> Client
    PLAY_SERVER_KEEP_ALIVE("ClientboundKeepAlivePacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_CHAT("ClientboundSystemChatPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_PLAYER_CHAT("ClientboundPlayerChatPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_EQUIPMENT("ClientboundSetEquipmentPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_SPAWN_POSITION("ClientboundSetDefaultSpawnPositionPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_UPDATE_HEALTH("ClientboundSetHealthPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_RESPAWN("ClientboundRespawnPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_POSITION("ClientboundPlayerPositionPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_HELD_ITEM_SLOT("ClientboundSetCarriedItemPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ANIMATION("ClientboundAnimatePacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_SPAWN_ENTITY("ClientboundAddEntityPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_SPAWN_ENTITY_LIVING("ClientboundAddMobPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_SPAWN_ENTITY_EXPERIENCE_ORB("ClientboundAddExperienceOrbPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_VELOCITY("ClientboundSetEntityMotionPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_DESTROY("ClientboundRemoveEntitiesPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY("ClientboundMoveEntityPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_LOOK("ClientboundMoveEntityPacket$Rot", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_MOVE_LOOK("ClientboundMoveEntityPacket$PosRot", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_TELEPORT("ClientboundTeleportEntityPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_HEAD_ROTATION("ClientboundRotateHeadPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_STATUS("ClientboundEntityEventPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_METADATA("ClientboundSetEntityDataPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_EFFECT("ClientboundUpdateMobEffectPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_REMOVE_ENTITY_EFFECT("ClientboundRemoveMobEffectPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_BLOCK_CHANGE("ClientboundBlockUpdatePacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_MULTI_BLOCK_CHANGE("ClientboundSectionBlocksUpdatePacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_EXPLOSION("ClientboundExplodePacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_WORLD_PARTICLES("ClientboundLevelParticlesPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_NAMED_SOUND_EFFECT("ClientboundSoundPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_WORLD_EVENT("ClientboundLevelEventPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_CUSTOM_PAYLOAD("ClientboundCustomPayloadPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_KICK_DISCONNECT("ClientboundDisconnectPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_TITLE("ClientboundSetTitleTextPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_TAB_COMPLETE("ClientboundTabListPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_BUNDLE("ClientboundBundlePacket", Protocol.PLAY, Direction.CLIENTBOUND),

    // Special unknown type for packets we don't recognize
    UNKNOWN("Unknown", Protocol.UNKNOWN, Direction.UNKNOWN);

    // Caches for fast lookup
    private static final Map<Class<?>, PacketType> CLASS_TO_TYPE = new ConcurrentHashMap<>();
    private static final Map<String, PacketType> NAME_TO_TYPE = new HashMap<>();

    static {
        // Build lookup tables for packet types.
        for (PacketType type : values()) {
            NAME_TO_TYPE.put(type.className, type);
        }
    }

    private final String className; // The class name of the packet.
    private final Protocol protocol; // The protocol stage of the packet.
    private final Direction direction; // The direction of the packet (serverbound or clientbound).

    /**
     * Constructs a PacketType enum value.
     *
     * @param className The class name of the packet.
     * @param protocol  The protocol stage of the packet.
     * @param direction The direction of the packet.
     */
    PacketType(String className, Protocol protocol, Direction direction) {
        this.className = className;
        this.protocol = protocol;
        this.direction = direction;
    }

    /**
     * Retrieves the PacketType from a packet class.
     * Uses caching for performance optimization.
     *
     * @param clazz The class of the packet.
     * @return The corresponding PacketType, or UNKNOWN if not found.
     */
    public static PacketType fromClass(Class<?> clazz) {
        return CLASS_TO_TYPE.computeIfAbsent(clazz, c -> {
            // Try exact class name match
            String simpleName = c.getSimpleName();
            PacketType type = NAME_TO_TYPE.get(simpleName);
            if (type != null) return type;

            // Try to find by checking superclasses
            Class<?> current = c;
            while (current != null && current != Object.class) {
                type = NAME_TO_TYPE.get(current.getSimpleName());
                if (type != null) return type;
                current = current.getSuperclass();
            }

            // Couldn't find it
            return UNKNOWN;
        });
    }

    /**
     * Retrieves the PacketType from a packet object.
     *
     * @param packet The packet object.
     * @return The corresponding PacketType, or UNKNOWN if not found.
     */
    public static PacketType fromPacket(Object packet) {
        return fromClass(packet.getClass());
    }

    /**
     * Retrieves the class name of the packet.
     *
     * @return The class name of the packet.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Retrieves the protocol stage of the packet.
     *
     * @return The protocol stage of the packet.
     */
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * Retrieves the direction of the packet.
     *
     * @return The direction of the packet.
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Checks if the packet is serverbound.
     *
     * @return True if the packet is serverbound, false otherwise.
     */
    public boolean isServerbound() {
        return direction == Direction.SERVERBOUND;
    }

    /**
     * Checks if the packet is clientbound.
     *
     * @return True if the packet is clientbound, false otherwise.
     */
    public boolean isClientbound() {
        return direction == Direction.CLIENTBOUND;
    }

    /**
     * Enum representing the protocol stage of a packet.
     */
    public enum Protocol {
        HANDSHAKE,
        STATUS,
        LOGIN,
        PLAY,
        UNKNOWN
    }

    /**
     * Enum representing the direction of a packet.
     */
    public enum Direction {
        SERVERBOUND,
        CLIENTBOUND,
        UNKNOWN
    }
}