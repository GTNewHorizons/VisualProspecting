package com.sinthoras.visualprospecting.hooks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.zip.DataFormatException;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.RedoServerCacheCommand;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.WorldIdHandler;
import com.sinthoras.visualprospecting.database.cachebuilder.WorldAnalysis;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import com.sinthoras.visualprospecting.item.ProspectorsLog;
import com.sinthoras.visualprospecting.network.ProspectingNotification;
import com.sinthoras.visualprospecting.network.ProspectingRequest;
import com.sinthoras.visualprospecting.network.ProspectionSharing;
import com.sinthoras.visualprospecting.network.WorldIdNotification;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
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
import gregtech.api.GregTech_API;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GT_OreDictUnificator;

public class HooksShared {

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
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void fmlLifeCycleEvent(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new HooksEventBus());
        FMLCommonHandler.instance().bus().register(new HooksFML());
    }

    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void fmlLifeCycleEvent(FMLPostInitializationEvent event) {
        GregTech_API.sAfterGTPostload.add(new VeinTypeCaching());
        GregTech_API.sAfterGTPostload.add(
                () -> GT_Values.RA.addAssemblerRecipe(
                        new ItemStack[] { GT_OreDictUnificator.get(OrePrefixes.plate, Materials.Wood, 2L),
                                new ItemStack(Items.writable_book, 1, 0x7FFF),
                                new ItemStack(Items.gold_nugget, 1, 0x7FFF) },
                        Materials.Glue.getFluid(20L),
                        new ItemStack(ProspectorsLog.instance, 1, 0),
                        128,
                        8));
    }

    public void fmlLifeCycleEvent(FMLServerAboutToStartEvent event) {}

    // register server commands in this event handler
    public void fmlLifeCycleEvent(FMLServerStartingEvent event) {

        // Get the server instance, and load the first world I think
        // Also get the world directory and world spawn, and keep them for later
        final MinecraftServer minecraftServer = event.getServer();
        WorldIdHandler.load(minecraftServer.worldServers[0]);

        World entityWorld = minecraftServer.getEntityWorld();
        File worldF = entityWorld.getSaveHandler().getWorldDirectory();
        ChunkCoordinates spawn = entityWorld.getSpawnPoint();

        // If the vein cache is unloadable OR the config demands it...
        // This code was here before I was; thus, it must not crash
        if (!ServerCache.instance.loadVeinCache(WorldIdHandler.getWorldId()) || Config.recacheVeins) {

            // Try to reanalyze, with a catch
            try {

                WorldAnalysis world = new WorldAnalysis(worldF);
                world.cacheVeins();
            } catch (IOException | DataFormatException e) {

                VP.info("Could not load world save files to build vein cache!");
                e.printStackTrace();
            }
        }

        // Recaching the veins seems hopeless
        // Attempt to delete the entries instead, and let other processes handle recaching
        // Get the world ID
        String worldID = WorldIdHandler.getWorldId();

        // We indicate whether the spawn chunks have been reloaded by just sticking a file in the storage dir
        // Get that file!
        File dir = new File(Utils.getSubDirectory(Tags.SERVER_DIR), worldID + File.separator);
        File spawnState = new File(dir, "spawn_recached");

        // Try to read it, false if we can't
        boolean spawnCached;
        try {

            spawnCached = Objects.equals(Files.readAllLines(spawnState.toPath()).get(0), "True");
        } catch (IOException e) {

            spawnCached = false;
        }

        // If the veins haven't been recached, DELETUS
        if (!spawnCached) {

            // Reset the spawn chunks
            // I'm pretty sure the spawn chunks are a 16x16 area centered on the world spawn

            // Convert to chunk coords, and make a 17x17 rect centered on the spawn chunk
            // I'm *fairly certain* that this will convert block pos to chunk pos... probably
            int spawnChunkX = Utils.coordBlockToChunk(spawn.posX);
            int spawnChunkZ = Utils.coordBlockToChunk(spawn.posZ);

            // The first corner is 8 chunks less in XZ, and the last is 8 more
            int startX = spawnChunkX - 8;
            int startZ = spawnChunkZ - 8;
            int endX = spawnChunkX + 8;
            int endZ = spawnChunkZ + 8;

            // delete
            VP.info("Deleting spawn chunk ore data...");
            ServerCache.instance.resetSome(0, startX, startZ, endX, endZ);

            // Reset the file
            // Try to write to it, error if we can't
            try {

                Files.write(spawnState.toPath(), "True".getBytes());
            } catch (IOException e) {

                // This is only an error if the directory exists
                if (dir.exists()) {
                    VP.error("Could not write to " + spawnState + "!");
                    VP.error("This may result in recaching spawn every world load, or not recaching it.");
                    VP.error("Please fix this expediently.");
                } else {

                    VP.info(
                            "Could not save spawn vein status;"
                                    + " if this is the first time you opened this world, you can ignore this.");
                }
            }
        }

        event.registerServerCommand(new RedoServerCacheCommand());
    }

    public void fmlLifeCycleEvent(FMLServerStartedEvent event) {

    }

    public void fmlLifeCycleEvent(FMLServerStoppingEvent event) {
        ServerCache.instance.saveVeinCache();
        ServerCache.instance.reset();
    }

    public void fmlLifeCycleEvent(FMLServerStoppedEvent event) {}
}
