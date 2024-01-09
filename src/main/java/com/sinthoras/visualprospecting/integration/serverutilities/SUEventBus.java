package com.sinthoras.visualprospecting.integration.serverutilities;

import net.minecraft.entity.player.EntityPlayerMP;

import com.sinthoras.visualprospecting.database.WorldIdHandler;
import com.sinthoras.visualprospecting.integration.serverutilities.database.ForgeTeamCache;
import com.sinthoras.visualprospecting.integration.serverutilities.database.ForgeTeamDb;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.events.team.ForgeTeamCreatedEvent;
import serverutils.events.team.ForgeTeamDeletedEvent;
import serverutils.events.team.ForgeTeamLoadedEvent;
import serverutils.events.team.ForgeTeamPlayerJoinedEvent;
import serverutils.events.team.ForgeTeamPlayerLeftEvent;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;

public class SUEventBus {

    @SubscribeEvent
    public void onTeamCreated(ForgeTeamCreatedEvent event) {
        ForgeTeam team = event.getTeam();

        ForgeTeamCache teamCache = ForgeTeamDb.instance.get(team);
        teamCache.loadVeinCache(WorldIdHandler.getWorldId());

        ForgePlayer owner = team.getOwner();
        if (owner == null) return;

        EntityPlayerMP player = owner.getPlayer();
        if (player == null) return;

        teamCache.syncPlayer(player);
    }

    @SubscribeEvent
    public void onTeamLoaded(ForgeTeamLoadedEvent event) {
        ForgeTeam team = event.getTeam();

        ForgeTeamDb.instance.get(team).loadVeinCache(WorldIdHandler.getWorldId());
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

    @SubscribeEvent
    public void onTeamPlayerLeft(ForgeTeamPlayerLeftEvent event) {
        EntityPlayerMP player = event.getPlayer().getPlayer();
        ForgeTeamDb.instance.get(event.getTeam()).removePlayer(player);
    }
}
