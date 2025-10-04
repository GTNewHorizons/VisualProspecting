package com.sinthoras.visualprospecting.hooks;

import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.RedoServerCacheCommand;
import com.sinthoras.visualprospecting.database.RedoServerSpawnCacheCommand;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.WorldIdHandler;
import com.sinthoras.visualprospecting.database.cachebuilder.WorldAnalysis;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import com.sinthoras.visualprospecting.item.ProspectorsLog;
import com.sinthoras.visualprospecting.network.ProspectingNotification;
import com.sinthoras.visualprospecting.network.ProspectingRequest;
import com.sinthoras.visualprospecting.network.ProspectionSharing;
import com.sinthoras.visualprospecting.network.WorldIdNotification;
import com.sinthoras.visualprospecting.task.TaskManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;

public class HooksShared {

    private static boolean newWorld = false;

    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    public void fmlLifeCycleEvent(FMLPreInitializationEvent event) {
        Config.syncronizeConfiguration(event.getSuggestedConfigurationFile());

        VP.network = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MODID);
        int networkId = 0;
        VP.network
                .registerMessage(ProspectingRequest.Handler.class, ProspectingRequest.class, networkId++, Side.SERVER);
        VP.network.registerMessage(
                ProspectingNotification.Handler.class,
                ProspectingNotification.class,
                networkId++,
                Side.CLIENT);
        VP.network.registerMessage(
                WorldIdNotification.Handler.class,
                WorldIdNotification.class,
                networkId++,
                Side.CLIENT);
        VP.network.registerMessage(
                ProspectionSharing.ServerHandler.class,
                ProspectionSharing.class,
                networkId++,
                Side.SERVER);
        VP.network.registerMessage(
                ProspectionSharing.ClientHandler.class,
                ProspectionSharing.class,
                networkId++,
                Side.CLIENT);

        ProspectorsLog.instance = new ProspectorsLog();
        GameRegistry.registerItem(ProspectorsLog.instance, ProspectorsLog.instance.getUnlocalizedName());

        initializeTaskManager();
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void fmlLifeCycleEvent(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new HooksEventBus());
        FMLCommonHandler.instance().bus().register(new HooksFML());
    }

    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void fmlLifeCycleEvent(FMLPostInitializationEvent event) {}

    public void fmlLifeCycleEvent(FMLLoadCompleteEvent event) {
        VeinTypeCaching.init();
    }

    public void fmlLifeCycleEvent(FMLServerAboutToStartEvent event) {
        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        if (worldDir != null) {
            File regionFolder = new File(worldDir, "region");
            newWorld = !regionFolder.exists();
        }
    }

    // register server commands in this event handler
    public void fmlLifeCycleEvent(FMLServerStartingEvent event) {

        // Get the server and load the ID handler
        WorldServer world = DimensionManager.getWorld(0);
        WorldIdHandler.load(world);

        // Attempt to load the vein cache. If unable or forcing a recache...
        boolean loaded = ServerCache.instance.loadVeinCache(WorldIdHandler.getWorldId());
        if (!newWorld && (!loaded || Config.recacheVeins)) {

            // Reanalyze the world and reload it into memory.
            try {
                WorldAnalysis worldAnalysis = new WorldAnalysis(world.getSaveHandler().getWorldDirectory());
                worldAnalysis.cacheVeins();
            } catch (IOException | DataFormatException e) {
                VP.LOG.info("Could not load world save files to build vein cache!", e);
            }
        }

        // Register the recache command
        event.registerServerCommand(new RedoServerCacheCommand());
        event.registerServerCommand(new RedoServerSpawnCacheCommand());
    }

    public void fmlLifeCycleEvent(FMLServerStartedEvent event) {}

    public void fmlLifeCycleEvent(FMLServerStoppingEvent event) {
        ServerCache.instance.saveVeinCache();
        ServerCache.instance.reset();
    }

    public void fmlLifeCycleEvent(FMLServerStoppedEvent event) {}

    protected void initializeTaskManager() {
        TaskManager.SERVER_INSTANCE = new TaskManager();
    }
}
