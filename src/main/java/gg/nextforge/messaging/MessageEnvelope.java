package gg.nextforge.messaging;

import java.util.UUID;

/**
 * A message wrapper that includes metadata and a payload.
 */
public class MessageEnvelope<T> {

    private final UUID messageId;
    private final long timestamp;
    private final Class<T> type;
    private final T payload;

    public MessageEnvelope(Class<T> type, T payload) {
        this.messageId = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.payload = payload;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Class<T> getType() {
        return type;
    }

    public T getPayload() {
        return payload;
    }
}
