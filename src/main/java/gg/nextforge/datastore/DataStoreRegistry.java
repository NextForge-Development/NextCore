package gg.nextforge.datastore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Central registry for reusable data stores.
 */
public class DataStoreRegistry {

    private static final Map<String, DataStore<?, ?>> stores = new ConcurrentHashMap<>();

    public static <K, V> DataStore<K, V> createExpiring(String name, long duration, TimeUnit unit) {
        DataStore<K, V> store = DataStore.expiring(duration, unit);
        stores.put(name, store);
        return store;
    }

    public static <K, V> DataStore<K, V> createPersistent(String name) {
        DataStore<K, V> store = DataStore.persistent();
        stores.put(name, store);
        return store;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> DataStore<K, V> get(String name) {
        return (DataStore<K, V>) stores.get(name);
    }

    public static void shutdownAll() {
        stores.values().forEach(DataStore::shutdown);
        stores.clear();
    }

    // Helper for UUID-keyed stores
    public static <V> DataStore<UUID, V> createUUIDStore(String name, long duration, TimeUnit unit) {
        return createExpiring(name, duration, unit);
    }
}
