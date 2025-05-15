package com.sinthoras.visualprospecting;

import com.sinthoras.visualprospecting.hooks.HooksShared;
import com.sinthoras.visualprospecting.integration.gregtech.VeinDatabase;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import gregtech.crossmod.visualprospecting.VisualProspectingDatabase;

@Mod(
        modid = Tags.MODID,
        version = Tags.VERSION,
        name = Tags.MODNAME,
        acceptedMinecraftVersions = "[1.7.10]",
        dependencies = "required-after:gregtech;" + "required-after:NotEnoughItems;" + "after:navigator;")
public class VPMod {

    @SidedProxy(clientSide = Tags.GROUPNAME + ".hooks.HooksClient", serverSide = Tags.GROUPNAME + ".hooks.HooksShared")
    public static HooksShared proxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    public void fmlLifeCycleEvent(FMLPreInitializationEvent event) {
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void fmlLifeCycleEvent(FMLInitializationEvent event) {
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void fmlLifeCycle(FMLPostInitializationEvent event) {
        VisualProspectingDatabase.registerDatabase(new VeinDatabase());
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    public void fmlLifeCycle(FMLLoadCompleteEvent event) {
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    public void fmlLifeCycle(FMLServerAboutToStartEvent event) {
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler
    public void fmlLifeCycle(FMLServerStartingEvent event) {
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    public void fmlLifeCycle(FMLServerStartedEvent event) {
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    public void fmlLifeCycle(FMLServerStoppingEvent event) {
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    public void fmlLifeCycle(FMLServerStoppedEvent event) {
        proxy.fmlLifeCycleEvent(event);
    }
}
