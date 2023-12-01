package com.sinthoras.visualprospecting.integration.serverutilities.proxy;

import com.sinthoras.visualprospecting.integration.serverutilities.SUEventBus;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

public class SUProxyCommon extends SUProxyBase {
    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new SUEventBus());
    }
}
