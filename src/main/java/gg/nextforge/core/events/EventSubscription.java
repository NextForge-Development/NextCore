package gg.nextforge.core.events;

public interface EventSubscription<T> {
    void unsubscribe();
    boolean isActive();
}