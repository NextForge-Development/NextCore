package gg.nextforge.datastore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class DataStore<K, V> {

    private final Map<K, V> store = new ConcurrentHashMap<>();
    private final Map<K, Long> expiryMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final long expiryMillis;

    private DataStore(long expiryMillis) {
        this.expiryMillis = expiryMillis;
        startCleaner();
    }

    public static <K, V> DataStore<K, V> expiring(long duration, TimeUnit unit) {
        return new DataStore<>(unit.toMillis(duration));
    }

    public static <K, V> DataStore<K, V> persistent() {
        return new DataStore<>(-1);
    }

    public void put(K key, V value) {
        store.put(key, value);
        if (expiryMillis > 0) {
            expiryMap.put(key, System.currentTimeMillis() + expiryMillis);
        }
    }

    public Optional<V> get(K key) {
        if (isExpired(key)) {
            remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(key));
    }

    public V getOrCreate(K key, Supplier<V> supplier) {
        return get(key).orElseGet(() -> {
            V value = supplier.get();
            put(key, value);
            return value;
        });
    }

    public void remove(K key) {
        store.remove(key);
        expiryMap.remove(key);
    }

    public boolean contains(K key) {
        return get(key).isPresent();
    }

    public int size() {
        return store.size();
    }

    private boolean isExpired(K key) {
        if (expiryMillis < 0) return false;
        return Optional.ofNullable(expiryMap.get(key))
                .map(expiry -> System.currentTimeMillis() > expiry)
                .orElse(false);
    }

    private void startCleaner() {
        if (expiryMillis < 0) return;
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            expiryMap.entrySet().removeIf(entry -> {
                boolean expired = now > entry.getValue();
                if (expired) store.remove(entry.getKey());
                return expired;
            });
        }, expiryMillis, expiryMillis, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
