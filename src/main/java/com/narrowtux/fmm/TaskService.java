package com.narrowtux.fmm;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskService {
    private static TaskService instance;

    private ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    {
        if (instance != null) {
            throw new IllegalStateException();
        }
        instance = this;

        getTasks().addListener((ListChangeListener<Task>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Task task : change.getAddedSubList()) {
                        executorService.submit(task);
                    }
                }
            }
        });
    }

    public ObservableList<Task> getTasks() {
        return tasks;
    }

    public static TaskService getInstance() {
        if (instance == null) {
            return new TaskService();
        }
        return instance;
    }

}
