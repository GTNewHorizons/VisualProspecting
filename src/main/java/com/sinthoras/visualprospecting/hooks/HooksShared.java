package com.sinthoras.visualprospecting.hooks;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.database.WorldIdHandler;
import com.sinthoras.visualprospecting.database.cachebuilder.WorldAnalysis;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import com.sinthoras.visualprospecting.network.ProspectingNotification;
import com.sinthoras.visualprospecting.network.ProspectingRequest;
import com.sinthoras.visualprospecting.network.WorldIdNotification;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.GregTech_API;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidRegistry;

import java.io.IOException;
import java.util.zip.DataFormatException;


public class HooksShared {
	
	// preInit "Run before anything else. Read your config, create blocks, items, 
	// etc, and register them with the GameRegistry."
	public void fmlLifeCycleEvent(FMLPreInitializationEvent event) 	{
		Config.syncronizeConfiguration(event.getSuggestedConfigurationFile());

		VP.network = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MODID);
		int networkId = 0;
		VP.network.registerMessage(ProspectingRequest.Handler.class, ProspectingRequest.class, networkId++, Side.SERVER);
		VP.network.registerMessage(ProspectingNotification.Handler.class, ProspectingNotification.class, networkId++, Side.CLIENT);
		VP.network.registerMessage(WorldIdNotification.Handler.class, WorldIdNotification.class, networkId++, Side.CLIENT);
	}
	
	// load "Do your mod setup. Build whatever data structures you care about. Register recipes."
	public void fmlLifeCycleEvent(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new HooksEventBus());
	}
	
	// postInit "Handle interaction with other mods, complete your setup based on this."
	public void fmlLifeCycleEvent(FMLPostInitializationEvent event) {
		GregTech_API.sAfterGTPostload.add(new VeinTypeCaching());

		VP.naturalGas = FluidRegistry.getFluid("gas_natural_gas");
		VP.lightOil = FluidRegistry.getFluid("liquid_light_oil");
		VP.mediumOil = FluidRegistry.getFluid("liquid_medium_oil");
		VP.heavyOil = FluidRegistry.getFluid("liquid_heavy_oil");
	}
	
	public void fmlLifeCycleEvent(FMLServerAboutToStartEvent event) {

	}

	// register server commands in this event handler
	public void fmlLifeCycleEvent(FMLServerStartingEvent event) {
		final MinecraftServer minecraftServer = event.getServer();
		WorldIdHandler.load(minecraftServer.worldServers[0]);
		if(VP.serverCache.loadVeinCache(WorldIdHandler.getWorldId()) == false || Config.recacheVeins) {
			try {
				WorldAnalysis world = new WorldAnalysis(minecraftServer.getEntityWorld().getSaveHandler().getWorldDirectory());
				world.cacheVeins();
				VP.serverCache.saveVeinCache();
			}
			catch (IOException | DataFormatException e) {
				VP.info("Could not load world save files to build vein cache!");
				e.printStackTrace();
			}
		}
	}
	
	public void fmlLifeCycleEvent(FMLServerStartedEvent event) {
		
	}
	
	public void fmlLifeCycleEvent(FMLServerStoppingEvent event) {
		VP.serverCache.saveVeinCache();
	}
	
	public void fmlLifeCycleEvent(FMLServerStoppedEvent event) {
		
	}
}