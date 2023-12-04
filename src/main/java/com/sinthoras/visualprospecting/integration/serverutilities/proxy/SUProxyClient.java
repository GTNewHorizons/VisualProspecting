package com.sinthoras.visualprospecting.integration.serverutilities.proxy;

import net.minecraftforge.common.MinecraftForge;

import com.sinthoras.visualprospecting.integration.serverutilities.SUEventBusClient;

import cpw.mods.fml.common.event.FMLInitializationEvent;

public class SUProxyClient extends SUProxyCommon {

    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new SUEventBusClient());
    }
}
