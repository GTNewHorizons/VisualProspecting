package com.sinthoras.visualprospecting.network;

import java.util.Collections;
import java.util.List;

import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Similar to {@link ProspectingNotification} but for data being sent as recorded team state instead of a live
 * prospection event.
 * <p>
 * Used by the team-sharing catch-up paths (player login and post-team-merge).
 */
public class TeamCatchupNotification extends ProspectingNotification {

    public TeamCatchupNotification() {
        super();
    }

    public TeamCatchupNotification(List<OreVeinPosition> oreVeins, List<UndergroundFluidPosition> undergroundFluids) {
        super(oreVeins, undergroundFluids);
    }

    public static TeamCatchupNotification veins(List<OreVeinPosition> oreVeins) {
        return new TeamCatchupNotification(oreVeins, Collections.emptyList());
    }

    public static TeamCatchupNotification fluids(List<UndergroundFluidPosition> undergroundFluids) {
        return new TeamCatchupNotification(Collections.emptyList(), undergroundFluids);
    }

    public static class ClientHandler implements IMessageHandler<TeamCatchupNotification, IMessage> {

        @Override
        public IMessage onMessage(TeamCatchupNotification message, MessageContext ctx) {
            ClientCache.instance.putOreVeins(message.getOreVeins(), true);
            ClientCache.instance.putUndergroundFluids(message.getUndergroundFluids(), true);
            return null;
        }
    }
}
