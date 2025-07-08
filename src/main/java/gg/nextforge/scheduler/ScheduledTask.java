package gg.nextforge.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Represents a scheduled task that can be cancelled.
 */
public class ScheduledTask {
    private final int id; // Unique ID for the task
    private BukkitTask bukkitTask; // Bukkit task instance
    private volatile boolean cancelled = false; // Flag indicating if the task is cancelled

    /**
     * Constructs a ScheduledTask instance.
     *
     * @param id The unique ID for the task.
     */
    ScheduledTask(int id) {
        this.id = id;
    }

    /**
     * Sets the BukkitTask instance for this ScheduledTask.
     *
     * @param task The BukkitTask instance.
     */
    void setBukkitTask(BukkitTask task) {
        this.bukkitTask = task;
    }

    /**
     * Cancels this task.
     * Can be called multiple times safely.
     */
    public void cancel() {
        if (!cancelled && bukkitTask != null) {
            bukkitTask.cancel();
            cancelled = true;
            CoreScheduler.getActiveTasks().remove(id);
        }
    }

    /**
     * Checks if this task is still active.
     *
     * @return True if the task is running, false otherwise.
     */
    public boolean isActive() {
        return !cancelled && bukkitTask != null &&
                Bukkit.getScheduler().isCurrentlyRunning(bukkitTask.getTaskId());
    }

    /**
     * Retrieves the task ID of this ScheduledTask.
     *
     * @return The task ID, or -1 if the task is not active.
     */
    public int getTaskId() {
        return bukkitTask != null ? bukkitTask.getTaskId() : -1;
    }
}
