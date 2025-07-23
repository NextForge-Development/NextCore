package gg.nextforge.database.repository;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class MongoRepository<T> implements RepositoryProvider<T> {

    protected final MongoCollection<Document> collection;
    private final Class<T> entityType;

    public MongoRepository(MongoCollection<Document> collection, Class<T> entityType) {
        this.collection = collection;
        this.entityType = entityType;
    }

    protected abstract T fromDocument(Document doc);
    protected abstract Document toDocument(T entity);

    public CompletableFuture<List<T>> findAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<T> results = new ArrayList<>();
            for (Document doc : collection.find()) {
                results.add(fromDocument(doc));
            }
            return results;
        });
    }

    public CompletableFuture<T> findOneAsync(String key, Object value) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(new Document(key, value)).first();
            return doc != null ? fromDocument(doc) : null;
        });
    }

    public CompletableFuture<Void> insertOneAsync(T entity) {
        return CompletableFuture.runAsync(() -> collection.insertOne(toDocument(entity)));
    }

    public CompletableFuture<Void> deleteAsync(String key, Object value) {
        return CompletableFuture.runAsync(() -> collection.deleteOne(new Document(key, value)));
    }

    public CompletableFuture<Void> updateOneAsync(String key, Object value, T entity) {
        return CompletableFuture.runAsync(() -> collection.updateOne(
                new Document(key, value), new Document("$set", toDocument(entity))));
    }

    public CompletableFuture<List<T>> findByFilterAsync(Document filter, Function<Document, T> mapper) {
        return CompletableFuture.supplyAsync(() -> {
            List<T> results = new ArrayList<>();
            for (Document doc : collection.find(filter)) {
                results.add(mapper.apply(doc));
            }
            return results;
        });
    }

    @Override
    public Class<T> getEntityType() {
        return entityType;
    }

    public MongoCollection<Document> getCollection() {
        return collection;
    }
}
