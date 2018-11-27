package co.gongzh.procbridge;

import org.jetbrains.annotations.Nullable;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * @author Gong Zhang
 */
final class TimeoutExecutor {

    private final long timeout;
    private final Runnable task;

    TimeoutExecutor(long timeout, Runnable task) {
        this.timeout = timeout;
        this.task = task;
    }

    void executeAndWait() throws TimeoutException, InterruptedException {
        executeAndWait(null);
    }

    void executeAndWait(@Nullable ExecutorService executorService) throws TimeoutException, InterruptedException {
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
        if (executorService != null) {
            executorService.execute(runnable);
        } else {
            new Thread(runnable).start();
        }

        try {
            semaphore.acquire();
            if (isTimeout[0]) {
                throw new TimeoutException();
            }
        } finally {
            timer.cancel();
        }
    }

}
