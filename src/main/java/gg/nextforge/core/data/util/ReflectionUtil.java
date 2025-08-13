// gg/nextforge/core/data/util/ReflectionUtil.java
package gg.nextforge.core.data.util;

import gg.nextforge.core.data.annotations.DataClass;
import gg.nextforge.core.data.annotations.PrimaryKey;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectionUtil {

    private static final Map<Class<?>, Field> PK_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, DataClass> DATACLASS_CACHE = new ConcurrentHashMap<>();

    private ReflectionUtil() {}

    /* ---------------- Primary Key ---------------- */

    public static Optional<Field> getPrimaryKeyField(Class<?> clazz) {
        return Optional.ofNullable(PK_CACHE.computeIfAbsent(clazz, ReflectionUtil::findPrimaryKeyField));
    }

    public static Optional<String> getPrimaryKeyName(Class<?> clazz) {
        return getPrimaryKeyField(clazz)
                .map(field -> {
                    PrimaryKey pk = field.getAnnotation(PrimaryKey.class);
                    if (pk != null && !pk.name().isEmpty()) return pk.name();
                    if (pk != null && pk.mongoId()) return "_id";
                    return field.getName();
                });
    }

    private static Field findPrimaryKeyField(Class<?> clazz) {
        // Suche in der Klasse
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(PrimaryKey.class)) return field;
        }
        // Suche in Superklassen
        Class<?> current = clazz.getSuperclass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(PrimaryKey.class)) return field;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /* ---------------- DataClass Infos ---------------- */

    private static DataClass getDataClassAnnotation(Class<?> clazz) {
        return DATACLASS_CACHE.computeIfAbsent(clazz, c -> c.getAnnotation(DataClass.class));
    }

    public static String getTableName(Class<?> clazz) {
        DataClass dc = getDataClassAnnotation(clazz);
        if (dc != null && !dc.table().isEmpty()) return dc.table();
        return clazz.getSimpleName().toLowerCase();
    }

    public static String getCollectionName(Class<?> clazz) {
        DataClass dc = getDataClassAnnotation(clazz);
        if (dc != null && !dc.collection().isEmpty()) return dc.collection();
        return clazz.getSimpleName().toLowerCase();
    }

    public static String getJsonFileName(Class<?> clazz) {
        DataClass dc = getDataClassAnnotation(clazz);
        if (dc != null && !dc.file().isEmpty()) return dc.file() + ".json";
        return clazz.getSimpleName().toLowerCase() + ".json";
    }

    public static String getEntityName(Class<?> clazz) {
        DataClass dc = getDataClassAnnotation(clazz);
        if (dc != null && !dc.value().isEmpty()) return dc.value();
        return clazz.getSimpleName();
    }
}
