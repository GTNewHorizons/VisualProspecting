package com.sinthoras.visualprospecting.network;

import java.util.List;

import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Similar as {@link ProspectingNotification} but indicates that the data is being delivered as recorded team state and
 * not a live prospection event.
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

    public static class ClientHandler implements IMessageHandler<TeamCatchupNotification, IMessage> {

        @Override
        public IMessage onMessage(TeamCatchupNotification message, MessageContext ctx) {
            ClientCache.instance.putOreVeinsSilent(message.getOreVeins());
            ClientCache.instance.putUndergroundFluidsSilent(message.getUndergroundFluids());
            return null;
        }
    }
}
