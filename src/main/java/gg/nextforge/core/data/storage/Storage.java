// gg/nextforge/core/data/storage/Storage.java
package gg.nextforge.core.data.storage;

import gg.nextforge.core.data.util.ReflectionUtil;

import java.util.*;
import java.util.stream.Collectors;

public interface Storage<T, ID> extends AutoCloseable {

    /** Entity-Typ (z.B. User.class) */
    Class<T> entityType();

    /** Optional: Ressourcen/Verbindungen aufbauen */
    default void init() throws Exception {}

    /* ---------- CRUD ---------- */

    /** Insert (wirft, wenn PK existiert) */
    T insert(T entity) throws Exception;

    /** Update (wirft, wenn PK nicht existiert) */
    T update(T entity) throws Exception;

    /** Upsert (falls nicht unterstützt, via existsById emulieren) */
    default T upsert(T entity) throws Exception {
        ID id = getId(entity).orElseThrow(() -> new IllegalStateException("No @PrimaryKey value"));
        return existsById(id) ? update(entity) : insert(entity);
    }

    /** Save = Upsert */
    default T save(T entity) throws Exception {
        return upsert(entity);
    }

    /** Batch Save */
    default List<T> saveAll(Collection<T> entities) throws Exception {
        List<T> out = new ArrayList<>(entities.size());
        for (T e : entities) out.add(save(e));
        return out;
    }

    Optional<T> findById(ID id) throws Exception;

    List<T> findAll(int limit, int offset) throws Exception;

    default List<T> findAll() throws Exception {
        return findAll(Integer.MAX_VALUE, 0);
    }

    boolean deleteById(ID id) throws Exception;

    default int deleteAllById(Collection<ID> ids) throws Exception {
        int n = 0; for (ID id : ids) if (deleteById(id)) n++;
        return n;
    }

    long count() throws Exception;

    boolean existsById(ID id) throws Exception;

    /* ---------- Helpers ---------- */

    /** Liest die PK per ReflectionUtil */
    @SuppressWarnings("unchecked")
    default Optional<ID> getId(T entity) {
        return ReflectionUtil.getPrimaryKeyField(entity.getClass()).map(f -> {
            try {
                f.setAccessible(true);
                return (ID) f.get(entity);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Extrahiert PKs aus Entities (für Batch-Operationen) */
    default List<ID> idsOf(Collection<T> entities) {
        return entities.stream()
                .map(this::getId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    default java.util.List<T> saveAllParallel(java.util.Collection<T> entities, int threads) throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(Math.max(1, threads));
        try {
            var futures = entities.stream().map(e -> pool.submit(() -> {
                try { return save(e); } catch (Exception ex) { throw new RuntimeException(ex); }
            })).toList();
            java.util.List<T> out = new java.util.ArrayList<>(futures.size());
            for (var f : futures) out.add(f.get());
            return out;
        } finally { pool.shutdown(); }
    }


    /** Ressourcen freigeben (DB-Connections etc.) */
    @Override
    default void close() throws Exception {}
}
