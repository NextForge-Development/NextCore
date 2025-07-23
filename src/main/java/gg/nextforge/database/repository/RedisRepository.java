package gg.nextforge.database.repository;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class RedisRepository<K, V> implements RepositoryProvider<V> {

    protected final RedissonClient redisson;
    protected final String mapName;
    private final Class<V> entityType;

    public RedisRepository(RedissonClient redisson, String mapName, Class<V> entityType) {
        this.redisson = redisson;
        this.mapName = mapName;
        this.entityType = entityType;
    }

    protected RMap<K, V> getMap() {
        return redisson.getMap(mapName);
    }

    public CompletableFuture<Void> saveAsync(K key, V value) {
        return CompletableFuture.runAsync(() -> getMap().put(key, value));
    }

    public CompletableFuture<V> findAsync(K key) {
        return CompletableFuture.supplyAsync(() -> getMap().get(key));
    }

    public CompletableFuture<List<V>> findAllAsync() {
        return CompletableFuture.supplyAsync(() -> getMap().values().stream().collect(Collectors.toList()));
    }

    public CompletableFuture<Void> deleteAsync(K key) {
        return CompletableFuture.runAsync(() -> getMap().remove(key));
    }

    public CompletableFuture<Boolean> containsKeyAsync(K key) {
        return CompletableFuture.supplyAsync(() -> getMap().containsKey(key));
    }

    public CompletableFuture<Void> clearAsync() {
        return CompletableFuture.runAsync(() -> getMap().clear());
    }

    @Override
    public Class<V> getEntityType() {
        return entityType;
    }
}
