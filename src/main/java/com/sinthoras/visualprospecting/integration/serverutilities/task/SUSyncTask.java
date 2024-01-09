package com.sinthoras.visualprospecting.integration.serverutilities.task;

import java.util.List;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.task.ITask;

public abstract class SUSyncTask implements ITask {

    protected final List<OreVeinPosition> oreVeins;
    protected final List<UndergroundFluidPosition> undergroundFluids;
    private long lastTimestamp = 0;

    protected SUSyncTask(List<OreVeinPosition> oreVeins, List<UndergroundFluidPosition> undergroundFluids) {
        this.oreVeins = oreVeins;
        this.undergroundFluids = undergroundFluids;
    }

    @Override
    public boolean process() {
        boolean done = workDone();
        long timestamp = System.currentTimeMillis();

        if (timestamp - lastTimestamp > 1000 / Config.uploadPacketsPerSecond && !done) {
            lastTimestamp = timestamp;
            run();
        }

        return done;
    }

    protected abstract void run();

    protected boolean workDone() {
        return oreVeins.isEmpty() && undergroundFluids.isEmpty();
    }
}
