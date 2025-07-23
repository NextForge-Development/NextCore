package gg.nextforge.messaging;

/**
 * A handle to cancel a message subscription.
 */
public class MessageSubscription {

    private final Runnable cancelAction;
    private boolean active = true;

    public MessageSubscription(Runnable cancelAction) {
        this.cancelAction = cancelAction;
    }

    public void unsubscribe() {
        if (active) {
            cancelAction.run();
            active = false;
        }
    }

    public boolean isActive() {
        return active;
    }
}
