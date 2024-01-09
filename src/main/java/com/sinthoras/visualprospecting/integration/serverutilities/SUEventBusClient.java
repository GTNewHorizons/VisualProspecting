package com.sinthoras.visualprospecting.integration.serverutilities;

import com.sinthoras.visualprospecting.hooks.ProspectingNotificationEvent;
import com.sinthoras.visualprospecting.integration.serverutilities.task.ClientSyncTaskBatcher;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class SUEventBusClient {

    @SubscribeEvent
    public void onProspectingOreNotificationEvent(ProspectingNotificationEvent.OreVein event) {
        ClientSyncTaskBatcher.instance.addOreVein(event.getPosition());
    }

    @SubscribeEvent
    public void OnProspectingFluidNotificationEvent(ProspectingNotificationEvent.UndergroundFluid event) {
        ClientSyncTaskBatcher.instance.addUndergroundFluid(event.getPosition());
    }
}
