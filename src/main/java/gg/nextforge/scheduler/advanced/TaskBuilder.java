package gg.nextforge.scheduler.advanced;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TaskBuilder<T> {

    private boolean async = true;
    private boolean repeating = false;

    private long delay = 0;
    private long period = 0;

    private Supplier<T> supplier;
    private Consumer<T> callback;
    private Consumer<Throwable> errorHandler;

    private AdvancedTaskScheduler scheduler;

    private TaskBuilder(AdvancedTaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public static <T> TaskBuilder<T> create(AdvancedTaskScheduler scheduler) {
        return new TaskBuilder<>(scheduler);
    }

    public TaskBuilder<T> async(boolean async) {
        this.async = async;
        return this;
    }

    public TaskBuilder<T> delay(long millis) {
        this.delay = millis;
        return this;
    }

    public TaskBuilder<T> period(long millis) {
        this.repeating = true;
        this.period = millis;
        return this;
    }

    public TaskBuilder<T> supplier(Supplier<T> supplier) {
        this.supplier = supplier;
        return this;
    }

    public TaskBuilder<T> onResult(Consumer<T> callback) {
        this.callback = callback;
        return this;
    }

    public TaskBuilder<T> onError(Consumer<Throwable> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public Future<?> schedule() {
        if (supplier == null) throw new IllegalStateException("No task supplier defined!");

        Callable<T> callable = () -> {
            try {
                return supplier.get();
            } catch (Throwable t) {
                if (errorHandler != null) errorHandler.accept(t);
                else t.printStackTrace();
                return null;
            }
        };

        Runnable task = () -> {
            try {
                T result = callable.call();
                if (callback != null && result != null) callback.accept(result);
            } catch (Exception e) {
                if (errorHandler != null) errorHandler.accept(e);
                else e.printStackTrace();
            }
        };

        if (repeating) {
            return scheduler.runRepeating(task, delay, period);
        } else if (delay > 0) {
            return scheduler.runLater(task, delay);
        } else if (async) {
            return scheduler.runAsync(task);
        } else {
            task.run();
            return null;
        }
    }
}
