package gg.nextforge.database.registry;

import gg.nextforge.database.repository.Repository;
import gg.nextforge.database.repository.RepositoryProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RepositoryRegistry {

    private static final Logger LOGGER = Logger.getLogger("RepositoryRegistry");
    private final Map<Class<?>, RepositoryProvider<?>> registry = new HashMap<>();

    public void registerProvider(RepositoryProvider<?> provider) {
        Class<?> entityType = provider.getEntityType();
        if (entityType != null) {
            registry.put(entityType, provider);
            LOGGER.info("Registered repository provider for: " + entityType.getSimpleName());
        } else {
            LOGGER.warning("Repository provider has null entity type: " + provider.getClass().getSimpleName());
        }
    }

    public <T> RepositoryProvider<T> getProvider(Class<T> entityType) {
        return (RepositoryProvider<T>) registry.get(entityType);
    }

    public <T> Repository<T> getSqlRepository(Class<T> entityType) {
        RepositoryProvider<?> provider = registry.get(entityType);
        if (provider instanceof Repository) {
            return (Repository<T>) provider;
        }
        return null;
    }


    public Set<Class<?>> getRegisteredTypes() {
        return registry.keySet();
    }

    public void clear() {
        registry.clear();
    }
}