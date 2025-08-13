package gg.nextforge.core.plugin.inject;

import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceRegistry {

    private final Map<Key, Object> services = new ConcurrentHashMap<>();

    public <T> void register(Class<T> type, T instance) {
        services.put(Key.of(type, ""), instance);
    }

    public <T> void register(Class<T> type, String name, T instance) {
        services.put(Key.of(type, name), instance);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Class<T> type) {
        Object o = services.get(Key.of(type, ""));
        return Optional.ofNullable((T) o);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Class<T> type, String name) {
        Object o = services.get(Key.of(type, name));
        return Optional.ofNullable((T) o);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreate(Class<T> type) {
        return (T) services.computeIfAbsent(Key.of(type, ""), k -> newInstance(type));
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create instance for " + type.getName(), e);
        }
    }

    public Map<Class<?>, Object> getAll() {
        // Gibt eine unveränderliche Kopie aller Services zurück
        Map<Class<?>, Object> map = new LinkedHashMap<>();
        for (var e : services.entrySet()) {
            map.put(e.getKey().type(), e.getValue());
        }
        return Collections.unmodifiableMap(map);
    }


    /* --- key --- */
    private record Key(Class<?> type, String name) {
        static Key of(Class<?> t, String n) { return new Key(t, n == null ? "" : n); }
    }
}
