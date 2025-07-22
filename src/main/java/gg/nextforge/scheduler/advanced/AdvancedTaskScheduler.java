package gg.nextforge.scheduler.advanced;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class AdvancedTaskScheduler {

    private static final Logger LOGGER = Logger.getLogger("AdvancedTaskScheduler");

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool();

    public ScheduledFuture<?> runLater(Runnable task, long delayMillis) {
        return executor.schedule(safe(task), delayMillis, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> runRepeating(Runnable task, long initialDelay, long period) {
        return executor.scheduleAtFixedRate(safe(task), initialDelay, period, TimeUnit.MILLISECONDS);
    }

    public Future<?> runAsync(Runnable task) {
        return asyncExecutor.submit(safe(task));
    }

    public <T> Future<T> supplyAsync(Callable<T> task) {
        return asyncExecutor.submit(task);
    }

    public <T> void supplyAsync(Callable<T> task, Consumer<T> resultHandler, Consumer<Throwable> errorHandler) {
        asyncExecutor.submit(() -> {
            try {
                T result = task.call();
                resultHandler.accept(result);
            } catch (Throwable t) {
                errorHandler.accept(t);
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
        asyncExecutor.shutdownNow();
    }

    private Runnable safe(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                LOGGER.severe("Task execution failed: " + t.getMessage());
                t.printStackTrace();
            }
        };
    }
}