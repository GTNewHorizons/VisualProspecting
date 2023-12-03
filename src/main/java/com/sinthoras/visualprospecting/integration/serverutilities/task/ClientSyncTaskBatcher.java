package com.sinthoras.visualprospecting.integration.serverutilities.task;

import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.task.ITask;
import com.sinthoras.visualprospecting.task.TaskManager;

import java.util.ArrayList;
import java.util.List;

public class ClientSyncTaskBatcher implements ITask {

    public static final ClientSyncTaskBatcher instance = new ClientSyncTaskBatcher();

    private final List<OreVeinPosition> oreVeins = new ArrayList<>();
    private final List<UndergroundFluidPosition> undergroundFluids = new ArrayList<>();
    private long lastTimestamp = 0;

    private ClientSyncTaskBatcher() {
        TaskManager.instance.addTask(this);
    }

    public void addOreVein(OreVeinPosition oreVein) {
        oreVeins.add(oreVein);
    }

    public void addUndergroundFluid(UndergroundFluidPosition undergroundFluid) {
        undergroundFluids.add(undergroundFluid);
    }

    @Override
    public boolean process() {
        long timestamp = System.currentTimeMillis();

        if (timestamp - lastTimestamp > 1000) {
            lastTimestamp = timestamp;
            run();
        }

        return false;
    }

    private void run() {
        int oreVeinCount = this.oreVeins.size();
        int undergroundFluidsCount = this.undergroundFluids.size();

        List<OreVeinPosition> oreVeins = new ArrayList<>(this.oreVeins);
        List<UndergroundFluidPosition> undergroundFluids = new ArrayList<>(this.undergroundFluids);

        ClientSyncToTeamTask task = new ClientSyncToTeamTask(oreVeins, undergroundFluids);
        TaskManager.instance.addTask(task);

        this.oreVeins.subList(0, oreVeinCount)
                .clear();
        this.undergroundFluids.subList(0, undergroundFluidsCount)
                .clear();
    }
}