package gg.nextforge.core.data.util;

import gg.nextforge.core.data.annotations.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public final class IndexUtil {
    private IndexUtil() {}

    public record Def(String name, boolean unique, List<String> columns) {}

    public static <T> List<Def> indexesFor(Class<T> type) {
        String table = ReflectionUtil.getTableName(type);
        List<Def> out = new ArrayList<>();

        // FIELD-Level (@Index / @Unique)
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                // @Index
                Index[] idxs = f.getAnnotationsByType(Index.class);
                for (Index idx : idxs) {
                    out.add(normalize(table, idx.name(), idx.unique(), List.of(f.getName())));
                }
                // @Unique -> unique Index
                Unique[] uqs = f.getAnnotationsByType(Unique.class);
                for (Unique uq : uqs) {
                    out.add(normalize(table, uq.name(), true, List.of(f.getName())));
                }
            }
        }

        // TYPE-Level (@Index / @Unique) – Spalten per columns()
        assert type != null;
        Index[] classIdx = type.getAnnotationsByType(Index.class);
        for (Index idx : classIdx) {
            if (idx.columns().length == 0) continue;
            out.add(normalize(table, idx.name(), idx.unique(), Arrays.asList(idx.columns())));
        }
        Unique[] classUq = type.getAnnotationsByType(Unique.class);
        for (Unique uq : classUq) {
            if (uq.columns().length == 0) continue;
            out.add(normalize(table, uq.name(), true, Arrays.asList(uq.columns())));
        }

        // Dedupe (gleiche Spalten + unique)
        return out.stream()
                .collect(Collectors.toMap(
                        d -> key(d.unique, d.columns),
                        d -> d, (a,b) -> a, LinkedHashMap::new))
                .values().stream().toList();
    }

    private static Def normalize(String table, String name, boolean unique, List<String> columns) {
        List<String> cols = List.copyOf(columns);
        String auto = (unique ? "uq_" : "idx_") + table + "_" + String.join("_", cols);
        return new Def(name == null || name.isBlank() ? auto : name, unique, cols);
    }

    private static String key(boolean unique, List<String> cols) {
        return (unique ? "U:" : "I:") + String.join(",", cols);
    }
}
