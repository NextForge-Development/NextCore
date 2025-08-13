// gg/nextforge/core/data/storage/jdbc/JdbcStorage.java
package gg.nextforge.core.data.storage.jdbc;

import gg.nextforge.core.data.annotations.Transient;
import gg.nextforge.core.data.storage.Storage;
import gg.nextforge.core.data.util.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class JdbcStorage<T, ID> implements Storage<T, ID> {
    private static final Logger log = LoggerFactory.getLogger(JdbcStorage.class);

    private final Class<T> type;
    private final String url;
    private final String user;
    private final String pass;

    public JdbcStorage(Class<T> type, String url, String user, String pass) {
        this.type = type;
        this.url = url;
        this.user = user;
        this.pass = pass;
    }

    @Override public Class<T> entityType() { return type; }

    protected Connection conn() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    @Override public void init() throws Exception {
        try (Connection c = conn()) {
            SchemaGenerator.ensureTable(c, type);
            IndexApplier.ensureIndexes(c, type, detectDialect(c));
        }
    }

    private IndexApplier.Dialect detectDialect(Connection c) throws SQLException {
        String product = c.getMetaData().getDatabaseProductName().toLowerCase();
        if (product.contains("h2")) return IndexApplier.Dialect.H2;
        if (product.contains("mysql")) return IndexApplier.Dialect.MYSQL;
        if (product.contains("mariadb")) return IndexApplier.Dialect.MYSQL;
        throw new IllegalArgumentException("Unsupported DB dialect: " + product);
    }


    /* ---------- CRUD ---------- */

    @Override
    public T insert(T entity) throws Exception {
        String table = ReflectionUtil.getTableName(type);
        List<Field> fields = persistableFields();
        // PK muss gesetzt sein (kein Auto-Gen in dieser Basis)
        Field pkf = ReflectionUtil.getPrimaryKeyField(type)
                .orElseThrow(() -> new IllegalStateException("No @PrimaryKey on " + type));
        Object pkVal = valueOf(entity, pkf);
        if (pkVal == null) throw new IllegalStateException("Primary key must be set for insert");

        String cols = fields.stream().map(Field::getName).collect(Collectors.joining(","));
        String qs = fields.stream().map(f -> "?").collect(Collectors.joining(","));
        String sql = "INSERT INTO " + table + " (" + cols + ") VALUES (" + qs + ")";

        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, entity, fields);
            ps.executeUpdate();
        }
        return entity;
    }

    @Override
    public T update(T entity) throws Exception {
        String table = ReflectionUtil.getTableName(type);
        Field pkf = ReflectionUtil.getPrimaryKeyField(type)
                .orElseThrow(() -> new IllegalStateException("No @PrimaryKey on " + type));
        Object id = valueOf(entity, pkf);
        if (id == null) throw new IllegalStateException("Primary key must be set for update");

        List<Field> fields = persistableFields();
        String set = fields.stream()
                .filter(f -> f != pkf)
                .map(f -> f.getName() + "=?")
                .collect(Collectors.joining(","));

        String sql = "UPDATE " + table + " SET " + set + " WHERE " + pkf.getName() + "=?";

        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = bindSkippingPk(ps, entity, fields, pkf);
            setParam(ps, idx, id);
            ps.executeUpdate();
        }
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) throws Exception {
        String table = ReflectionUtil.getTableName(type);
        Field pkf = ReflectionUtil.getPrimaryKeyField(type)
                .orElseThrow(() -> new IllegalStateException("No @PrimaryKey on " + type));
        String sql = "SELECT * FROM " + table + " WHERE " + pkf.getName() + "=? LIMIT 1";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            setParam(ps, 1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<T> findAll(int limit, int offset) throws Exception {
        String table = ReflectionUtil.getTableName(type);
        String sql = "SELECT * FROM " + table + " LIMIT " + limit + " OFFSET " + offset;
        List<T> out = new ArrayList<>();
        try (Connection c = conn();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    @Override
    public boolean deleteById(ID id) throws Exception {
        String table = ReflectionUtil.getTableName(type);
        Field pkf = ReflectionUtil.getPrimaryKeyField(type)
                .orElseThrow(() -> new IllegalStateException("No @PrimaryKey on " + type));
        String sql = "DELETE FROM " + table + " WHERE " + pkf.getName() + "=?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            setParam(ps, 1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public long count() throws Exception {
        String table = ReflectionUtil.getTableName(type);
        try (Connection c = conn();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next(); return rs.getLong(1);
        }
    }

    @Override
    public boolean existsById(ID id) throws Exception {
        return findById(id).isPresent();
    }

    /* ---------- Transactions ---------- */

    @FunctionalInterface public interface SqlFunction<C, R> { R apply(C c) throws Exception; }

    public <R> R inTransaction(SqlFunction<Connection, R> work) throws Exception {
        try (Connection c = conn()) {
            boolean old = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                R r = work.apply(c);
                c.commit();
                return r;
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(old);
            }
        }
    }

    /* --- Parallel-IO (Batch Save parallel, transactional optional) --- */

    @Override
    public java.util.List<T> saveAll(java.util.Collection<T> entities) throws Exception {
        // Default: parallel ohne TX (schnell). Für starke Konsistenz: inTransaction(...) + sequential.
        var pool = java.util.concurrent.Executors.newFixedThreadPool(Math.min(8, Math.max(2, Runtime.getRuntime().availableProcessors())));
        try {
            var futures = entities.stream().map(e ->
                    pool.submit(() -> { try { return save(e); } catch (Exception ex) { throw new RuntimeException(ex); } })
            ).toList();
            java.util.List<T> out = new java.util.ArrayList<>(futures.size());
            for (var f : futures) out.add(f.get());
            return out;
        } finally { pool.shutdown(); }
    }

    public java.util.List<T> saveAllTx(java.util.Collection<T> entities) throws Exception {
        return inTransaction(c -> {
            java.util.List<T> out = new java.util.ArrayList<>(entities.size());
            for (T e : entities) out.add(save(e)); // sequential, eine TX
            return out;
        });
    }

    /* ---------- Helpers ---------- */

    private List<Field> persistableFields() {
        List<Field> fs = new ArrayList<>();
        Class<?> c = type;
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (!f.isAnnotationPresent(Transient.class)) fs.add(f);
            }
            c = c.getSuperclass();
        }
        // Stabil: PK zuerst lassen? Für Insert egal. Für Update skippen wir explizit.
        return fs;
    }

    private void bind(PreparedStatement ps, T entity, List<Field> fields) throws Exception {
        int i = 1;
        for (Field f : fields) {
            setParam(ps, i++, valueOf(entity, f));
        }
    }

    private int bindSkippingPk(PreparedStatement ps, T entity, List<Field> fields, Field pkf) throws Exception {
        int i = 1;
        for (Field f : fields) {
            if (f == pkf) continue;
            setParam(ps, i++, valueOf(entity, f));
        }
        return i;
    }

    private Object valueOf(T entity, Field f) {
        try { f.setAccessible(true); return f.get(entity); }
        catch (IllegalAccessException e) { throw new RuntimeException(e); }
    }

    private void setParam(PreparedStatement ps, int idx, Object v) throws SQLException {
        if (v == null) { ps.setObject(idx, null); return; }
        if (v instanceof UUID u) { ps.setString(idx, u.toString()); return; }
        if (v instanceof Instant t) { ps.setTimestamp(idx, Timestamp.from(t)); return; }
        if (v instanceof Enum<?> e) { ps.setString(idx, e.name()); return; }
        ps.setObject(idx, v);
    }

    private T mapRow(ResultSet rs) throws Exception {
        Constructor<T> ctor = type.getDeclaredConstructor();
        ctor.setAccessible(true);
        T instance = ctor.newInstance();
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        Map<String, Integer> nameToIndex = new HashMap<>();
        for (int i = 1; i <= cols; i++) nameToIndex.put(md.getColumnLabel(i), i);

        Class<?> c = type;
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(Transient.class)) continue;
                f.setAccessible(true);
                String col = f.getName();
                Integer idx = nameToIndex.get(col);
                if (idx == null) continue;
                Object raw = rs.getObject(idx);
                if (raw == null) { f.set(instance, null); continue; }
                if (f.getType() == UUID.class && raw instanceof String s) {
                    f.set(instance, UUID.fromString(s)); continue;
                }
                if (f.getType() == Instant.class && (raw instanceof Timestamp ts)) {
                    f.set(instance, ts.toInstant()); continue;
                }
                if (f.getType().isEnum() && raw instanceof String s) {
                    @SuppressWarnings({"unchecked","rawtypes"})
                    Object enumVal = Enum.valueOf((Class<Enum>) f.getType(), s);
                    f.set(instance, enumVal); continue;
                }
                f.set(instance, raw);
            }
            c = c.getSuperclass();
        }
        return instance;
    }
}
