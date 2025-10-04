package com.sinthoras.visualprospecting.hooks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.WorldIdHandler;
import com.sinthoras.visualprospecting.network.WorldIdNotification;
import com.sinthoras.visualprospecting.task.TaskManager;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class HooksFML {

    @SubscribeEvent
    public void onEvent(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        ClientCache.instance.reset();
    }

    @SubscribeEvent
    public void onEvent(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientCache.instance.saveVeinCache();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        TaskManager.SERVER_INSTANCE.onTick();
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        TaskManager.CLIENT_INSTANCE.onTick();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onEvent(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP playerMP) {
            VP.network.sendTo(new WorldIdNotification(WorldIdHandler.getWorldId()), playerMP);
        } else if (event.player instanceof EntityPlayer) {
            ClientCache.instance.loadVeinCache(WorldIdHandler.getWorldId());
        }
    }
}
