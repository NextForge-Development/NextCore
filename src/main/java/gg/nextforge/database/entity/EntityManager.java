package gg.nextforge.database.entity;

import gg.nextforge.database.annotations.Column;
import gg.nextforge.database.annotations.Table;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EntityManager {

    private final Connection connection;

    public EntityManager(Connection connection) {
        this.connection = connection;
    }

    public void createSchema(Class<?> clazz) throws SQLException {
        if (!clazz.isAnnotationPresent(Table.class)) {
            throw new IllegalArgumentException("Class must be annotated with @Table");
        }

        Table tableAnnotation = clazz.getAnnotation(Table.class);
        String tableName = tableAnnotation.value();

        List<String> columns = new ArrayList<>();
        String primaryKey = null;

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Column.class)) continue;

            Column column = field.getAnnotation(Column.class);
            String name = column.value();
            String type = mapJavaTypeToSql(field.getType());

            columns.add(name + " " + type);

            if (column.id()) {
                primaryKey = name;
            }
        }

        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(tableName)
                .append(" (")
                .append(String.join(", ", columns));

        if (primaryKey != null) {
            builder.append(", PRIMARY KEY (").append(primaryKey).append(")");
        }

        builder.append(")");

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(builder.toString());
        }
    }

    private String mapJavaTypeToSql(Class<?> type) {
        if (type == String.class) return "VARCHAR(255)";
        if (type == int.class || type == Integer.class) return "INT";
        if (type == long.class || type == Long.class) return "BIGINT";
        if (type == boolean.class || type == Boolean.class) return "BOOLEAN";
        if (type == double.class || type == Double.class) return "DOUBLE";
        return "TEXT";
    }
}
