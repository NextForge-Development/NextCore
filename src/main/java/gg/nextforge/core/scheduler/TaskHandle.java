package gg.nextforge.core.scheduler;

public interface TaskHandle {
    void cancel();
    boolean isCancelled();
}
