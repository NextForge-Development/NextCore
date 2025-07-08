package gg.nextforge.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * Manages multiple configuration files in a thread-safe manner.
 *
 * The ConfigManager class provides utilities for loading, saving, and reloading
 * configuration files. It supports auto-reloading when files change and ensures
 * thread-safe access to configuration data.
 */
public class ConfigManager {

    private final Plugin plugin; // The plugin instance associated with this manager
    private final Map<String, ConfigFile> configs = new ConcurrentHashMap<>(); // Cache for loaded configuration files
    private final File configFolder; // The folder where configuration files are stored

    /**
     * Constructs a ConfigManager instance and initializes the default configuration.
     *
     * @param plugin The plugin instance associated with this manager.
     */
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFolder = plugin.getDataFolder();

        // Create the data folder if it doesn't exist
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }

        // Load the default config.yml file
        loadConfig("config.yml");
    }

    /**
     * Loads or reloads a configuration file.
     *
     * If the file does not exist, it is created with default values. If the file
     * is already loaded, it is reloaded from disk.
     *
     * @param fileName The name of the configuration file (e.g., "messages.yml").
     * @return The loaded ConfigFile instance.
     */
    public ConfigFile loadConfig(String fileName) {
        // Remove from cache to force reload
        ConfigFile existing = configs.get(fileName);
        if (existing != null) {
            existing.reload();
            return existing;
        }

        // Create a new ConfigFile instance
        ConfigFile configFile = new ConfigFile(plugin, configFolder, fileName);
        configs.put(fileName, configFile);
        return configFile;
    }

    /**
     * Retrieves a configuration file, loading it if not already loaded.
     *
     * @param fileName The name of the configuration file.
     * @return The ConfigFile instance for the specified file.
     */
    public ConfigFile getConfig(String fileName) {
        return configs.computeIfAbsent(fileName, this::loadConfig);
    }

    /**
     * Retrieves the default config.yml file.
     *
     * @return The ConfigFile instance for the default configuration.
     */
    public ConfigFile getConfig() {
        return getConfig("config.yml");
    }

    /**
     * Reloads all loaded configuration files.
     *
     * This method is useful for commands that require refreshing all configurations.
     */
    public void reloadAll() {
        configs.values().forEach(ConfigFile::reload);
    }

    /**
     * Saves all loaded configuration files to disk.
     *
     * This method is typically called when the plugin is disabled to ensure
     * all changes are persisted.
     */
    public void saveAll() {
        configs.values().forEach(ConfigFile::save);
    }
}