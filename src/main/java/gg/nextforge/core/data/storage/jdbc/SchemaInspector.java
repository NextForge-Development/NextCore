package gg.nextforge.core.data.storage.jdbc;

import gg.nextforge.core.data.annotations.Transient;
import gg.nextforge.core.data.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class SchemaInspector {
    public enum Dialect { MYSQL, H2 }

    public record Column(String name, String type, boolean nullable) {}
    public record Diff(List<String> alterSql) {}

    private SchemaInspector() {}

    /* ---------- Public API ---------- */

    /** Vergleicht DB-Tabelle mit Entity und liefert ALTER SQL (ADD/MODIFY, optional DROP). */
    public static <T> Diff diff(Connection c, Class<T> type, Dialect dialect, boolean dropUnknownColumns) throws Exception {
        String table = ReflectionUtil.getTableName(type);
        Map<String, Column> actual = describeTable(c, table);
        Map<String, Column> expected = expectedColumns(type);

        List<String> alters = new ArrayList<>();

        // ADD fehlender Spalten
        for (var e : expected.entrySet()) {
            if (!actual.containsKey(e.getKey())) {
                alters.add(addColumnSql(table, e.getValue(), dialect));
            }
        }

        // MODIFY Typ/Nullability
        for (var e : expected.entrySet()) {
            Column got = actual.get(e.getKey());
            if (got == null) continue;
            if (!equalsIgnoreCaseTrim(got.type(), e.getValue().type()) || got.nullable() != e.getValue().nullable()) {
                alters.add(modifyColumnSql(table, e.getValue(), dialect));
            }
        }

        // DROP unbekannter Spalten
        if (dropUnknownColumns) {
            for (var a : actual.keySet()) {
                if (!expected.containsKey(a)) {
                    alters.add("ALTER TABLE " + ident(table, dialect) + " DROP COLUMN " + ident(a, dialect));
                }
            }
        }

        return new Diff(alters);
    }

    /** Führt die erzeugten ALTER-Statements aus (in einer Transaktion). */
    public static void apply(Connection c, Diff diff) throws Exception {
        if (diff.alterSql().isEmpty()) return;
        boolean old = c.getAutoCommit();
        c.setAutoCommit(false);
        try (Statement st = c.createStatement()) {
            for (String sql : diff.alterSql()) st.executeUpdate(sql);
            c.commit();
        } catch (Exception ex) {
            c.rollback();
            throw ex;
        } finally {
            c.setAutoCommit(old);
        }
    }

    /* ---------- Intern ---------- */

    private static Map<String, Column> describeTable(Connection c, String table) throws SQLException {
        Map<String, Column> cols = new LinkedHashMap<>();
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getColumns(null, null, table, null)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                String type = normalizeType(rs.getString("TYPE_NAME"), rs.getInt("COLUMN_SIZE"), rs.getInt("DECIMAL_DIGITS"));
                boolean nullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                cols.put(name, new Column(name, type, nullable));
            }
        }
        return cols;
    }

    private static <T> Map<String, Column> expectedColumns(Class<T> type) {
        Map<String, Column> cols = new LinkedHashMap<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(Transient.class)) continue;
                String name = f.getName();
                String sqlType = sqlType(f.getType());
                boolean nullable = !f.getType().isPrimitive(); // simple heuristic
                cols.put(name, new Column(name, sqlType, nullable));
            }
        }
        return cols;
    }

    private static String addColumnSql(String table, Column c, Dialect d) {
        return "ALTER TABLE " + ident(table, d) + " ADD COLUMN " + ident(c.name(), d) + " " + c.type() + (c.nullable() ? "" : " NOT NULL");
    }

    private static String modifyColumnSql(String table, Column c, Dialect d) {
        return switch (d) {
            case MYSQL -> "ALTER TABLE " + ident(table, d) + " MODIFY " + ident(c.name(), d) + " " + c.type() + (c.nullable() ? "" : " NOT NULL");
            case H2    -> "ALTER TABLE " + ident(table, d) + " ALTER COLUMN " + ident(c.name(), d) + " " + c.type() + (c.nullable() ? "" : " NOT NULL");
        };
    }

    private static String ident(String s, Dialect d) {
        // H2 ist i.d.R. case-insensitive; wir quoten nicht für Einfachheit
        return s;
    }

    /* ---- Type Mapping: konsistent mit SchemaGenerator ---- */
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
        if (t == java.util.UUID.class) return "VARCHAR(36)";
        return "VARCHAR(255)";
    }

    /** Vereinheitlicht DB-Typen wie VARCHAR / VARCHAR2 / CHARACTER VARYING(255) -> VARCHAR(255) */
    private static String normalizeType(String typeName, int size, int scale) {
        String t = typeName.toUpperCase(Locale.ROOT).trim();
        if (t.contains("CHAR")) return "VARCHAR(" + Math.max(1, size) + ")";
        if (t.startsWith("DEC") || t.startsWith("NUM")) return "DECIMAL(" + (size > 0 ? size : 38) + "," + Math.max(0, scale) + ")";
        if (t.startsWith("INT")) return "INT";
        if (t.startsWith("BIGINT")) return "BIGINT";
        if (t.startsWith("BOOL")) return "BOOLEAN";
        if (t.startsWith("DOUBLE")) return "DOUBLE";
        if (t.startsWith("REAL") || t.startsWith("FLOAT")) return "REAL";
        if (t.startsWith("TIMESTAMP") || t.startsWith("DATETIME")) return "TIMESTAMP";
        return t; // Fallback
    }

    private static boolean equalsIgnoreCaseTrim(String a, String b) {
        if (a == null || b == null) return Objects.equals(a, b);
        return a.replaceAll("\\s+", "").equalsIgnoreCase(b.replaceAll("\\s+", ""));
    }
}
