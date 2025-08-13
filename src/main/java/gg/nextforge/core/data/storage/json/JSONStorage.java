// gg/nextforge/core/data/storage/json/JSONStorage.java
package gg.nextforge.core.data.storage.json;

import gg.nextforge.core.data.storage.Storage;
import gg.nextforge.core.data.util.ReflectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JSONStorage<T, ID> implements Storage<T, ID> {

    private final Class<T> type;
    private final Path file;
    private final ObjectMapper mapper;
    private final Map<ID, T> cache = new ConcurrentHashMap<>();

    public JSONStorage(Class<T> type, Path dir) {
        this.type = type;
        String name = ReflectionUtil.getJsonFileName(type);
        this.file = dir.resolve(name);
        this.mapper = new ObjectMapper()
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override public Class<T> entityType() { return type; }

    @Override
    public void init() throws Exception {
        Files.createDirectories(file.getParent());
        if (Files.exists(file)) load();
        else persist(); // create empty file
    }

    @Override
    public T insert(T entity) throws Exception {
        // Auto-UUID
        var pkf = gg.nextforge.core.data.util.ReflectionUtil.getPrimaryKeyField(type).orElseThrow();
        pkf.setAccessible(true);
        Object id = pkf.get(entity);
        if (id == null && pkf.getType() == java.util.UUID.class) {
            pkf.set(entity, java.util.UUID.randomUUID());
            id = pkf.get(entity);
        }
        if (cache.containsKey((ID) id)) throw new IllegalStateException("Duplicate PK: " + id);
        cache.put((ID) id, entity);
        persistAtomic();
        return entity;
    }


    @Override
    public T update(T entity) throws Exception {
        ID id = getId(entity).orElseThrow();
        if (!cache.containsKey(id)) throw new IllegalStateException("Not found: " + id);
        cache.put(id, entity);
        persistAtomic();
        return entity;
    }

    @Override
    public java.util.List<T> saveAll(java.util.Collection<T> entities) throws Exception {
        // Parallel befüllen, am Ende ein atomarer Persist
        entities.parallelStream().forEach(e -> {
            try {
                ID id = getId(e).orElse(null);
                if (id == null) {
                    var pkf = gg.nextforge.core.data.util.ReflectionUtil.getPrimaryKeyField(type).orElseThrow();
                    pkf.setAccessible(true);
                    if (pkf.getType() == java.util.UUID.class) {
                        try { pkf.set(e, java.util.UUID.randomUUID()); } catch (Exception ex) { throw new RuntimeException(ex); }
                    }
                    id = getId(e).orElseThrow();
                }
                cache.put(id, e);
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        persistAtomic();
        return new java.util.ArrayList<>(cache.values());
    }

    private void persistAtomic() throws java.io.IOException {
        var tmp = file.resolveSibling(file.getFileName() + ".tmp");
        java.util.List<T> all = new java.util.ArrayList<>(cache.values());
        java.nio.file.Files.createDirectories(file.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), new Snapshot<>(all));
        java.nio.file.Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public List<T> findAll(int limit, int offset) {
        return cache.values().stream().skip(offset).limit(limit).toList();
    }

    @Override
    public boolean deleteById(ID id) throws Exception {
        boolean removed = cache.remove(id) != null;
        if (removed) persist();
        return removed;
    }

    @Override
    public long count() { return cache.size(); }

    @Override
    public boolean existsById(ID id) { return cache.containsKey(id); }

    /* ---------- IO ---------- */

    private record Snapshot<T>(List<T> data) {}

    private void load() throws IOException {
        if (Files.size(file) == 0) return;
        Snapshot<T> snap = mapper.readValue(file.toFile(),
                mapper.getTypeFactory().constructParametricType(Snapshot.class, type));
        cache.clear();
        if (snap != null && snap.data != null) {
            for (T e : snap.data) {
                @SuppressWarnings("unchecked")
                ID id = (ID) ReflectionUtil.getPrimaryKeyField(type).map(f -> {
                    try { f.setAccessible(true); return f.get(e); } catch (Exception ex) { throw new RuntimeException(ex); }
                }).orElseThrow();
                cache.put(id, e);
            }
        }
    }

    private void persist() throws IOException {
        List<T> all = new ArrayList<>(cache.values());
        Files.createDirectories(file.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), new Snapshot<>(all));
    }
}
