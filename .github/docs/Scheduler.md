# ⏱️ NextForge Scheduler – Design & Usage

A lightweight, **object-oriented** task scheduler independent of Bukkit/Paper’s `BukkitScheduler`.  
Supports **sync** (main-thread) and **async** execution with `runTask`, `runTaskLater`, and `runTaskTimer`.  
Sync work is executed via a **main-thread pump** you call once per server tick.

> Why a custom scheduler? Control, testability, isolation from server APIs, and predictable backpressure behavior.

---

## Key Features

- **Sync vs Async** execution
- **Immediate / delayed / repeating** tasks
- **Backpressure**: cap the number of sync tasks executed per tick
- **No BukkitScheduler** required
- **OOP-focused API** with `Scheduler` and `TaskHandle`
- **Graceful shutdown**

---

## Architecture Overview

```
+---------------------------+     tick() per server tick      +-------------------+
|  Your Plugin Main Thread  |  ------------------------------> |  DefaultScheduler |
|        (server main)      |                                  |   (sync queue)    |
+---------------------------+                                  +-------------------+
                 ^                                                         |
                 |                                                         | schedules
                 |                                                         v
           runTask(..., true)                                      +-------------------+
                                                                   |  Async Executor   |
                                                                   | (ScheduledThread) |
                                                                   +-------------------+
```

- **Sync tasks** are enqueued with a `nextRunMillis` and executed inside `Scheduler#tick()` (called on main thread).
- **Async tasks** are dispatched to a `ScheduledThreadPoolExecutor`.

---

## API

```java
// gg/nextforge/core/util/scheduler/Scheduler.java
public interface Scheduler extends AutoCloseable {
    TaskHandle runTask(Runnable task, boolean sync);
    TaskHandle runTaskLater(Runnable task, long delayTicks, boolean sync);
    TaskHandle runTaskTimer(Runnable task, long delayTicks, long periodTicks, boolean sync);

    /** Call exactly once per server tick on the main thread. */
    void tick();

    /** Bind the current thread as the main thread (call during plugin enable on main). */
    void bindMainThread();

    /** True if called from the bound main thread. */
    boolean isMainThread();

    @Override void close();
}

// gg/nextforge/core/util/scheduler/TaskHandle.java
public interface TaskHandle {
    void cancel();
    boolean isCancelled();
}
```

### Default Implementation

```java
// gg/nextforge/core/util/scheduler/DefaultScheduler.java
public class DefaultScheduler implements Scheduler {
    // - Async: ScheduledThreadPoolExecutor
    // - Sync: PriorityQueue<SyncTask> ordered by nextRunMillis
    // - Backpressure via maxSyncPerTick
}
```

> See the source in `DefaultScheduler` for full implementation details.

---

## Usage

### Initialization

```java
Scheduler scheduler = new DefaultScheduler(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2), // async threads
        10 // max sync tasks executed per tick
);

// Bind the main thread (call this on server main thread, e.g., in onEnable)
scheduler.bindMainThread();
```

### Driving the Sync Pump

Call once per server tick **on the main thread**:

```java
// Example: from your own tick hook or an event callback
scheduler.tick();
```

### Submitting Tasks

```java
// Sync now
scheduler.runTask(() -> {
    // main-thread-safe work
}, true);

// Sync later (5 ticks)
scheduler.runTaskLater(() -> {
    // delayed sync work
}, 5L, true);

// Sync repeating (start in 20 ticks, every 20 ticks)
TaskHandle syncTimer = scheduler.runTaskTimer(() -> {
    // periodic sync work
}, 20L, 20L, true);

// Async now
scheduler.runTask(() -> {
    // background work (NO main-only API calls!)
}, false);

// Async later
scheduler.runTaskLater(() -> {
    // delayed background work
}, 40L, false);

// Async repeating
TaskHandle asyncTimer = scheduler.runTaskTimer(() -> {
    // periodic background work
}, 0L, 100L, false);

// Cancel timers
syncTimer.cancel();
asyncTimer.cancel();
```

---

## Best Practices

- **Main-thread only APIs** (like many Minecraft server APIs) must run in **sync** tasks.
- Use the scheduler as a **bridge** from async to sync:
  ```java
  scheduler.runTask(() -> {
      var result = heavyComputation(); // async
      scheduler.runTask(() -> applyResultOnMainThread(result), true);
  }, false);
  ```
- Tune **`maxSyncPerTick`** to avoid lag spikes.
- Always call **`close()`** in plugin disable to stop async threads and clear queues.

---

## Testing

- The design is fully testable: drive time via `tick()` calls and fake `System.currentTimeMillis()` if you abstract it.
- For async code, use latches/barriers in tests, or inject a single-thread executor for determinism.

---

## FAQ

**Q: Why not use `BukkitScheduler`?**  
A: This scheduler is library-level and independent from Bukkit/Paper. It can be reused in non-MC contexts, improves testability, and gives full control over backpressure and execution.

**Q: What’s a “tick”?**  
A: One server frame, ~50ms. We provide `ticks → ms` conversion internally (1 tick = 50ms).

**Q: What happens if I don't call `tick()`?**  
A: Sync tasks will not run. Ensure you pump the scheduler once per tick on the main thread.

---

## Reference

- `Scheduler` – public API
- `TaskHandle` – cancellation
- `DefaultScheduler` – implementation: sync queue + async executor
- Tick duration constant: **50 ms** (1 tick)

---

Happy scheduling!
