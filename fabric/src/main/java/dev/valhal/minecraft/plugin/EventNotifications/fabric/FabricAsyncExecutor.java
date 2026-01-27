package dev.valhal.minecraft.plugin.EventNotifications.fabric;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FabricAsyncExecutor implements Executor {
    private final ExecutorService executorService;

    public FabricAsyncExecutor() {
        this.executorService = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r, "EventNotifications-Worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void execute(Runnable command) {
        executorService.execute(command);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
