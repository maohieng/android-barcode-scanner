package github.jomutils.android.barcode;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps an existing executor to provide a [.shutdown] method that allows subsequent
 * cancellation of submitted runnables.
 */
public class ScopedExecutor implements Executor {
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private final Executor executor;

    public ScopedExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Runnable command) {
        // Return early if this object has been shut down.
        if (shutdown.get()) {
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (shutdown.get()) {
                    return;
                }

                command.run();
            }
        });
    }

    public void shutdown() {
        shutdown.set(true);
    }

}