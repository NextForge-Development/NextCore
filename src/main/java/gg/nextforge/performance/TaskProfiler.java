package gg.nextforge.performance;

import java.util.concurrent.Callable;

/**
 * Wraps a Runnable or Callable to automatically record performance.
 */
public class TaskProfiler {

    private final PerformanceCollector collector;

    public TaskProfiler(PerformanceCollector collector) {
        this.collector = collector;
    }

    public Runnable wrap(String key, Runnable runnable) {
        return () -> {
            long start = System.nanoTime();
            try {
                runnable.run();
            } finally {
                long duration = System.nanoTime() - start;
                collector.record(new PerformanceSnapshot(key, start, duration));
            }
        };
    }

    public <T> Callable<T> wrap(String key, Callable<T> callable) {
        return () -> {
            long start = System.nanoTime();
            try {
                return callable.call();
            } finally {
                long duration = System.nanoTime() - start;
                collector.record(new PerformanceSnapshot(key, start, duration));
            }
        };
    }
}