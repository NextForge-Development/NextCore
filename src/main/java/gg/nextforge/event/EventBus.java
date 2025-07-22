package gg.nextforge.event;

import gg.nextforge.scheduler.CoreScheduler;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * EventBus provides a simplified way to register and handle Bukkit events using lambdas.
 * <p>
 * This class eliminates the need for creating multiple listener classes and allows
 * developers to register event handlers directly with concise syntax.
 */
public class EventBus {

    private static EventBus instance; // Singleton instance of the EventBus
    private final Plugin plugin; // The plugin instance associated with this EventBus
    private final Map<Class<? extends Event>, List<RegisteredHandler>> handlers = new ConcurrentHashMap<>(); // Map of event classes to their handlers

    /**
     * Constructs an EventBus instance and sets it as the singleton.
     *
     * @param plugin The plugin instance associated with this EventBus.
     */
    public EventBus(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    /**
     * Registers a simple event handler with default priority.
     *
     * @param eventClass The class of the event to listen for.
     * @param handler    The lambda function to handle the event.
     * @param <T>        The type of the event.
     * @return An EventSubscription object for managing the handler.
     */
    public static <T extends Event> EventSubscription on(Class<T> eventClass, Consumer<T> handler) {
        return instance.subscribe(eventClass, handler, EventPriority.NORMAL, false);
    }

    /**
     * Registers an event handler with a custom priority.
     *
     * @param eventClass The class of the event to listen for.
     * @param handler    The lambda function to handle the event.
     * @param priority   The priority of the event handler.
     * @param <T>        The type of the event.
     * @return An EventSubscription object for managing the handler.
     */
    public static <T extends Event> EventSubscription on(Class<T> eventClass, Consumer<T> handler, EventPriority priority) {
        return instance.subscribe(eventClass, handler, priority, false);
    }

    /**
     * Registers an asynchronous event handler. <br><br>
     * <b>Note:</b> This will run the handler on a separate thread.
     * Be cautious with thread safety when accessing shared resources.
     * Do not use Bukkit API calls directly in async handlers.
     *
     * @param eventClass The class of the event to listen for.
     * @param handler    The lambda function to handle the event.
     * @param <T>        The type of the event.
     * @return An EventSubscription object for managing the handler.
     */
    public static <T extends Event> EventSubscription onAsync(Class<T> eventClass, Consumer<T> handler) {
        return instance.subscribe(eventClass, handler, EventPriority.NORMAL, true);
    }

    /**
     * Registers an event handler with a filter.
     * The handler is only triggered if the predicate returns true.
     *
     * @param eventClass The class of the event to listen for.
     * @param handler    The lambda function to handle the event.
     * @param filter     The predicate to filter events.
     * @param <T>        The type of the event.
     * @return An EventSubscription object for managing the handler.
     */
    public static <T extends Event> EventSubscription on(Class<T> eventClass, Consumer<T> handler, Predicate<T> filter) {
        return instance.subscribe(eventClass, e -> {
            if (filter.test(e)) {
                handler.accept(e);
            }
        }, EventPriority.NORMAL, false);
    }

    /**
     * Registers an event handler that will be executed a specified number of times.
     * After the maximum number of executions, the handler will automatically unregister itself.
     *
     * @param eventClass      The class of the event to listen for.
     * @param handler         The lambda function to handle the event.
     * @param maxExecutions   The maximum number of times the handler should be executed.
     * @param <T>             The type of the event.
     * @return An EventSubscription object for managing the handler.
     */
    public static <T extends Event> EventSubscription onTimes(Class<T> eventClass, Consumer<T> handler, int maxExecutions) {
        AtomicInteger counter = new AtomicInteger();
        EventSubscription[] sub = new EventSubscription[1]; // dirty, but functional
        sub[0] = on(eventClass, e -> {
            handler.accept(e);
            if (counter.incrementAndGet() >= maxExecutions) {
                sub[0].unregister();
            }
        });
        return sub[0];
    }

    /**
     * Registers an event handler that will be executed after a specified timeout.
     * The handler will automatically unregister itself after the timeout.
     *
     * @param eventClass The class of the event to listen for.
     * @param handler    The lambda function to handle the event.
     * @param duration   The duration to wait before executing the handler.
     * @param unit       The time unit of the duration.
     * @param <T>        The type of the event.
     * @return An EventSubscription object for managing the handler.
     */
    public static <T extends Event> EventSubscription onTimeout(Class<T> eventClass, Consumer<T> handler, long duration, TimeUnit unit) {
        EventSubscription subscription = on(eventClass, handler);
        CoreScheduler.runLater(subscription::unregister, unit.toMillis(duration) / 50); // Convert ms to ticks
        return subscription;
    }

    /**
     * Registers an event handler that will be executed a specified number of times
     * within a timeout period. After the maximum number of executions or the timeout,
     * the handler will automatically unregister itself.
     *
     * @param eventClass      The class of the event to listen for.
     * @param handler         The lambda function to handle the event.
     * @param maxExecutions   The maximum number of times the handler should be executed.
     * @param timeout         The timeout duration after which the handler will be unregistered.
     * @param unit            The time unit of the timeout duration.
     * @param <T>             The type of the event.
     * @return An EventSubscription object for managing the handler.
     */
    public static <T extends Event> EventSubscription onLimit(Class<T> eventClass, Consumer<T> handler, int maxExecutions, long timeout, TimeUnit unit) {
        AtomicInteger counter = new AtomicInteger();
        EventSubscription[] sub = new EventSubscription[1];
        sub[0] = on(eventClass, e -> {
            handler.accept(e);
            if (counter.incrementAndGet() >= maxExecutions) {
                sub[0].unregister();
            }
        });
        CoreScheduler.runLater(() -> {
            if (!sub[0].isCancelled()) sub[0].unregister();
        }, unit.toMillis(timeout) / 50);
        return sub[0];
    }

    /**
     * Fires a custom event.
     *
     * @param event The event to fire.
     */
    public static void fire(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Subscribes to an event with the specified parameters.
     *
     * @param eventClass The class of the event to listen for.
     * @param handler    The lambda function to handle the event.
     * @param priority   The priority of the event handler.
     * @param async      Whether the handler should run asynchronously.
     * @param <T>        The type of the event.
     * @return An EventSubscription object for managing the handler.
     */
    @SuppressWarnings("unchecked")
    private <T extends Event> EventSubscription subscribe(
            Class<T> eventClass,
            Consumer<T> handler,
            EventPriority priority,
            boolean async) {

        // Create a wrapper for the handler
        RegisteredHandler registered = new RegisteredHandler(eventClass, handler, priority, async);

        // Add the handler to the map
        handlers.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(registered);

        // Create a Bukkit listener
        Listener listener = new Listener() {
        };

        EventExecutor executor = (l, event) -> {
            if (eventClass.isInstance(event)) {
                if (async) {
                    // Run async handlers on a separate thread
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        handler.accept((T) event);
                    });
                } else {
                    handler.accept((T) event);
                }
            }
        };

        // Register the event with Bukkit
        Bukkit.getPluginManager().registerEvent(
                eventClass, listener, priority, executor, plugin, false
        );

        // Return the subscription object
        return new EventSubscription(eventClass, registered, listener);
    }

    /**
     * Retrieves the number of registered listeners.
     *
     * @return The total number of registered listeners.
     */
    public int getListenerCount() {
        return handlers.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Fires a custom CoreLib event.
     *
     * @param event The CoreEvent to fire.
     */
    public void fireEvent(CoreEvent event) {
        // Placeholder for firing CoreLib events
    }

    /**
     * Registers a handler for CoreLib events.
     *
     * @param listener   The listener object.
     * @param eventClass The class of the CoreEvent to listen for.
     * @param priority   The priority of the event handler.
     * @param method     The method to invoke for the event.
     */
    public void register(Object listener, Class<? extends CoreEvent> eventClass,
                         EventPriority priority, java.lang.reflect.Method method) {
        // Placeholder for registering CoreLib event handlers
    }

    /**
     * Unregisters all handlers for a listener.
     *
     * @param listener The listener object to unregister.
     */
    public void unregister(Object listener) {
        // Placeholder for unregistering CoreLib event handlers
    }

    /**
     * Represents a registered event handler.
     */
    private record RegisteredHandler(Class<? extends Event> eventClass, Consumer<? extends Event> handler,
                                     EventPriority priority, boolean async) {
    }

    /**
     * Represents a subscription to an event.
     * Allows unregistering the handler when no longer needed.
     */
    public class EventSubscription {
        private final Class<? extends Event> eventClass;
        private final RegisteredHandler handler;
        private final Listener listener;
        @Getter
        private boolean cancelled = false;

        EventSubscription(Class<? extends Event> eventClass, RegisteredHandler handler, Listener listener) {
            this.eventClass = eventClass;
            this.handler = handler;
            this.listener = listener;
        }

        /**
         * Unregisters the event handler.
         */
        public void unregister() {
            if (!cancelled) {
                HandlerList.unregisterAll(listener);
                List<RegisteredHandler> classHandlers = handlers.get(eventClass);
                if (classHandlers != null) {
                    classHandlers.remove(handler);
                }
                cancelled = true;
            }
        }
    }
}