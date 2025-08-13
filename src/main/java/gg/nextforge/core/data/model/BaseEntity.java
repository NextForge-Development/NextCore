package gg.nextforge.core.data.model;

import gg.nextforge.core.data.annotations.PrimaryKey;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseEntity implements Serializable {

    private static final Map<Class<?>, Field> PK_CACHE = new ConcurrentHashMap<>();

    private Instant createdAt;
    private Instant updatedAt;

    /* ---------- PrimaryKey API ---------- */

    public Optional<Object> primaryKey() {
        Field f = pkField();
        if (f == null) return Optional.empty();
        try {
            f.setAccessible(true);
            return Optional.ofNullable(f.get(this));
        } catch (IllegalAccessException e) {
            return Optional.empty();
        }
    }

    public void setPrimaryKey(Object value) {
        Field f = pkField();
        if (f == null) throw new IllegalStateException("No @PrimaryKey on " + getClass());
        try {
            f.setAccessible(true);
            f.set(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Field pkField() {
        return PK_CACHE.computeIfAbsent(getClass(), cls -> {
            for (Field f : cls.getDeclaredFields()) {
                if (f.isAnnotationPresent(PrimaryKey.class)) return f;
            }
            Class<?> c = cls.getSuperclass();
            while (c != null && c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.isAnnotationPresent(PrimaryKey.class)) return f;
                }
                c = c.getSuperclass();
            }
            return null;
        });
    }

    /* ---------- Timestamps ---------- */

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** Call bei Insert */
    public void markCreated() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    /** Call bei Update */
    public void markUpdated() {
        updatedAt = Instant.now();
    }

    /* ---------- Equality & Debug ---------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity other)) return false;

        // Gleichheit über PrimaryKey, falls vorhanden
        Optional<Object> a = this.primaryKey();
        Optional<Object> b = other.primaryKey();

        if (a.isPresent() && b.isPresent()) {
            return Objects.equals(getClass(), other.getClass()) && Objects.equals(a.get(), b.get());
        }
        return false; // ohne PK keine sinnvolle Gleichheit über Instanzen hinweg
    }

    @Override
    public int hashCode() {
        return primaryKey().map(Objects::hashCode).orElse(System.identityHashCode(this));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{pk=" + primaryKey().orElse(null) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt + "}";
    }
}
