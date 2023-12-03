package com.sinthoras.visualprospecting.integration.serverutilities.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;

public class SUProxyBase {

    public void preInit(FMLPreInitializationEvent event, int networkId) {}

    public void init(FMLInitializationEvent event) {}

    public void serverStopping(FMLServerStoppingEvent event) {}
}
