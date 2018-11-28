package co.gongzh.procbridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * @author Gong Zhang
 */
final class TimeoutExecutor implements Executor {

    private final long timeout;
    private final @Nullable Executor base;

    TimeoutExecutor(long timeout, @Nullable Executor base) {
        this.timeout = timeout;
        this.base = base;
    }

    public long getTimeout() {
        return timeout;
    }

    @Nullable
    public Executor getBaseExecutor() {
        return base;
    }

    @Override
    public void execute(@NotNull Runnable task) throws TimeoutException {
        final Semaphore semaphore = new Semaphore(0);
        final boolean[] isTimeout = { false };

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                isTimeout[0] = true;
                semaphore.release();
            }
        }, timeout);

        Runnable runnable = () -> {
            try {
                task.run();
            } finally {
                semaphore.release();
            }
        };
        if (base != null) {
            base.execute(runnable);
        } else {
            new Thread(runnable).start();
        }

        try {
            semaphore.acquire();
            if (isTimeout[0]) {
                throw new TimeoutException();
            }
        } catch (InterruptedException ignored) {
        } finally {
            timer.cancel();
        }
    }

}
