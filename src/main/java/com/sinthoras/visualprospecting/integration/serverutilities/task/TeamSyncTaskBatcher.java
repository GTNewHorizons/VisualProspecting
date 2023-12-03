package com.sinthoras.visualprospecting.integration.serverutilities.task;

import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.task.ITask;
import com.sinthoras.visualprospecting.task.TaskManager;
import serverutils.lib.data.ForgeTeam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamSyncTaskBatcher implements ITask {

    public static final TeamSyncTaskBatcher instance = new TeamSyncTaskBatcher();

    private final Map<String, TeamBatchData> teamBatchMap = new HashMap<>();
    private long lastTimestamp = 0;

    private TeamSyncTaskBatcher() {
        TaskManager.instance.addTask(this);
    }

    public void addOreVeins(ForgeTeam team, List<OreVeinPosition> oreVeins) {
        getTeamBatchData(team).addOreVeins(oreVeins);
    }

    public void addUndergroundFluids(ForgeTeam team, List<UndergroundFluidPosition> undergroundFluids) {
        getTeamBatchData(team).addUndergroundFluids(undergroundFluids);
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
        if (isEmpty())
            return;

        List<String> entriesToRemove = new ArrayList<>();
        teamBatchMap.forEach((key, teamBatchData) -> { if (teamBatchData.sendData()) entriesToRemove.add(key); });

        for (String key : entriesToRemove) {
            teamBatchMap.remove(key);
        }
    }

    private TeamBatchData getTeamBatchData(ForgeTeam team) {
        return teamBatchMap.computeIfAbsent(team.getUIDCode(), (_key) -> new TeamBatchData(team));
    }

    private boolean isEmpty() {
        return teamBatchMap.isEmpty();
    }

    private static class TeamBatchData {

        private final ForgeTeam team;
        private final List<OreVeinPosition> oreVeins = new ArrayList<>();
        private final List<UndergroundFluidPosition> undergroundFluids = new ArrayList<>();

        private TeamBatchData(ForgeTeam team) {
            this.team = team;
        }

        private void addOreVeins(List<OreVeinPosition> oreVeins) {
            this.oreVeins.addAll(oreVeins);
        }

        public void addUndergroundFluids(List<UndergroundFluidPosition> undergroundFluids) {
            this.undergroundFluids.addAll(undergroundFluids);
        }

        private boolean sendData() {
            TaskManager.instance.addTask(new TeamSyncToAllMembersTask(team, oreVeins, undergroundFluids));
            return true;
        }
    }
}
