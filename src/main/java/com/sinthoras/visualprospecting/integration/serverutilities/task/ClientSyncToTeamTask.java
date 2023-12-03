package com.sinthoras.visualprospecting.integration.serverutilities.task;

import java.util.List;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.integration.serverutilities.network.TeamDataSyncMsg;

public class ClientSyncToTeamTask extends SUSyncTask {

    public ClientSyncToTeamTask(List<OreVeinPosition> oreVeins, List<UndergroundFluidPosition> undergroundFluids) {
        super(oreVeins, undergroundFluids);
    }

    @Override
    protected void run() {
        final TeamDataSyncMsg packet = new TeamDataSyncMsg();

        oreVeins.subList(0, packet.addOreVeins(oreVeins)).clear();
        undergroundFluids.subList(0, packet.addUndergroundFluids(undergroundFluids)).clear();

        VP.network.sendToServer(packet);
    }
}
