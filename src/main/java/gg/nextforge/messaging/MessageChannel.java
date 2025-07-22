package gg.nextforge.messaging;

import java.util.function.Consumer;

/**
 * A named logical channel for message segregation.
 */
public class MessageChannel<T> {

    private final String name;
    private final Class<T> type;
    private final MessageBus messageBus;

    public MessageChannel(String name, Class<T> type, MessageBus messageBus) {
        this.name = name;
        this.type = type;
        this.messageBus = messageBus;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    public void publish(T message) {
        messageBus.publish(message);
    }

    public MessageSubscription subscribe(Consumer<T> handler) {
        return messageBus.subscribe(type, handler::accept);
    }
}