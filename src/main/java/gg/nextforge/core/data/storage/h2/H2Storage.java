package gg.nextforge.core.data.storage.h2;

import gg.nextforge.core.data.storage.jdbc.JdbcStorage;

public class H2Storage<T, ID> extends JdbcStorage<T, ID> {
    /** url example: jdbc:h2:./data/mydb;MODE=MySQL;DATABASE_TO_UPPER=false */
    public H2Storage(Class<T> type, String url, String user, String pass) {
        super(type, url, user, pass);
    }
}