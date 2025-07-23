package gg.nextforge.database.query;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class QueryBuilder {

    private final String table;
    private final List<String> where = new ArrayList<>();
    private final List<Object> params = new ArrayList<>();
    private String orderBy = "";
    private String limit = "";

    public QueryBuilder(String table) {
        this.table = table;
    }

    public QueryBuilder where(String column, String operator, Object value) {
        where.add(column + " " + operator + " ?");
        params.add(value);
        return this;
    }

    public QueryBuilder orderBy(String column, boolean ascending) {
        this.orderBy = "ORDER BY " + column + (ascending ? " ASC" : " DESC");
        return this;
    }

    public QueryBuilder limit(int limit) {
        this.limit = "LIMIT " + limit;
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder("SELECT * FROM ").append(table);
        if (!where.isEmpty()) {
            sb.append(" WHERE ");
            sb.append(String.join(" AND ", where));
        }
        if (!orderBy.isEmpty()) sb.append(" ").append(orderBy);
        if (!limit.isEmpty()) sb.append(" ").append(limit);
        return sb.toString();
    }

    public Object[] getParameters() {
        return params.toArray();
    }
}