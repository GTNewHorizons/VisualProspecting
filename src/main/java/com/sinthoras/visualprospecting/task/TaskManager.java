package com.sinthoras.visualprospecting.task;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {

    public static final TaskManager instance = new TaskManager();

    private final List<ITask> tasks = new ArrayList<>();
    private final List<ITask> taskQueue = new ArrayList<>();

    public void addTask(ITask task) {
        taskQueue.add(task);
    }

    public synchronized void onTick() {
        tasks.addAll(taskQueue);
        taskQueue.clear();

        if (tasks.isEmpty())
            return;

        List<ITask> tasksToRemove = new ArrayList<>();

        for (ITask task : tasks) {
            if (task.process())
                tasksToRemove.add(task);
        }

        for (ITask task : tasksToRemove) {
            tasks.remove(task);
        }
    }
}
