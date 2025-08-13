package gg.nextforge.core.data.storage.jdbc;

import gg.nextforge.core.data.annotations.PrimaryKey;
import gg.nextforge.core.data.annotations.Transient;
import gg.nextforge.core.data.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

public final class SchemaGenerator {
    private SchemaGenerator() {}

    public static <T> void ensureTable(Connection c, Class<T> type) throws Exception {
        String table = ReflectionUtil.getTableName(type);
        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(table).append(" (");
        Field pk = ReflectionUtil.getPrimaryKeyField(type)
                .orElseThrow(() -> new IllegalStateException("No @PrimaryKey on " + type));

        boolean first = true;
        for (Class<?> cur = type; cur != null && cur != Object.class; cur = cur.getSuperclass()) {
            for (Field f : cur.getDeclaredFields()) {
                if (f.isAnnotationPresent(Transient.class)) continue;
                if (!first) ddl.append(", ");
                first = false;
                ddl.append(f.getName()).append(" ").append(sqlType(f.getType()));
                if (f == pk) ddl.append(" PRIMARY KEY");
            }
        }
        ddl.append(")");
        try (Statement st = c.createStatement()) { st.executeUpdate(ddl.toString()); }
    }

    private static String sqlType(Class<?> t) {
        if (t == String.class) return "VARCHAR(255)";
        if (t == int.class || t == Integer.class) return "INT";
        if (t == long.class || t == Long.class) return "BIGINT";
        if (t == boolean.class || t == Boolean.class) return "BOOLEAN";
        if (t == double.class || t == Double.class) return "DOUBLE";
        if (t == float.class || t == Float.class) return "REAL";
        if (t == java.math.BigDecimal.class) return "DECIMAL(38,10)";
        if (t == Instant.class) return "TIMESTAMP";
        if (t.isEnum()) return "VARCHAR(64)";
        if (t == UUID.class) return "VARCHAR(36)";
        // Fallback
        return "VARCHAR(255)";
    }
}
