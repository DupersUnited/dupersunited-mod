package wtf.dupers.dupersunited.features.fleet;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class FleetScheduler {
    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Fleet");
        t.setDaemon(true);
        return t;
    });

    public static void after(Runnable task, long delayMs) {
        if (delayMs <= 0) {
            task.run();
        } else {
            EXEC.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        }
    }
}