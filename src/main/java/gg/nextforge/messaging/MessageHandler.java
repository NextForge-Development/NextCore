package gg.nextforge.messaging;

/**
 * Functional interface for handling typed message payloads.
 *
 * @param <T> The type of payload this handler supports.
 */
@FunctionalInterface
public interface MessageHandler<T> {
    void handle(T payload);
}
