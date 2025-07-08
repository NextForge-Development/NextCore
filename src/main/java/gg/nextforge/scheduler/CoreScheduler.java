package gg.nextforge.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper around Bukkit's scheduler to simplify task management.
 * <p>
 * Features:
 * - Static access for convenience (e.g., CoreScheduler.runLater())
 * - Automatic task cleanup
 * - Cancel tokens for better task control
 * - Avoids manual tracking of task IDs
 */
public class CoreScheduler {

    private static CoreScheduler instance; // Singleton instance of CoreScheduler
    private final Plugin plugin; // Plugin instance for scheduling tasks
    private final Map<Integer, ScheduledTask> activeTasks = new ConcurrentHashMap<>(); // Active tasks map
    private final AtomicInteger taskCounter = new AtomicInteger(0); // Counter for task IDs

    /**
     * Constructs a CoreScheduler instance.
     *
     * @param plugin The plugin instance.
     */
    public CoreScheduler(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    /**
     * Runs a task on the next tick.
     *
     * @param task The task to run.
     * @return A ScheduledTask representing the scheduled task.
     */
    public static ScheduledTask run(Runnable task) {
        return instance.runTask(task);
    }

    // Static convenience methods for scheduling tasks

    /**
     * Runs a task after a specified delay.
     *
     * @param task  The task to run.
     * @param delay The delay in ticks (20 ticks = 1 second).
     * @return A ScheduledTask representing the scheduled task.
     */
    public static ScheduledTask runLater(Runnable task, long delay) {
        return instance.runTaskLater(task, delay);
    }

    /**
     * Runs a task repeatedly with a specified delay and interval.
     *
     * @param task   The task to run.
     * @param delay  The initial delay in ticks.
     * @param period The interval between executions in ticks.
     * @return A ScheduledTask representing the scheduled task.
     */
    public static ScheduledTask runTimer(Runnable task, long delay, long period) {
        return instance.runTaskTimer(task, delay, period);
    }

    /**
     * Runs a task asynchronously.
     *
     * @param task The task to run.
     * @return A ScheduledTask representing the scheduled task.
     */
    public static ScheduledTask runAsync(Runnable task) {
        return instance.runTaskAsync(task);
    }

    /**
     * Runs a task asynchronously after a specified delay.
     *
     * @param task  The task to run.
     * @param delay The delay in ticks.
     * @return A ScheduledTask representing the scheduled task.
     */
    public static ScheduledTask runAsyncLater(Runnable task, long delay) {
        return instance.runTaskAsyncLater(task, delay);
    }

    /**
     * Runs a task asynchronously and repeatedly.
     *
     * @param task   The task to run.
     * @param delay  The initial delay in ticks.
     * @param period The interval between executions in ticks.
     * @return A ScheduledTask representing the scheduled task.
     */
    public static ScheduledTask runAsyncTimer(Runnable task, long delay, long period) {
        return instance.runTaskAsyncTimer(task, delay, period);
    }

    /**
     * Cleans up all active tasks.
     * Should be called when the plugin is disabled.
     */
    public void shutdown() {
        activeTasks.values().forEach(ScheduledTask::cancel);
        activeTasks.clear();
    }

    // Instance methods for scheduling tasks

    private ScheduledTask runTask(Runnable task) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);

        BukkitTask bukkitTask = Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                task.run();
            } finally {
                activeTasks.remove(id);
            }
        });

        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }

    private ScheduledTask runTaskLater(Runnable task, long delay) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);

        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                task.run();
            } finally {
                activeTasks.remove(id);
            }
        }, delay);

        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }

    private ScheduledTask runTaskTimer(Runnable task, long delay, long period) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);

        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);

        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }

    private ScheduledTask runTaskAsync(Runnable task) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);

        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                task.run();
            } finally {
                activeTasks.remove(id);
            }
        });

        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }

    private ScheduledTask runTaskAsyncLater(Runnable task, long delay) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);

        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                task.run();
            } finally {
                activeTasks.remove(id);
            }
        }, delay);

        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }

    private ScheduledTask runTaskAsyncTimer(Runnable task, long delay, long period) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);

        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, task, delay, period
        );

        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }

    /**
     * Runs a task on the main thread.
     * Useful for accessing Bukkit API from asynchronous tasks.
     *
     * @param task The task to run.
     */
    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}