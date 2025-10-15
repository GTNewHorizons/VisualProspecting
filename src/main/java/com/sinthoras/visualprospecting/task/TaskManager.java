package com.sinthoras.visualprospecting.task;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TaskManager {

    // Both instances are initialized in the sided proxy
    @SideOnly(Side.CLIENT)
    public static TaskManager CLIENT_INSTANCE;
    public static TaskManager SERVER_INSTANCE;

    private final List<ITask> tasks = new ArrayList<>();

    public void addTask(ITask task) {
        tasks.add(task);
    }

    public void onTick() {
        tasks.removeIf(ITask::process);
    }
}
