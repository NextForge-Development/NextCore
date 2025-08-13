package gg.nextforge.core.data.storage.mongodb;

import gg.nextforge.core.data.annotations.Transient;
import gg.nextforge.core.data.storage.Storage;
import gg.nextforge.core.data.util.ReflectionUtil;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

public class MongoDBStorage<T, ID> implements Storage<T, ID> {

    private final Class<T> type;
    private final MongoClient client;
    private final MongoDatabase db;
    private final MongoCollection<Document> coll;
    private final Field pkField;
    private final String pkName;

    private boolean useTransactions = false;

    public MongoDBStorage(Class<T> type, String connectionString, String database) {
        this(type, connectionString, database, false);
    }
    public MongoDBStorage(Class<T> type, String connectionString, String database, boolean useTransactions) {
        this.type = type;
        this.client = MongoClients.create(connectionString);
        this.db = client.getDatabase(database);
        String collection = ReflectionUtil.getCollectionName(type);
        this.coll = db.getCollection(collection);
        this.pkField = ReflectionUtil.getPrimaryKeyField(type).orElseThrow();
        this.pkName = ReflectionUtil.getPrimaryKeyName(type).orElse("_id");
        this.useTransactions = useTransactions;
    }

    @Override
    public void init() {
        ensureIndexesMongo();
    }

    private void ensureIndexesMongo() {
        // 1) Soll-Konfiguration aus Annotationen
        var desired = gg.nextforge.core.data.util.IndexUtil.indexesFor(type);

        // 2) Ist-Zustand aus Mongo lesen: Map<Name, Existing>
        Map<String, ExistingIndex> existing = new java.util.HashMap<>();
        for (org.bson.Document d : coll.listIndexes()) {
            String name = d.getString("name");
            if (name == null) continue;
            @SuppressWarnings("unchecked")
            org.bson.Document key = (org.bson.Document) d.get("key");
            boolean unique = Boolean.TRUE.equals(d.getBoolean("unique"));
            existing.put(name, new ExistingIndex(name, key, unique));
        }

        // 3) Abgleich + Erstellung/Neuanlage
        for (var def : desired) {
            String name = def.name();
            org.bson.conversions.Bson keys = com.mongodb.client.model.Indexes.ascending(def.columns().toArray(new String[0]));
            org.bson.Document keyDoc = toKeyDoc(def.columns());
            boolean needCreate = true;

            ExistingIndex ex = existing.get(name);
            if (ex != null) {
                // Vergleiche Keys & Unique
                boolean sameKeys = ex.key.equals(keyDoc);
                boolean sameUnique = ex.unique == def.unique();
                if (sameKeys && sameUnique) {
                    needCreate = false; // alles gut
                } else {
                    // Unterschiedlich: löschen und neu anlegen
                    coll.dropIndex(name);
                    needCreate = true;
                }
            }

            if (needCreate) {
                var opts = new com.mongodb.client.model.IndexOptions()
                        .name(name)
                        .unique(def.unique());
                coll.createIndex(keys, opts);
            }
        }
    }

    private static org.bson.Document toKeyDoc(java.util.List<String> cols) {
        var d = new org.bson.Document();
        for (String c : cols) d.append(c, 1); // ascending
        return d;
    }

    private record ExistingIndex(String name, Document key, boolean unique) {
    }

    @Override public Class<T> entityType() { return type; }
    @Override public void close() { client.close(); }

    @Override
    public T insert(T entity) throws Exception {
        pkField.setAccessible(true);
        Object id = pkField.get(entity);
        if (id == null && pkField.getType() == java.util.UUID.class) {
            pkField.set(entity, java.util.UUID.randomUUID());
        }
        if (useTransactions) {
            try (var session = client.startSession()) {
                return session.withTransaction(() -> { coll.insertOne(session, toDocument(entity)); return entity; });
            }
        } else {
            coll.insertOne(toDocument(entity));
            return entity;
        }
    }

    @Override
    public T update(T entity) throws Exception {
        Object id = getId(entity).orElseThrow();
        var filter = com.mongodb.client.model.Filters.eq(pkName, id instanceof java.util.UUID u ? u.toString() : id);
        if (useTransactions) {
            try (var session = client.startSession()) {
                return session.withTransaction(() -> { coll.replaceOne(session, filter, toDocument(entity)); return entity; });
            }
        } else {
            coll.replaceOne(filter, toDocument(entity));
            return entity;
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        Document d = coll.find(Filters.eq(pkName, id)).first();
        return d == null ? Optional.empty() : Optional.of(fromDocument(d));
    }

    @Override
    public List<T> findAll(int limit, int offset) {
        List<T> out = new ArrayList<>();
        try (MongoCursor<Document> it = coll.find().skip(offset).limit(limit).iterator()) {
            while (it.hasNext()) out.add(fromDocument(it.next()));
        }
        return out;
    }

    @Override
    public boolean deleteById(ID id) {
        return coll.deleteOne(Filters.eq(pkName, id)).getDeletedCount() > 0;
    }

    @Override
    public long count() {
        return coll.countDocuments();
    }

    @Override
    public boolean existsById(ID id) {
        return coll.find(Filters.eq(pkName, id)).limit(1).first() != null;
    }

    /* ---------- Mapping ---------- */

    private Document toDocument(T entity) {
        Document d = new Document();
        Class<?> c = type;
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(Transient.class)) continue;
                f.setAccessible(true);
                try {
                    Object v = f.get(entity);
                    String name = f == pkField && pkName.equals("_id") ? "_id" : f.getName();
                    if (v instanceof UUID u) v = u.toString();
                    if (v instanceof Instant t) v = Date.from(t);
                    if (v instanceof Enum<?> e) v = e.name();
                    d.put(name, v);
                } catch (IllegalAccessException ignored) {}
            }
            c = c.getSuperclass();
        }
        return d;
    }

    private T fromDocument(Document d) {
        try {
            Constructor<T> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            T inst = ctor.newInstance();

            Class<?> c = type;
            while (c != null && c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.isAnnotationPresent(Transient.class)) continue;
                    f.setAccessible(true);
                    String name = f == pkField && pkName.equals("_id") ? "_id" : f.getName();
                    Object v = d.get(name);
                    if (v == null) { f.set(inst, null); continue; }
                    if (f.getType() == UUID.class && v instanceof String s) {
                        f.set(inst, UUID.fromString(s)); continue;
                    }
                    if (f.getType() == Instant.class) {
                        if (v instanceof Date dt) { f.set(inst, dt.toInstant()); continue; }
                    }
                    if (f.getType().isEnum() && v instanceof String s) {
                        @SuppressWarnings({"rawtypes","unchecked"})
                        Object ev = Enum.valueOf((Class<Enum>) f.getType(), s);
                        f.set(inst, ev); continue;
                    }
                    f.set(inst, v);
                }
                c = c.getSuperclass();
            }
            return inst;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
