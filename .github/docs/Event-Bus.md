# 📨 NextForge EventBus – Design & Usage

A lightweight, **type-safe** event system for your plugins.  
Subscribe with **priority** and a typed `Consumer<T>`, get back an `EventSubscription` you can **unsubscribe** later.

---

## Key Features

- **Type-safe**: subscribe per event class (no raw casting in listeners)
- **Priorities** (`LOWEST … MONITOR`) with deterministic order
- **OOP API**: `EventBus`, `EventSubscription`, `EventPriority`
- **Unsubscribe** via handle or bus
- **Thread-safe** dispatch (copy-on-dispatch)
- Small and test-friendly

---

## Architecture

```
+------------+        subscribe(handler)        +-----------+
|  Plugin    | --------------------------------> | EventBus  |
+------------+                                   +-----------+
       ^                                              |
       | post(event)                                  | calls (by priority)
       +----------------------------------------------+
```

The bus stores subscriptions per **event class** and dispatches them in **priority order**.

---

## API

```java
// gg/nextforge/core/util/events/EventPriority.java
public enum EventPriority {
    LOWEST(0), LOW(1), NORMAL(2), HIGH(3), HIGHEST(4), MONITOR(5);
    private final int value;
    EventPriority(int v) { this.value = v; }
    public int getValue() { return value; }
}

// gg/nextforge/core/util/events/EventSubscription.java
public interface EventSubscription<T> {
    void unsubscribe();
    boolean isActive();
}

// gg/nextforge/core/util/events/EventBus.java
public class EventBus {
    public <T> EventSubscription<T> subscribe(Class<T> eventType, EventPriority priority, java.util.function.Consumer<T> handler);
    public <T> void unsubscribe(EventSubscription<T> subscription);
    public <T> void post(T event);
}
```
> Implementation: backing store `Map<Class<?>, List<Subscription<?>>>`, sorted by priority (desc). Dispatch iterates over a **copy** to avoid CME.

---

## Usage

### Define an event
```java
public record PlayerJoinEvent(String playerName) {}
```

### Subscribe
```java
EventBus bus = new EventBus();

var sub = bus.subscribe(
    PlayerJoinEvent.class,
    EventPriority.NORMAL,
    e -> System.out.println("Welcome, " + e.playerName())
);
```

### Post an event
```java
bus.post(new PlayerJoinEvent("Steve"));
```

### Unsubscribe
```java
sub.unsubscribe();             // stops this one
bus.unsubscribe(sub);          // or via bus
```

---

## Threading

- Subscriptions are stored in a concurrent map; dispatch copies current listeners to a local list to avoid modification during iteration.
- The bus itself is **not** an async executor — if you want async handlers, schedule them via your **Scheduler**.

Example:
```java
bus.subscribe(MyEvent.class, EventPriority.HIGH, e -> {
    scheduler.runTask(() -> handleOnMain(e), true); // switch to main thread
});
```

---

## Testing

- Pure Java: no server dependencies.
- Deterministic ordering via `EventPriority` values.
- Easy to verify with unit tests by posting events and asserting side effects.

---

## Optional: Cancelable Events (extension idea)

If you need cancelation, add an interface:
```java
public interface Cancelable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}
```
Handlers can set `event.setCancelled(true)`.  
Your `post` can then stop propagation for priorities `< MONITOR` if cancelled.

---

## Reference Implementation (summary)

```java
public class EventBus {
    private final java.util.Map<Class<?>, java.util.List<Subscription<?>>> subscribers = new java.util.concurrent.ConcurrentHashMap<>();

    public <T> EventSubscription<T> subscribe(Class<T> eventType, EventPriority priority, java.util.function.Consumer<T> handler) {
        var sub = new Subscription<>(eventType, priority, handler);
        subscribers.computeIfAbsent(eventType, k -> new java.util.ArrayList<>()).add(sub);
        subscribers.get(eventType).sort(java.util.Comparator.comparingInt((Subscription<?> s) -> s.priority.getValue()).reversed());
        return sub;
    }

    public <T> void unsubscribe(EventSubscription<T> subscription) {
        if (subscription instanceof Subscription<?> sub) {
            var list = subscribers.get(sub.eventType);
            if (list != null) list.remove(sub);
        }
    }

    public <T> void post(T event) {
        @SuppressWarnings("unchecked")
        var list = (java.util.List<Subscription<T>>) (java.util.List<?>) subscribers.getOrDefault(event.getClass(), java.util.List.of());
        for (var sub : new java.util.ArrayList<>(list)) {
            if (!sub.cancelled) {
                try { sub.handler.accept(event); } catch (Throwable t) { t.printStackTrace(); }
            }
        }
    }

    private static final class Subscription<T> implements EventSubscription<T> {
        private final Class<T> eventType;
        private final EventPriority priority;
        private final java.util.function.Consumer<T> handler;
        private volatile boolean cancelled = false;
        Subscription(Class<T> eventType, EventPriority priority, java.util.function.Consumer<T> handler) {
            this.eventType = eventType; this.priority = priority; this.handler = handler;
        }
        public void unsubscribe() { cancelled = true; }
        public boolean isActive() { return !cancelled; }
    }
}
```

---

Happy eventing! Combine this with the **Scheduler** for safe main-thread execution.
