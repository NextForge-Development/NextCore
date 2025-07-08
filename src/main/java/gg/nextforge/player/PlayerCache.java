package gg.nextforge.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gg.nextforge.scheduler.CoreScheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * PlayerCache provides a fast and efficient way to cache player data.
 * <p>
 * This class avoids the performance issues of Bukkit's OfflinePlayer by caching
 * player data in memory and persisting it to disk in JSON format. It supports
 * fast lookups for UUID-to-name mapping, custom player data storage, and is
 * persistent across server restarts.
 */
public class PlayerCache implements Listener {

    // Auto-save interval in ticks (5 minutes)
    private static final long SAVE_INTERVAL = 20L * 60 * 5;
    private final Plugin plugin; // The plugin instance associated with this cache
    private final File dataFolder; // Directory for storing player data files
    private final Map<UUID, CachedPlayer> cache = new ConcurrentHashMap<>(); // Cache for player data
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>(); // Mapping of player names to UUIDs
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Gson instance for JSON serialization
    // Statistics for monitoring cache performance
    private long cacheHits = 0; // Number of cache hits
    private long cacheMisses = 0; // Number of cache misses

    /**
     * Constructs a PlayerCache instance and initializes the cache.
     *
     * @param plugin The plugin instance associated with this cache.
     */
    public PlayerCache(Plugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");

        // Create the data folder if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Load existing player data from disk
        loadAllPlayers();

        // Schedule periodic auto-save
        CoreScheduler.runTimer(this::save, SAVE_INTERVAL, SAVE_INTERVAL);
    }

    /**
     * Retrieves cached player data by UUID.
     * Creates a new entry if the player is not already cached.
     *
     * @param uuid The UUID of the player.
     * @return The CachedPlayer instance for the specified UUID.
     */
    public CachedPlayer getPlayer(UUID uuid) {
        CachedPlayer player = cache.get(uuid);
        if (player != null) {
            cacheHits++;
            return player;
        }

        cacheMisses++;
        return cache.computeIfAbsent(uuid, this::loadOrCreatePlayer);
    }

    /**
     * Retrieves cached player data by name.
     * Case-insensitive lookup; returns null if the player is not cached.
     *
     * @param name The name of the player.
     * @return The CachedPlayer instance for the specified name, or null if not cached.
     */
    public CachedPlayer getPlayer(String name) {
        UUID uuid = nameToUuid.get(name.toLowerCase());
        return uuid != null ? getPlayer(uuid) : null;
    }

    /**
     * Retrieves the UUID of a player by name.
     * Faster than Bukkit's OfflinePlayer lookup.
     *
     * @param name The name of the player.
     * @return The UUID of the player, or null if not cached.
     */
    public UUID getUUID(String name) {
        return nameToUuid.get(name.toLowerCase());
    }

    /**
     * Checks if a player has joined the server before.
     * Faster than OfflinePlayer.hasPlayedBefore().
     *
     * @param uuid The UUID of the player.
     * @return true if the player has joined before, false otherwise.
     */
    public boolean hasPlayedBefore(UUID uuid) {
        return cache.containsKey(uuid) || new File(dataFolder, uuid + ".json").exists();
    }

    /**
     * Saves all cached player data to disk.
     * Called periodically and on server shutdown.
     */
    public void save() {
        int saved = 0;
        for (CachedPlayer player : cache.values()) {
            if (player.isDirty()) {
                savePlayer(player);
                saved++;
            }
        }

        if (saved > 0) {
            plugin.getLogger().info("Saved " + saved + " player data files.");
        }
    }

    /**
     * Saves a specific player's data to disk.
     *
     * @param player The CachedPlayer instance to save.
     */
    public void savePlayer(CachedPlayer player) {
        File file = new File(dataFolder, player.getUuid() + ".json");

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(player, writer);
            player.setDirty(false);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to save player data: " + player.getUuid(), e);
        }
    }

    /**
     * Loads all player data files from disk.
     * Called on server startup.
     */
    private void loadAllPlayers() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        int loaded = 0;
        for (File file : files) {
            try {
                String uuidStr = file.getName().replace(".json", "");
                UUID uuid = UUID.fromString(uuidStr);

                CachedPlayer player = loadPlayer(uuid);
                if (player != null) {
                    cache.put(uuid, player);
                    nameToUuid.put(player.getLastName().toLowerCase(), uuid);
                    loaded++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player data: " + file.getName());
            }
        }

        plugin.getLogger().info("Loaded " + loaded + " player data files.");
    }

    /**
     * Loads a player's data from disk.
     *
     * @param uuid The UUID of the player.
     * @return The CachedPlayer instance, or null if the file does not exist.
     */
    private CachedPlayer loadPlayer(UUID uuid) {
        File file = new File(dataFolder, uuid + ".json");
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, CachedPlayer.class);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to load player data: " + uuid, e);
            return null;
        }
    }

    /**
     * Loads or creates a player's data.
     * If the data does not exist on disk, a new entry is created.
     *
     * @param uuid The UUID of the player.
     * @return The CachedPlayer instance.
     */
    private CachedPlayer loadOrCreatePlayer(UUID uuid) {
        CachedPlayer player = loadPlayer(uuid);
        if (player != null) return player;

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        player = new CachedPlayer(uuid);
        player.setLastName(offlinePlayer.getName() != null ?
                offlinePlayer.getName() : uuid.toString());

        return player;
    }

    // Event handlers

    /**
     * Handles the PlayerJoinEvent.
     * Updates cached data and name-to-UUID mapping.
     *
     * @param event The PlayerJoinEvent instance.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        CachedPlayer cached = getPlayer(player.getUniqueId());

        cached.setLastName(player.getName());
        cached.setLastLogin(System.currentTimeMillis());
        cached.setOnline(true);
        cached.setDirty(true);

        nameToUuid.put(player.getName().toLowerCase(), player.getUniqueId());
    }

    /**
     * Handles the PlayerQuitEvent.
     * Updates cached data and saves the player's data to disk.
     *
     * @param event The PlayerQuitEvent instance.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CachedPlayer cached = getPlayer(player.getUniqueId());

        cached.setLastSeen(System.currentTimeMillis());
        cached.setOnline(false);
        cached.setDirty(true);

        savePlayer(cached);
    }

    // Statistics and management methods

    /**
     * Retrieves the number of cached players.
     *
     * @return The total number of cached players.
     */
    public int getCachedPlayerCount() {
        return cache.size();
    }

    /**
     * Retrieves the number of cache hits.
     *
     * @return The total number of cache hits.
     */
    public long getCacheHits() {
        return cacheHits;
    }

    /**
     * Retrieves the number of cache misses.
     *
     * @return The total number of cache misses.
     */
    public long getCacheMisses() {
        return cacheMisses;
    }

    /**
     * Clears all cached data.
     * Use with caution as this will remove all cached player data.
     */
    public void clearCache() {
        cache.clear();
        nameToUuid.clear();
        cacheHits = 0;
        cacheMisses = 0;
    }
}