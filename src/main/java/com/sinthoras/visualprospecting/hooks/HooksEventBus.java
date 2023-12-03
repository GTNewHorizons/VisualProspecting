package com.sinthoras.visualprospecting.hooks;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.integration.serverutilities.database.ForgeTeamDb;
import net.minecraftforge.event.world.WorldEvent;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.ServerCache;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class HooksEventBus {

    @SubscribeEvent
    public void onEvent(WorldEvent.Unload event) {
        if (Utils.isLogicalClient()) {
            ClientCache.instance.saveVeinCache();
        }
    }

    @SubscribeEvent
    public void onEvent(WorldEvent.Save event) {
        ServerCache.instance.saveVeinCache();

        if (Utils.isServerUtilitiesInstalled() && Config.enableServerUtilsTeamSharing) {
            ForgeTeamDb.instance.save();
        }
    }
}
