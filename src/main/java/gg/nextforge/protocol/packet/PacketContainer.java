package gg.nextforge.protocol.packet;

import gg.nextforge.protocol.reflect.StructureModifier;
import gg.nextforge.version.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Represents a container for a Minecraft packet, providing convenient methods
 * for accessing and modifying packet fields using reflection.
 * <p>
 * This class simplifies field access and manipulation by utilizing the
 * `StructureModifier` system, inspired by ProtocolLib but implemented from scratch.
 * It is slower than direct field access but offers greater convenience and
 * version independence.
 */
public class PacketContainer {

    private final Object handle; // The underlying NMS packet object.
    private final PacketType type; // The type of the packet.
    private final boolean cancelled; // Indicates whether the packet is cancelled.

    // Structure modifiers for accessing fields of various types.
    private StructureModifier<Byte> bytes;
    private StructureModifier<Short> shorts;
    private StructureModifier<Integer> integers;
    private StructureModifier<Long> longs;
    private StructureModifier<Float> floats;
    private StructureModifier<Double> doubles;
    private StructureModifier<String> strings;
    private StructureModifier<Boolean> booleans;
    private StructureModifier<UUID> uuids;
    private StructureModifier<byte[]> byteArrays;
    private StructureModifier<int[]> intArrays;
    private final StructureModifier<Object> modifier;

    /**
     * Constructs a `PacketContainer` for the given NMS packet object.
     *
     * @param handle The NMS packet object.
     */
    public PacketContainer(Object handle) {
        this.handle = handle;
        this.type = PacketType.fromPacket(handle);
        this.cancelled = false;

        // Initialize the base modifier for accessing fields.
        this.modifier = new StructureModifier<>(handle.getClass(), Object.class);
    }

    /**
     * Constructs a `PacketContainer` for the given packet type and NMS packet object.
     *
     * @param type   The type of the packet.
     * @param handle The NMS packet object.
     */
    public PacketContainer(PacketType type, Object handle) {
        this.handle = handle;
        this.type = type;
        this.cancelled = false;

        // Initialize the base modifier for accessing fields.
        this.modifier = new StructureModifier<>(handle.getClass(), Object.class);
    }

    /**
     * Creates a new packet of the specified type using reflection.
     *
     * @param type The type of the packet to create.
     * @return A new `PacketContainer` instance for the created packet.
     */
    public static PacketContainer createPacket(PacketType type) {
        try {
            // Get the class of the packet based on its type.
            Class<?> packetClass = ReflectionUtil.getNMSClass(getPacketPath(type));

            // Create a new instance of the packet.
            Object packet = ReflectionUtil.getConstructor(packetClass).newInstance();

            return new PacketContainer(type, packet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create packet of type: " + type, e);
        }
    }

    /**
     * Retrieves the class path for the specified packet type.
     *
     * @param type The type of the packet.
     * @return The class path for the packet.
     */
    private static String getPacketPath(PacketType type) {
        return switch (type.getProtocol()) {
            case HANDSHAKE -> "network.protocol.handshake." + type.getClassName();
            case STATUS -> "network.protocol.status." + type.getClassName();
            case LOGIN -> "network.protocol.login." + type.getClassName();
            case PLAY -> "network.protocol.game." + type.getClassName();
            default -> throw new IllegalArgumentException("Unknown protocol: " + type.getProtocol());
        };
    }

    /**
     * Retrieves the underlying NMS packet object.
     *
     * @return The NMS packet object.
     */
    public Object getHandle() {
        return handle;
    }

    /**
     * Retrieves the type of the packet.
     *
     * @return The type of the packet.
     */
    public PacketType getType() {
        return type;
    }

    /**
     * Creates a deep clone of this packet using serialization.
     *
     * @return A new `PacketContainer` instance representing the cloned packet.
     */
    public PacketContainer deepClone() {
        try {
            // Create a new instance of the packet.
            Object cloned = handle.getClass().getConstructor().newInstance();

            // Copy all fields from the original packet to the cloned packet.
            StructureModifier<Object> source = getModifier();
            StructureModifier<Object> target = new StructureModifier<>(cloned.getClass(), Object.class);

            for (int i = 0; i < source.size(); i++) {
                target.write(i, source.read(i));
            }

            return new PacketContainer(type, cloned);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone packet", e);
        }
    }

    // Lazy initialization for field accessors.

    /**
     * Retrieves the `StructureModifier` for byte fields.
     *
     * @return The `StructureModifier` for byte fields.
     */
    public StructureModifier<Byte> getBytes() {
        if (bytes == null) {
            bytes = modifier.withType(byte.class);
        }
        return bytes;
    }

    /**
     * Retrieves the `StructureModifier` for short fields.
     *
     * @return The `StructureModifier` for short fields.
     */
    public StructureModifier<Short> getShorts() {
        if (shorts == null) {
            shorts = modifier.withType(short.class);
        }
        return shorts;
    }

    /**
     * Retrieves the `StructureModifier` for integer fields.
     *
     * @return The `StructureModifier` for integer fields.
     */
    public StructureModifier<Integer> getIntegers() {
        if (integers == null) {
            integers = modifier.withType(int.class);
        }
        return integers;
    }

    /**
     * Retrieves the `StructureModifier` for long fields.
     *
     * @return The `StructureModifier` for long fields.
     */
    public StructureModifier<Long> getLongs() {
        if (longs == null) {
            longs = modifier.withType(long.class);
        }
        return longs;
    }

    /**
     * Retrieves the `StructureModifier` for float fields.
     *
     * @return The `StructureModifier` for float fields.
     */
    public StructureModifier<Float> getFloats() {
        if (floats == null) {
            floats = modifier.withType(float.class);
        }
        return floats;
    }

    /**
     * Retrieves the `StructureModifier` for double fields.
     *
     * @return The `StructureModifier` for double fields.
     */
    public StructureModifier<Double> getDoubles() {
        if (doubles == null) {
            doubles = modifier.withType(double.class);
        }
        return doubles;
    }

    /**
     * Retrieves the `StructureModifier` for string fields.
     *
     * @return The `StructureModifier` for string fields.
     */
    public StructureModifier<String> getStrings() {
        if (strings == null) {
            strings = modifier.withType(String.class);
        }
        return strings;
    }

    /**
     * Retrieves the `StructureModifier` for boolean fields.
     *
     * @return The `StructureModifier` for boolean fields.
     */
    public StructureModifier<Boolean> getBooleans() {
        if (booleans == null) {
            booleans = modifier.withType(boolean.class);
        }
        return booleans;
    }

    /**
     * Retrieves the `StructureModifier` for UUID fields.
     *
     * @return The `StructureModifier` for UUID fields.
     */
    public StructureModifier<UUID> getUUIDs() {
        if (uuids == null) {
            uuids = modifier.withType(UUID.class);
        }
        return uuids;
    }

    /**
     * Retrieves the `StructureModifier` for byte array fields.
     *
     * @return The `StructureModifier` for byte array fields.
     */
    public StructureModifier<byte[]> getByteArrays() {
        if (byteArrays == null) {
            byteArrays = modifier.withType(byte[].class);
        }
        return byteArrays;
    }

    /**
     * Retrieves the `StructureModifier` for integer array fields.
     *
     * @return The `StructureModifier` for integer array fields.
     */
    public StructureModifier<int[]> getIntArrays() {
        if (intArrays == null) {
            intArrays = modifier.withType(int[].class);
        }
        return intArrays;
    }

    /**
     * Retrieves the base `StructureModifier` for custom field types.
     *
     * @return The base `StructureModifier`.
     */
    public StructureModifier<Object> getModifier() {
        return modifier;
    }

    /**
     * Retrieves a `StructureModifier` for a specific field type.
     *
     * @param <T>  The type of the field.
     * @param type The class of the field type.
     * @return The `StructureModifier` for the specified field type.
     */
    public <T> StructureModifier<T> getSpecificModifier(Class<T> type) {
        return modifier.withType(type);
    }

    // Convenience methods for common operations.

    /**
     * Retrieves the entity ID from packets that contain one.
     *
     * @return The entity ID.
     */
    public int getEntityId() {
        return getIntegers().read(0);
    }

    /**
     * Sets the entity ID for packets that contain one.
     *
     * @param id The entity ID to set.
     */
    public void setEntityId(int id) {
        getIntegers().write(0, id);
    }

    /**
     * Retrieves the chat message from chat packets.
     *
     * @return The chat message.
     * @throws IllegalStateException If the packet is not a chat packet.
     */
    public String getMessage() {
        if (type == PacketType.PLAY_CLIENT_CHAT || type == PacketType.PLAY_SERVER_CHAT) {
            return getStrings().read(0);
        }
        throw new IllegalStateException("getMessage() called on non-chat packet: " + type);
    }

    /**
     * Sets the chat message for chat packets.
     *
     * @param message The chat message to set.
     * @throws IllegalStateException If the packet is not a chat packet.
     */
    public void setMessage(String message) {
        if (type == PacketType.PLAY_CLIENT_CHAT || type == PacketType.PLAY_SERVER_CHAT) {
            getStrings().write(0, message);
        } else {
            throw new IllegalStateException("setMessage() called on non-chat packet: " + type);
        }
    }

    /**
     * Retrieves the block position from block-related packets.
     *
     * @return The block position as a `Location` object with a null world.
     * @throws IllegalStateException If the packet is not block-related.
     */
    public org.bukkit.Location getBlockPosition() {
        if (type.name().contains("BLOCK")) {
            long packed = getLongs().read(0);
            int x = (int) (packed >> 38);
            int y = (int) (packed & 0xFFF);
            int z = (int) (packed << 26 >> 38);
            return new org.bukkit.Location(null, x, y, z);
        }
        throw new IllegalStateException("getBlockPosition() called on non-block packet: " + type);
    }

    /**
     * Retrieves the raw bytes of the packet for low-level manipulation.
     *
     * @return The raw bytes of the packet.
     * @throws RuntimeException If serialization fails.
     */
    public byte[] getRawBytes() {
        try {
            // Get the packet serializer class and buffer creation method.
            Class<?> packetDataSerializerClass = ReflectionUtil.getNMSClass("network.FriendlyByteBuf");
            Class<?> unpooledClass = ReflectionUtil.getClass("io.netty.buffer.Unpooled");
            Method buffer = ReflectionUtil.getMethod(unpooledClass, "buffer");

            // Create a new buffer.
            Object byteBuf = ReflectionUtil.invoke(buffer, null);

            // Create a packet data serializer.
            Constructor<?> serializerConstructor = ReflectionUtil.getConstructor(packetDataSerializerClass, byteBuf.getClass());
            Object serializer = ReflectionUtil.newInstance(serializerConstructor, byteBuf);

            // Write the packet to the buffer.
            Method writeMethod = ReflectionUtil.getMethod(handle.getClass(), "write", packetDataSerializerClass);
            ReflectionUtil.invoke(writeMethod, handle, serializer);

            // Read the bytes from the buffer.
            Method readableBytes = ReflectionUtil.getMethod(byteBuf.getClass(), "readableBytes");
            int length = ReflectionUtil.invoke(readableBytes, byteBuf);
            byte[] data = new byte[length];
            Method readBytes = ReflectionUtil.getMethod(byteBuf.getClass(), "readBytes", byte[].class);
            ReflectionUtil.invoke(readBytes, byteBuf, data);

            return data;
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize packet to bytes", e);
        }
    }
}