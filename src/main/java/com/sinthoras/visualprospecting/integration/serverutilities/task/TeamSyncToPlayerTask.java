package com.sinthoras.visualprospecting.integration.serverutilities.task;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.integration.serverutilities.network.TeamDataSyncMsg;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

public class TeamSyncToPlayerTask extends SUSyncTask {

    private final EntityPlayerMP targetPlayer;

    public TeamSyncToPlayerTask(EntityPlayerMP player, List<OreVeinPosition> oreVeins, List<UndergroundFluidPosition> undergroundFluids) {
        super(oreVeins, undergroundFluids);

        this.targetPlayer = player;
    }

    @Override
    protected void run() {
        final TeamDataSyncMsg packet = new TeamDataSyncMsg();

        oreVeins.subList(0, packet.addOreVeins(oreVeins))
                .clear();
        undergroundFluids.subList(0, packet.addUndergroundFluids(undergroundFluids))
                .clear();

        VP.network.sendTo(packet, targetPlayer);
    }
}
