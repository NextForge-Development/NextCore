package gg.nextforge.scheduler;

import gg.nextforge.scheduler.advanced.AdvancedTaskScheduler;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A standalone, thread-safe scheduler implementation.
 * No Bukkit dependencies, suitable for general Java use.
 */
public class CoreScheduler {

    private static CoreScheduler instance;
    @Getter
    private static AdvancedTaskScheduler advancedScheduler;

    @Getter
    private static final Map<Integer, ScheduledTask> activeTasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executorService;
    private final AtomicInteger taskCounter = new AtomicInteger(0);

    public CoreScheduler() {
        this.executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        this.advancedScheduler = new AdvancedTaskScheduler();
        instance = this;
    }

    public static ScheduledTask run(Runnable task) {
        return instance.schedule(task, 0, false, false);
    }

    public static ScheduledTask runLater(Runnable task, long delayTicks) {
        return instance.schedule(task, delayTicks, false, false);
    }

    public static ScheduledTask runTimer(Runnable task, long delayTicks, long periodTicks) {
        return instance.scheduleRepeating(task, delayTicks, periodTicks, false);
    }

    public static ScheduledTask runAsync(Runnable task) {
        return instance.schedule(task, 0, true, false);
    }

    public static ScheduledTask runAsyncLater(Runnable task, long delayTicks) {
        return instance.schedule(task, delayTicks, true, false);
    }

    public static ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return instance.scheduleRepeating(task, delayTicks, periodTicks, true);
    }

    public void shutdown() {
        activeTasks.values().forEach(ScheduledTask::cancel);
        activeTasks.clear();
        executorService.shutdownNow();
    }

    private ScheduledTask schedule(Runnable task, long delayTicks, boolean async, boolean repeating) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduledTask = new ScheduledTask(id);
        long delayMillis = ticksToMillis(delayTicks);

        ScheduledFuture<?> future = executorService.schedule(() -> {
            try {
                task.run();
            } finally {
                if (!repeating) {
                    activeTasks.remove(id);
                }
            }
        }, delayMillis, TimeUnit.MILLISECONDS);

        scheduledTask.setFuture(future);
        activeTasks.put(id, scheduledTask);
        return scheduledTask;
    }

    private ScheduledTask scheduleRepeating(Runnable task, long delayTicks, long periodTicks, boolean async) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduledTask = new ScheduledTask(id);
        long delayMillis = ticksToMillis(delayTicks);
        long periodMillis = ticksToMillis(periodTicks);

        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(task, delayMillis, periodMillis, TimeUnit.MILLISECONDS);

        scheduledTask.setFuture(future);
        activeTasks.put(id, scheduledTask);
        return scheduledTask;
    }

    private long ticksToMillis(long ticks) {
        return ticks * 50L; // 20 ticks = 1 second
    }

}
