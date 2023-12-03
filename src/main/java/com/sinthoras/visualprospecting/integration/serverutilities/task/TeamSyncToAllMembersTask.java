package com.sinthoras.visualprospecting.integration.serverutilities.task;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.integration.serverutilities.network.TeamDataSyncMsg;
import net.minecraft.entity.player.EntityPlayerMP;
import serverutils.lib.data.ForgeTeam;

import java.util.List;

public class TeamSyncToAllMembersTask extends SUSyncTask {

    private final ForgeTeam team;

    protected TeamSyncToAllMembersTask(ForgeTeam team, List<OreVeinPosition> oreVeins, List<UndergroundFluidPosition> undergroundFluids) {
        super(oreVeins, undergroundFluids);
        this.team = team;
    }

    @Override
    protected void run() {
        final TeamDataSyncMsg packet = new TeamDataSyncMsg();

        oreVeins.subList(0, packet.addOreVeins(oreVeins))
                .clear();
        undergroundFluids.subList(0, packet.addUndergroundFluids(undergroundFluids))
                .clear();

        for (EntityPlayerMP member : team.getOnlineMembers()) {
            VP.network.sendTo(packet, member);
        }
    }
}
