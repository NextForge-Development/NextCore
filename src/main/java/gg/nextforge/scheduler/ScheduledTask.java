package gg.nextforge.scheduler;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ScheduledFuture;

public class ScheduledTask {

    @Getter
    private final int id;

    @Setter
    private ScheduledFuture<?> future;

    public ScheduledTask(int id) {
        this.id = id;
    }

    public void cancel() {
        if (future != null) {
            future.cancel(false);
        }
    }
}
