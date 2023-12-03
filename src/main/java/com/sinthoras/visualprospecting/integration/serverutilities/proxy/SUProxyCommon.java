package com.sinthoras.visualprospecting.integration.serverutilities.proxy;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.integration.serverutilities.SUEventBus;
import com.sinthoras.visualprospecting.integration.serverutilities.database.ForgeTeamDb;
import com.sinthoras.visualprospecting.integration.serverutilities.network.TeamDataSyncMsg;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;

public class SUProxyCommon extends SUProxyBase {

    @Override
    public void preInit(FMLPreInitializationEvent event, int networkId) {

        VP.network.registerMessage(
                TeamDataSyncMsg.ServerHandler.class,
                TeamDataSyncMsg.class,
                networkId++,
                Side.SERVER);
        VP.network.registerMessage(
                TeamDataSyncMsg.ClientHandler.class,
                TeamDataSyncMsg.class,
                networkId++,
                Side.CLIENT
        );
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (!Config.enableServerUtilsTeamSharing) return;

        MinecraftForge.EVENT_BUS.register(new SUEventBus());
    }

    @Override
    public void serverStopping(FMLServerStoppingEvent event) {
        if (!Config.enableServerUtilsTeamSharing) return;

        ForgeTeamDb.instance.save();
        ForgeTeamDb.instance.reset();
    }
}
