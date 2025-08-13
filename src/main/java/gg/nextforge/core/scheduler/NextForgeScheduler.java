package gg.nextforge.core.scheduler;



import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class NextForgeScheduler implements Scheduler {

    private static final long TICK_MILLIS = 50L;

    private final ScheduledThreadPoolExecutor async;         // async timers & tasks
    private final PriorityQueue<SyncTask> syncQueue;         // due by nextRunMillis
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile Thread mainThread;                      // bound main thread
    private final int maxSyncPerTick;                        // backpressure limit per tick

    public NextForgeScheduler(int asyncThreads, int maxSyncPerTick) {
        this.async = new ScheduledThreadPoolExecutor(Math.max(1, asyncThreads), r -> {
            Thread t = new Thread(r, "NextForge-AsyncScheduler");
            t.setDaemon(true);
            return t;
        });
        this.async.setRemoveOnCancelPolicy(true);
        this.syncQueue = new PriorityQueue<>(Comparator.comparingLong(st -> st.nextRunMillis));
        this.maxSyncPerTick = Math.max(1, maxSyncPerTick);
    }

    @Override
    public void bindMainThread() {
        this.mainThread = Thread.currentThread();
    }

    @Override
    public boolean isMainThread() {
        return Thread.currentThread() == mainThread;
    }

    @Override
    public TaskHandle runTask(Runnable task, boolean sync) {
        if (sync) {
            return enqueueSync(task, 0L, 0L);
        } else {
            var f = async.submit(wrap(task));
            return new AsyncHandle(f);
        }
    }

    @Override
    public TaskHandle runTaskLater(Runnable task, long delayTicks, boolean sync) {
        long delayMs = ticksToMillis(delayTicks);
        if (sync) {
            return enqueueSync(task, System.currentTimeMillis() + delayMs, 0L);
        } else {
            var f = async.schedule(wrap(task), Math.max(0L, delayMs), TimeUnit.MILLISECONDS);
            return new AsyncHandle(f);
        }
    }

    @Override
    public TaskHandle runTaskTimer(Runnable task, long delayTicks, long periodTicks, boolean sync) {
        long delayMs = ticksToMillis(delayTicks);
        long periodMs = ticksToMillis(periodTicks);
        if (sync) {
            return enqueueSync(task, System.currentTimeMillis() + delayMs, periodMs);
        } else {
            var f = async.scheduleAtFixedRate(wrap(task),
                    Math.max(0L, delayMs),
                    Math.max(TICK_MILLIS, periodMs),
                    TimeUnit.MILLISECONDS);
            return new AsyncHandle(f);
        }
    }

    @Override
    public void tick() {
        if (closed.get()) return;
        assertMain();

        long now = System.currentTimeMillis();
        int executed = 0;

        while (!syncQueue.isEmpty() && executed < maxSyncPerTick) {
            SyncTask st = syncQueue.peek();
            if (st.cancelled.get()) { syncQueue.poll(); continue; }
            if (st.nextRunMillis > now) break;

            syncQueue.poll(); // pop
            try { st.task.run(); }
            catch (Throwable t) { t.printStackTrace(); }

            executed++;

            if (!st.cancelled.get() && st.periodMillis > 0L) {
                st.nextRunMillis = now + st.periodMillis;
                syncQueue.offer(st);
            }
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        // cancel all sync tasks
        for (SyncTask st : new ArrayList<>(syncQueue)) st.cancelled.set(true);
        syncQueue.clear();
        // shutdown async executor
        async.shutdownNow();
    }

    /* ---------------- internal ---------------- */

    private void assertMain() {
        if (mainThread == null)
            throw new IllegalStateException("Main thread not bound. Call bindMainThread() on the server thread.");
        if (!isMainThread())
            throw new IllegalStateException("tick() must be called on the bound main thread.");
    }

    private static long ticksToMillis(long ticks) {
        return Math.max(0L, ticks) * TICK_MILLIS;
    }

    private Runnable wrap(Runnable r) {
        return () -> {
            if (closed.get()) return;
            try { r.run(); } catch (Throwable t) { t.printStackTrace(); }
        };
    }

    private TaskHandle enqueueSync(Runnable task, long firstRunMillis, long periodMs) {
        SyncTask st = new SyncTask(task, firstRunMillis <= 0 ? System.currentTimeMillis() : firstRunMillis, periodMs);
        synchronized (syncQueue) { syncQueue.offer(st); }
        return st;
    }

    /* ---------------- models ---------------- */

    private static final class SyncTask implements TaskHandle {
        final Runnable task;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        long nextRunMillis;
        final long periodMillis;

        SyncTask(Runnable task, long nextRunMillis, long periodMillis) {
            this.task = Objects.requireNonNull(task, "task");
            this.nextRunMillis = nextRunMillis;
            this.periodMillis = Math.max(0L, periodMillis);
        }

        @Override public void cancel() { cancelled.set(true); }
        @Override public boolean isCancelled() { return cancelled.get(); }
    }

    private static final class AsyncHandle implements TaskHandle {
        private final Future<?> f;
        AsyncHandle(Future<?> f) { this.f = f; }
        @Override public void cancel() { f.cancel(true); }
        @Override public boolean isCancelled() { return f.isCancelled(); }
    }
}
