package gg.nextforge.messaging.queue;

import gg.nextforge.messaging.MessageHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueuedMessageDispatcher<T> {

    private final MessageQueue<T> queue = new MessageQueue<>();
    private final MessageHandler<T> handler;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    public QueuedMessageDispatcher(MessageHandler<T> handler) {
        this.handler = handler;
        start();
    }

    private void start() {
        executor.submit(() -> {
            while (running) {
                try {
                    T message = queue.poll();
                    if (message != null) {
                        handler.handle(message);
                    }
                    Thread.sleep(5); // fine-tune for your environment
                } catch (Exception ex) {
                    ex.printStackTrace(); // TODO: Logging
                }
            }
        });
    }

    public void dispatchLater(T message) {
        queue.enqueue(message);
    }

    public void stop() {
        running = false;
        executor.shutdownNow();
    }
}
