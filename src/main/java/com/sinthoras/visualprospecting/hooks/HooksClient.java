package com.sinthoras.visualprospecting.hooks;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizons.navigator.api.NavigatorApi;
import com.gtnewhorizons.navigator.api.util.Util;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.ResetClientCacheCommand;
import com.sinthoras.visualprospecting.integration.model.layers.OreVeinLayerManager;
import com.sinthoras.visualprospecting.integration.model.layers.UndergroundFluidLayerManager;
import com.sinthoras.visualprospecting.integration.voxelmap.VoxelMapEventHandler;
import com.sinthoras.visualprospecting.task.TaskManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;

public class HooksClient extends HooksShared {

    @Override
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void fmlLifeCycleEvent(FMLPreInitializationEvent event) {
        super.fmlLifeCycleEvent(event);
        if (Utils.isNavigatorInstalled()) {
            registerMapLayers();
        }

        initializeTaskManager();
    }

    @Override
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void fmlLifeCycleEvent(FMLInitializationEvent event) {
        super.fmlLifeCycleEvent(event);
        FMLCommonHandler.instance().bus().register(new HooksKey());
    }

    @Override
    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void fmlLifeCycleEvent(FMLPostInitializationEvent event) {
        super.fmlLifeCycleEvent(event);
        ClientCommandHandler.instance.registerCommand(new ResetClientCacheCommand());
    }

    @Override
    public void fmlLifeCycleEvent(FMLServerAboutToStartEvent event) {
        super.fmlLifeCycleEvent(event);
    }

    @Override
    public void fmlLifeCycleEvent(FMLServerStartingEvent event) {
        super.fmlLifeCycleEvent(event);
    }

    @Override
    public void fmlLifeCycleEvent(FMLServerStartedEvent event) {
        super.fmlLifeCycleEvent(event);
    }

    @Override
    public void fmlLifeCycleEvent(FMLServerStoppingEvent event) {
        super.fmlLifeCycleEvent(event);
    }

    @Override
    public void fmlLifeCycleEvent(FMLServerStoppedEvent event) {
        super.fmlLifeCycleEvent(event);
    }

    @Override
    protected void initializeTaskManager() {
        TaskManager.SERVER_INSTANCE = new TaskManager();
        TaskManager.CLIENT_INSTANCE = new TaskManager();
    }

    public void registerMapLayers() {

        NavigatorApi.registerLayerManager(OreVeinLayerManager.instance);
        NavigatorApi.registerLayerManager(UndergroundFluidLayerManager.instance);

        if (Util.isVoxelMapInstalled()) {
            MinecraftForge.EVENT_BUS.register(new VoxelMapEventHandler());
        }
    }
}
