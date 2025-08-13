package gg.nextforge.core.scheduler;

public interface Scheduler extends AutoCloseable {
    TaskHandle runTask(Runnable task, boolean sync);
    TaskHandle runTaskLater(Runnable task, long delayTicks, boolean sync);
    TaskHandle runTaskTimer(Runnable task, long delayTicks, long periodTicks, boolean sync);

    /** Must be called from the server main thread once per tick. */
    void tick();

    /** Bind the current thread as the main thread (call in onEnable on main). */
    void bindMainThread();

    /** Optional guard to assert we're on the bound main thread. */
    boolean isMainThread();

    @Override void close();
}
