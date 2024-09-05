package com.sinthoras.visualprospecting;

import com.sinthoras.visualprospecting.hooks.HooksShared;
import com.sinthoras.visualprospecting.integration.gregtech.VeinDatabase;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
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
        dependencies = "required-after:gregtech;" + "after:navigator;")
public class VPMod {

    @SidedProxy(clientSide = Tags.GROUPNAME + ".hooks.HooksClient", serverSide = Tags.GROUPNAME + ".hooks.HooksShared")
    public static HooksShared proxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    public void fmlLifeCycleEvent(FMLPreInitializationEvent event) {
        VP.debug("Registered sided proxy for: " + (Utils.isLogicalClient() ? "Client" : "Dedicated server"));
        VP.debug("preInit()" + event.getModMetadata().name);
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void fmlLifeCycleEvent(FMLInitializationEvent event) {
        VP.debug("init()");
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void fmlLifeCycle(FMLPostInitializationEvent event) {
        VP.debug("postInit()");
        VP.debug("Registering with the GT5U ore vein database");
        VisualProspectingDatabase.registerDatabase(new VeinDatabase());
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    public void fmlLifeCycle(FMLServerAboutToStartEvent event) {
        VP.debug("Server about to start");
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler
    public void fmlLifeCycle(FMLServerStartingEvent event) {
        VP.debug("Server starting");
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    public void fmlLifeCycle(FMLServerStartedEvent event) {
        VP.debug("Server started");
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    public void fmlLifeCycle(FMLServerStoppingEvent event) {
        VP.debug("Server stopping");
        proxy.fmlLifeCycleEvent(event);
    }

    @Mod.EventHandler
    public void fmlLifeCycle(FMLServerStoppedEvent event) {
        VP.debug("Server stopped");
        proxy.fmlLifeCycleEvent(event);
    }
}
