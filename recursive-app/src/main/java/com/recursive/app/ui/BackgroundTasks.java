package com.recursive.app.ui;

import javafx.concurrent.Task;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs blocking service calls off the JavaFX application thread and
 * marshals the outcome back onto it. Service calls (database queries, OCR,
 * model requests) must never run on the FX thread.
 */
public final class BackgroundTasks {

    private BackgroundTasks() {
    }

    public static <T> void run(String threadName, Supplier<T> work,
                               Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onError.accept(task.getException()));
        Thread thread = new Thread(task, threadName);
        thread.setDaemon(true);
        thread.start();
    }
}
