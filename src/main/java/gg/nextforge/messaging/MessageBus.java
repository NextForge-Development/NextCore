package gg.nextforge.messaging;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central dispatcher for publishing and subscribing to messages.
 */
public class MessageBus {

    private final Map<Class<?>, List<MessageHandler<?>>> handlerMap = new ConcurrentHashMap<>();

    public <T> MessageSubscription subscribe(Class<T> type, MessageHandler<T> handler) {
        handlerMap.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
        return new MessageSubscription(() -> unsubscribe(type, handler));
    }

    @SuppressWarnings("unchecked")
    public <T> void publish(T payload) {
        Class<T> type = (Class<T>) payload.getClass();
        List<MessageHandler<?>> handlers = handlerMap.get(type);
        if (handlers != null) {
            for (MessageHandler<?> handler : new ArrayList<>(handlers)) {
                ((MessageHandler<T>) handler).handle(payload);
            }
        }
    }

    private <T> void unsubscribe(Class<T> type, MessageHandler<T> handler) {
        List<MessageHandler<?>> handlers = handlerMap.get(type);
        if (handlers != null) {
            handlers.remove(handler);
            if (handlers.isEmpty()) {
                handlerMap.remove(type);
            }
        }
    }
}
