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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * Represents a single configuration file.
 * This class is thread-safe and supports auto-reloading.
 */
public class ConfigFile {
    private final Plugin plugin; // The plugin instance associated with this config file
    private final String fileName; // The name of the configuration file
    private final File file; // The file object representing the configuration file
    private final ReadWriteLock lock = new ReentrantReadWriteLock(); // Lock for thread-safe access
    private FileConfiguration config; // The loaded configuration
    private long lastModified = 0; // Timestamp of the last modification

    /**
     * Constructs a ConfigFile instance.
     *
     * @param plugin       The plugin instance.
     * @param configFolder The folder where the configuration file is located.
     * @param fileName     The name of the configuration file.
     */
    ConfigFile(Plugin plugin, File configFolder, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.file = new File(configFolder, fileName);

        // Save default config from jar if it doesn't exist
        if (!file.exists()) {
            saveDefaultConfig();
        }

        // Load the configuration
        reload();
    }

    /**
     * Saves the default configuration from jar resources.
     * If no default exists, create an empty file.
     */
    private void saveDefaultConfig() {
        try {
            // Try to get the resource from the jar
            InputStream resource = plugin.getResource(fileName);
            if (resource != null) {
                Files.copy(resource, file.toPath());
                resource.close();
            } else {
                // No default resource, create an empty file
                file.createNewFile();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to save default config: " + fileName, e);
        }
    }

    /**
     * Reloads the configuration from disk.
     * This method is thread-safe and blocks writers during reload.
     */
    public void reload() {
        lock.writeLock().lock();
        try {
            config = YamlConfiguration.loadConfiguration(file);
            lastModified = file.lastModified();

            // Load defaults from jar if available
            InputStream defConfigStream = plugin.getResource(fileName);
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)
                );
                config.setDefaults(defConfig);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Saves the configuration to disk.
     * This method is thread-safe and blocks all access during save.
     */
    public void save() {
        lock.writeLock().lock();
        try {
            config.save(file);
            lastModified = file.lastModified();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to save config: " + fileName, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Checks if the file has been modified externally.
     * Useful for auto-reload features.
     *
     * @return true if the file has been modified externally, false otherwise.
     */
    public boolean isModifiedExternally() {
        return file.lastModified() > lastModified;
    }

    /**
     * Retrieves a string value from the configuration.
     * This method is thread-safe.
     *
     * @param path The path to the configuration value.
     * @param def  The default value if the path does not exist.
     * @return The string value at the specified path, or the default value.
     */
    public String getString(String path, String def) {
        lock.readLock().lock();
        try {
            return config.getString(path, def);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Retrieves an integer value from the configuration.
     * This method is thread-safe.
     *
     * @param path The path to the configuration value.
     * @param def  The default value if the path does not exist.
     * @return The integer value at the specified path, or the default value.
     */
    public int getInt(String path, int def) {
        lock.readLock().lock();
        try {
            return config.getInt(path, def);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Retrieves a double value from the configuration.
     * This method is thread-safe.
     *
     * @param path The path to the configuration value.
     * @param def  The default value if the path does not exist.
     * @return The double value at the specified path, or the default value.
     */
    public double getDouble(String path, double def) {
        lock.readLock().lock();
        try {
            return config.getDouble(path, def);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Retrieves a boolean value from the configuration.
     * This method is thread-safe.
     *
     * @param path The path to the configuration value.
     * @param def  The default value if the path does not exist.
     * @return The boolean value at the specified path, or the default value.
     */
    public boolean getBoolean(String path, boolean def) {
        lock.readLock().lock();
        try {
            return config.getBoolean(path, def);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Retrieves a list of strings from the configuration.
     * This method is thread-safe.
     *
     * @param path The path to the configuration value.
     * @return The list of strings at the specified path.
     */
    public List<String> getStringList(String path) {
        lock.readLock().lock();
        try {
            return config.getStringList(path);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets a value in the configuration and saves it to disk.
     * This method is thread-safe and auto-saves after each set.
     *
     * @param path  The path to the configuration value.
     * @param value The value to set.
     */
    public void set(String path, Object value) {
        lock.writeLock().lock();
        try {
            config.set(path, value);
            save(); // Auto-save after setting the value
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the raw FileConfiguration object.
     * WARNING: This method is not thread-safe! Use with caution.
     *
     * @return The raw FileConfiguration object.
     */
    public FileConfiguration getRawConfig() {
        return config;
    }
}