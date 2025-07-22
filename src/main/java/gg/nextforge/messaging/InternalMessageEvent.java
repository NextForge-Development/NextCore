package gg.nextforge.messaging;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * A Bukkit-compatible event for internal messaging.
 * This enables forwarding messages into the Bukkit event system.
 */
public class InternalMessageEvent<T> extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final MessageEnvelope<T> envelope;

    public InternalMessageEvent(MessageEnvelope<T> envelope) {
        this.envelope = envelope;
    }

    public MessageEnvelope<T> getEnvelope() {
        return envelope;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
