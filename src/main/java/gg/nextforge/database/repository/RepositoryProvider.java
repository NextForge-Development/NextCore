package gg.nextforge.database.repository;

public interface RepositoryProvider<T> {
    Class<T> getEntityType();
}
