// gg/nextforge/core/data/storage/jdbc/IndexApplier.java
package gg.nextforge.core.data.storage.jdbc;

import gg.nextforge.core.data.util.IndexUtil;
import gg.nextforge.core.data.util.ReflectionUtil;

import java.sql.*;
import java.util.*;

public final class IndexApplier {
    public enum Dialect { MYSQL, H2 }
    private IndexApplier() {}

    public static <T> void ensureIndexes(Connection c, Class<T> type, Dialect dialect) throws Exception {
        String table = ReflectionUtil.getTableName(type);
        Map<String, Existing> existing = loadExisting(c, table);

        for (IndexUtil.Def def : IndexUtil.indexesFor(type)) {
            // Name ist deterministisch → reicht zum Abgleich
            if (!existing.containsKey(def.name())) {
                String sql = createIndexSql(table, def, dialect);
                try (Statement st = c.createStatement()) { st.executeUpdate(sql); }
            }
        }
    }

    private record Existing(String name, boolean unique, List<String> cols) {}

    private static Map<String, Existing> loadExisting(Connection c, String table) throws SQLException {
        Map<String, Existing> map = new HashMap<>();
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getIndexInfo(null, null, table, false, false)) {
            // GROUP BY INDEX_NAME
            Map<String, List<IndexRow>> rows = new LinkedHashMap<>();
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                if (name == null) continue; // PK index ggf. null
                boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                String col = rs.getString("COLUMN_NAME");
                rows.computeIfAbsent(name, k -> new ArrayList<>())
                        .add(new IndexRow(name, !nonUnique, col, rs.getShort("ORDINAL_POSITION")));
            }
            for (var e : rows.entrySet()) {
                List<IndexRow> l = e.getValue();
                l.sort(Comparator.comparingInt(o -> o.pos));
                map.put(e.getKey(), new Existing(e.getKey(), l.get(0).unique, l.stream().map(r -> r.col).toList()));
            }
        }
        return map;
    }

    private record IndexRow(String name, boolean unique, String col, int pos) {}

    private static String ident(String s, Dialect d) { return s; }

    private static String createIndexSql(String table, IndexUtil.Def def, Dialect d) {
        String cols = String.join(",", def.columns());
        String base = (def.unique() ? "CREATE UNIQUE INDEX " : "CREATE INDEX ");
        // MySQL hat kein IF NOT EXISTS für CREATE INDEX → wir prüfen vorher per Metadata.
        return base + ident(def.name(), d) + " ON " + ident(table, d) + " (" + cols + ")";
    }
}
