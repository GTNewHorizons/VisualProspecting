package com.sinthoras.visualprospecting.integration.serverutilities;

import com.sinthoras.visualprospecting.database.WorldIdHandler;
import com.sinthoras.visualprospecting.hooks.ProspectingNotificationEvent;
import com.sinthoras.visualprospecting.integration.serverutilities.database.ForgeTeamDb;
import com.sinthoras.visualprospecting.integration.serverutilities.task.ClientSyncTaskBatcher;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import serverutils.events.team.ForgeTeamCreatedEvent;
import serverutils.events.team.ForgeTeamDeletedEvent;
import serverutils.events.team.ForgeTeamLoadedEvent;
import serverutils.events.team.ForgeTeamPlayerJoinedEvent;
import serverutils.lib.data.ForgeTeam;

public class SUEventBus {

    @SubscribeEvent
    public void onProspectingOreNotificationEvent(ProspectingNotificationEvent.OreVein event) {

        ClientSyncTaskBatcher.instance.addOreVein(event.getPosition());
    }

    @SubscribeEvent
    public void OnProspectingFluidNotificationEvent(ProspectingNotificationEvent.UndergroundFluid event) {
        ClientSyncTaskBatcher.instance.addUndergroundFluid(event.getPosition());
    }

    @SubscribeEvent
    public void onTeamCreated(ForgeTeamCreatedEvent event) {
        ForgeTeam team = event.getTeam();

        ForgeTeamDb.instance.get(team)
            .loadVeinCache(WorldIdHandler.getWorldId());
    }

    @SubscribeEvent
    public void onTeamLoaded(ForgeTeamLoadedEvent event) {
        ForgeTeam team = event.getTeam();

        ForgeTeamDb.instance.get(team)
            .loadVeinCache(WorldIdHandler.getWorldId());
    }

    @SubscribeEvent
    public void onTeamDeleted(ForgeTeamDeletedEvent event) {
        ForgeTeam team = event.getTeam();

        ForgeTeamDb.instance.delete(team, WorldIdHandler.getWorldId());
    }

    @SubscribeEvent
    public void onTeamPlayerJoined(ForgeTeamPlayerJoinedEvent event) {
        EntityPlayerMP player = event.getPlayer().getPlayer();
        ForgeTeamDb.instance.syncPlayer(player);
    }
}
