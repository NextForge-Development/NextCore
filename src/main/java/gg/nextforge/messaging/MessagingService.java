package gg.nextforge.messaging;

import gg.nextforge.messaging.queue.MessageQueue;
import gg.nextforge.messaging.queue.QueuedMessageDispatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entry point for plugin-wide message communication.
 */
public class MessagingService {

    private final MessageBus messageBus;
    private final Map<String, MessageChannel<?>> channels = new HashMap<>();
    private final Map<Class<?>, QueuedMessageDispatcher<?>> asyncDispatchers = new ConcurrentHashMap<>();

    public MessagingService() {
        this.messageBus = new MessageBus();
    }

    public MessageBus getBus() {
        return messageBus;
    }

    public <T> MessageChannel<T> createChannel(String name, Class<T> type) {
        MessageChannel<T> channel = new MessageChannel<>(name, type, messageBus);
        channels.put(name, channel);
        return channel;
    }

    @SuppressWarnings("unchecked")
    public <T> MessageChannel<T> getChannel(String name, Class<T> type) {
        return (MessageChannel<T>) channels.get(name);
    }

    public boolean hasChannel(String name) {
        return channels.containsKey(name);
    }

    public void clearChannels() {
        channels.clear();
    }

    public <T> void registerAsyncHandler(Class<T> type, MessageHandler<T> handler) {
        asyncDispatchers.put(type, new QueuedMessageDispatcher<>(handler));
    }

    @SuppressWarnings("unchecked")
    public <T> void publishAsync(T message) {
        QueuedMessageDispatcher<T> dispatcher = (QueuedMessageDispatcher<T>) asyncDispatchers.get(message.getClass());
        if (dispatcher != null) {
            dispatcher.dispatchLater(message);
        } else {
            throw new IllegalStateException("No async dispatcher registered for type: " + message.getClass());
        }
    }

    public void shutdownAsync() {
        asyncDispatchers.values().forEach(QueuedMessageDispatcher::stop);
        asyncDispatchers.clear();
    }
}
