package com.sinthoras.visualprospecting.task;

import java.util.List;

import net.minecraft.client.Minecraft;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.network.ProspectionSharing;

public class SnapshotUploadTask implements ITask {

    private final List<OreVeinPosition> oreVeins;
    private final List<UndergroundFluidPosition> undergroundFluids;
    private final boolean routeToTeam;
    private long lastUpload = 0;
    private boolean firstMessage = true;

    public SnapshotUploadTask() {
        this(false);
    }

    public SnapshotUploadTask(boolean routeToTeam) {
        oreVeins = ClientCache.instance.getAllOreVeins();
        undergroundFluids = ClientCache.instance.getAllUndergroundFluids();
        this.routeToTeam = routeToTeam;
    }

    @Override
    public boolean process() {
        if (Minecraft.getMinecraft().getNetHandler() == null) return true;

        final long timestamp = System.currentTimeMillis();
        if (timestamp - lastUpload > 1000 / Config.uploadPacketsPerSecond && !listsEmpty()) {
            lastUpload = timestamp;
            final ProspectionSharing packet = new ProspectionSharing();

            final int addedOreVeins = packet.putOreVeins(oreVeins);
            oreVeins.subList(0, addedOreVeins).clear();

            final int addedUndergroundFluids = packet.putOreUndergroundFluids(undergroundFluids);
            undergroundFluids.subList(0, addedUndergroundFluids).clear();

            packet.setFirstMessage(firstMessage);
            firstMessage = false;

            packet.setLastMessage(listsEmpty());
            packet.setRouteToTeam(routeToTeam);

            VP.network.sendToServer(packet);
        }
        return listsEmpty();
    }

    private boolean listsEmpty() {
        return oreVeins.isEmpty() && undergroundFluids.isEmpty();
    }
}
