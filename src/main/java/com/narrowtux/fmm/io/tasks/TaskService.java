package com.narrowtux.fmm.io.tasks;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TaskService {
    private static TaskService instance;

    private ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(() -> {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                e.printStackTrace();
            });

            if (r instanceof Task) {
                Task task = (Task) r;
                task.exceptionProperty().addListener(new ChangeListener<Throwable>() {
                    @Override
                    public void changed(ObservableValue obs, Throwable ov, Throwable nv) {
                        if (nv != null) {
                            nv.printStackTrace();
                        }
                    }
                });
            }

            r.run();

        });
        thread.setDaemon(true);
        return thread;
    });

    private TaskService() {
        if (instance != null) {
            throw new IllegalStateException("Is a singleton");
        }
        instance = this;
    }

    public void submit(Task task) {
        Platform.runLater(() -> {
            tasks.add(task);
        });
        executorService.submit(task);
    }

    public ObservableList<Task> getTasks() {
        return tasks;
    }

    public static TaskService getInstance() {
        if (instance == null) {
            instance = new TaskService();
        }
        return instance;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
