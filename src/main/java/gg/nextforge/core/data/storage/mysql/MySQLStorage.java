package gg.nextforge.core.data.storage.mysql;

import gg.nextforge.core.data.storage.jdbc.JdbcStorage;

public class MySQLStorage<T, ID> extends JdbcStorage<T, ID> {
    public MySQLStorage(Class<T> type, String host, int port, String db, String user, String pass) {
        super(type, "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                user, pass);
    }
}
