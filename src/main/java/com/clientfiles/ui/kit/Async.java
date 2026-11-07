package com.clientfiles.ui.kit;

import com.clientfiles.core.AppException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/** Runs blocking work away from the event thread and returns to it with the outcome. */
public final class Async {
    private static final Logger LOG = Logger.getLogger(Async.class.getName());

    private Async() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> success, Consumer<AppException> failure) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                T result;
                try {
                    result = get();
                } catch (ExecutionException e) {
                    failure.accept(toAppException(e.getCause()));
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                success.accept(result);
            }
        }.execute();
    }

    private static AppException toAppException(Throwable cause) {
        if (cause instanceof AppException known) {
            return known;
        }
        LOG.log(Level.SEVERE, "Unexpected failure", cause);
        return new AppException("Something went wrong. Please try again.", cause, false);
    }
}
