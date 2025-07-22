package gg.nextforge.database.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Repository<T> {

    private final Connection connection;
    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    public Repository(Connection connection) {
        this.connection = connection;
    }

    protected abstract String getTableName();
    protected abstract T map(ResultSet rs) throws SQLException;

    public CompletableFuture<List<T>> findAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<T> results = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + getTableName());
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(map(rs));
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to fetch all from " + getTableName(), e);
            }
            return results;
        });
    }

    public CompletableFuture<Optional<T>> findByIdAsync(Object id, String idColumn) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM " + getTableName() + " WHERE " + idColumn + " = ?")) {
                stmt.setObject(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(map(rs));
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to fetch by id from " + getTableName(), e);
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<Void> executeUpdateAsync(String sql, Object... params) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to execute update", e);
            }
        });
    }

    public CompletableFuture<List<T>> executeQueryAsync(String sql, Function<ResultSet, T> mapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            List<T> results = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapper.apply(rs));
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to execute query", e);
            }
            return results;
        });
    }
}