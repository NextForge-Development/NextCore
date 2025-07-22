package gg.nextforge.database.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UnitOfWork {

    private static final Logger LOGGER = Logger.getLogger("UnitOfWork");

    private final Connection connection;

    public UnitOfWork(Connection connection) {
        this.connection = connection;
    }

    public void execute(Consumer<Connection> action) {
        try {
            connection.setAutoCommit(false);
            action.accept(connection);
            connection.commit();
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                LOGGER.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
            }
            LOGGER.log(Level.SEVERE, "Transaction failed", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to reset auto-commit", e);
            }
        }
    }
}