package gg.nextforge.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger("Database");

    public enum DatabaseType {
        MYSQL, H2, SQLITE, MONGODB, REDISSON
    }

    @Getter
    private final DatabaseType type;
    private final String connectionString;
    private Connection sqlConnection;
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private RedissonClient redissonClient;

    public DatabaseManager(DatabaseType type, String connectionString) {
        this.type = type;
        this.connectionString = connectionString;
        initialize();
    }

    private void initialize() {
        try {
            switch (type) {
                case MYSQL:
                case H2:
                case SQLITE:
                    sqlConnection = DriverManager.getConnection(connectionString);
                    break;
                case MONGODB:
                    String uri = (connectionString);
                    String databaseName = uri.substring(uri.lastIndexOf("/") + 1);
                    mongoClient = MongoClients.create(uri);
                    mongoDatabase = mongoClient.getDatabase(databaseName);
                    break;
                case REDISSON:
                    Config config = new Config();
                    config.useSingleServer().setAddress(connectionString);
                    redissonClient = Redisson.create(config);
                    break;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database connection", e);
        }
    }

    public Connection getSqlConnection() {
        return sqlConnection;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    public void close() {
        try {
            if (sqlConnection != null && !sqlConnection.isClosed()) {
                sqlConnection.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to close SQL connection", e);
        }

        if (mongoClient != null) {
            mongoClient.close();
        }

        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    public boolean isConnected() {
        switch (type) {
            case MYSQL:
            case H2:
            case SQLITE:
                try {
                    return sqlConnection != null && !sqlConnection.isClosed();
                } catch (SQLException e) {
                    return false;
                }
            case MONGODB:
                return mongoClient != null;
            case REDISSON:
                return redissonClient != null && !redissonClient.isShutdown();
            default:
                return false;
        }
    }
}
