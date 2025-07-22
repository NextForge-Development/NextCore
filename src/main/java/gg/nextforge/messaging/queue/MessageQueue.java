package gg.nextforge.messaging.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageQueue<T> {

    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>();

    public void enqueue(T message) {
        queue.add(message);
    }

    public T poll() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
    }
}
