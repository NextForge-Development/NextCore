package gg.nextforge.core;

import gg.nextforge.core.plugin.ForgedPlugin;
import gg.nextforge.core.plugin.dependency.model.DependencyArtifact;
import gg.nextforge.core.plugin.dependency.model.DependencyRepository;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NextCore extends ForgedPlugin {

    private static NextCore instance;

    public static NextCore getInstance() {
        return instance;
    }

    @Override
    public void enable() {
        if (instance != null) {
            throw new IllegalStateException("NextCore is already enabled!");
        }
        instance = this;

        // Register commands, listeners, etc.
        getLogger().info("NextCore has been enabled successfully.");
    }

    @Override
    public void disable() {
        if (instance == null) {
            throw new IllegalStateException("NextCore is not enabled!");
        }
        instance = null;

        // Cleanup resources, unregister listeners, etc.
        getLogger().info("NextCore has been disabled successfully.");
    }

    @Override
    public List<DependencyRepository> repositories() {
        return List.of();
    }

    @Override
    public Map<String, List<DependencyArtifact>> groups() {
        return Map.of();
    }

    @Override
    public UUID pluginId() {
        return null;
    }
}
