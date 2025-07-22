package gg.nextforge.database.bootstrap;

import gg.nextforge.database.entity.EntityManager;
import gg.nextforge.database.entity.EntityScanner;

import java.sql.Connection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntityBootstrapper {

    private static final Logger LOGGER = Logger.getLogger("EntityBootstrapper");

    private final Connection connection;
    private final String basePackage;

    public EntityBootstrapper(Connection connection, String basePackage) {
        this.connection = connection;
        this.basePackage = basePackage;
    }

    public void initialize() {
        try {
            EntityScanner scanner = new EntityScanner();
            List<Class<?>> entities = scanner.findEntities(basePackage);
            EntityManager manager = new EntityManager(connection);

            for (Class<?> entity : entities) {
                try {
                    manager.createSchema(entity);
                    LOGGER.info("Schema created for entity: " + entity.getSimpleName());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to create schema for " + entity.getSimpleName(), e);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to bootstrap entities", e);
        }
    }
}