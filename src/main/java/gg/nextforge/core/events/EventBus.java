package gg.nextforge.core.events;

import org.bukkit.event.EventPriority;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EventBus {

    private final Map<Class<?>, List<Subscription<?>>> subscribers = new ConcurrentHashMap<>();

    public <T> EventSubscription<T> subscribe(Class<T> eventType, EventPriority priority, Consumer<T> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(handler, "handler");

        Subscription<T> sub = new Subscription<>(eventType, priority, handler);
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(sub);
        subscribers.get(eventType).sort(Comparator.comparingInt((Subscription<?> s) -> s.priority.getSlot()).reversed());
        return sub;
    }

    public <T> void unsubscribe(EventSubscription<T> subscription) {
        if (subscription instanceof Subscription<?> sub) {
            List<Subscription<?>> list = subscribers.get(sub.eventType);
            if (list != null) list.remove(sub);
        }
    }

    public <T> void post(T event) {
        @SuppressWarnings("unchecked")
        List<Subscription<T>> list = (List<Subscription<T>>) (List<?>) subscribers.getOrDefault(event.getClass(), List.of());
        for (Subscription<T> sub : new ArrayList<>(list)) {
            if (!sub.cancelled) {
                try {
                    sub.handler.accept(event);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    /* ---------- intern ---------- */

    private static final class Subscription<T> implements EventSubscription<T> {
        private final Class<T> eventType;
        private final EventPriority priority;
        private final Consumer<T> handler;
        private volatile boolean cancelled = false;

        Subscription(Class<T> eventType, EventPriority priority, Consumer<T> handler) {
            this.eventType = eventType;
            this.priority = priority;
            this.handler = handler;
        }

        @Override
        public void unsubscribe() {
            this.cancelled = true;
        }

        @Override
        public boolean isActive() {
            return !cancelled;
        }
    }
}