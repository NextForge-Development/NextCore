package gg.nextforge.player;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents cached player data for storing commonly accessed information.
 * <p>
 * This class avoids expensive OfflinePlayer lookups by caching player data.
 * It supports custom metadata storage, which is serialized to JSON.
 * Metadata should be simple types like strings, numbers, or booleans.
 */
@Data
@Setter(AccessLevel.PACKAGE)
public class CachedPlayer {

    private final UUID uuid; // The unique identifier of the player
    private final Map<String, Object> metadata = new HashMap<>(); // Custom metadata storage
    private String lastName; // The last known name of the player
    private final long firstLogin; // Timestamp of the player's first login
    private long lastLogin; // Timestamp of the player's last login
    private long lastSeen; // Timestamp of the player's last seen activity
    private boolean online; // Whether the player is currently online
    // Transient fields (not serialized)
    private transient boolean dirty = false; // Flag indicating if the data has been modified

    /**
     * Constructs a CachedPlayer instance with the given UUID.
     * Initializes timestamps to the current time.
     *
     * @param uuid The unique identifier of the player.
     */
    public CachedPlayer(UUID uuid) {
        this.uuid = uuid;
        this.firstLogin = System.currentTimeMillis();
        this.lastLogin = firstLogin;
        this.lastSeen = firstLogin;
    }

    /**
     * Sets the player's last known name.
     *
     * @param lastName The last name of the player.
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.dirty = true;
    }

    /**
     * Sets the timestamp of the player's last login.
     *
     * @param lastLogin The last login timestamp.
     */
    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
        this.dirty = true;
    }

    /**
     * Sets the timestamp of the player's last seen activity.
     *
     * @param lastSeen The last seen timestamp.
     */
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
        this.dirty = true;
    }

    /**
     * Sets the player's online status.
     *
     * @param online true if the player is online, false otherwise.
     */
    public void setOnline(boolean online) {
        this.online = online;
        this.dirty = true;
    }

    /**
     * Retrieves a custom metadata value.
     *
     * @param key The key of the metadata.
     * @param <T> The type of the metadata value.
     * @return The metadata value, or null if not set.
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        return (T) metadata.get(key);
    }

    /**
     * Sets a custom metadata value.
     * The value must be JSON-serializable.
     *
     * @param key   The key of the metadata.
     * @param value The value to set.
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
        this.dirty = true;
    }

    /**
     * Removes a custom metadata value.
     *
     * @param key The key of the metadata to remove.
     */
    public void removeMetadata(String key) {
        if (metadata.remove(key) != null) {
            this.dirty = true;
        }
    }

    /**
     * Checks if a metadata key exists.
     *
     * @param key The key to check.
     * @return true if the key exists, false otherwise.
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    /**
     * Retrieves all metadata as a new map.
     * Modifications to the returned map do not affect the original metadata.
     *
     * @return A copy of the metadata map.
     */
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }

}