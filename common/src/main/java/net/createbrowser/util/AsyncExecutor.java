package net.createbrowser.util;

import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Thread pool for off-game-thread I/O. Results are dispatched back to the game thread. */
public final class AsyncExecutor {

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static final ExecutorService POOL = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "CreateBrowser-IO");
        t.setDaemon(true);
        return t;
    });

    private AsyncExecutor() {}

    public static <T> void run(ThrowingSupplier<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return task.get();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, POOL)
        .thenAcceptAsync(
            result -> Minecraft.getInstance().execute(() -> onSuccess.accept(result)),
            POOL)
        .exceptionally(ex -> {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            Minecraft.getInstance().execute(() -> onError.accept(cause));
            return null;
        });
    }
}
