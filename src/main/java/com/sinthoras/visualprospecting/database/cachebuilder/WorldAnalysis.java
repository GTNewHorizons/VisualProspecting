package com.sinthoras.visualprospecting.database.cachebuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.DataFormatException;

import net.minecraft.util.ChunkCoordinates;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ServerCache;

import io.xol.enklume.MinecraftWorld;

public class WorldAnalysis {

    private final MinecraftWorld world;

    public WorldAnalysis(File worldDirectory) throws IOException {
        world = new MinecraftWorld(worldDirectory);
    }

    public void cacheVeins() throws IOException, DataFormatException {
        VP.info("Starting to parse world save to cache GT vein locations. This might take some time...");
        ServerCache.instance.reset();
        final List<Integer> dimensionIds = world.getDimensionIds();
        AnalysisProgressTracker.setNumberOfDimensions(dimensionIds.size());
        for (int dimensionId : dimensionIds) {
            final DimensionAnalysis dimension = new DimensionAnalysis(dimensionId);
            dimension.processMinecraftWorld(world);
            AnalysisProgressTracker.dimensionProcessed();
        }
        AnalysisProgressTracker.processingFinished();
        VP.info("Saving ore vein cache...");
        ServerCache.instance.saveVeinCache();
    }

    // This only does overworld spawn
    public void cacheSpawnVeins(ChunkCoordinates spawn) throws IOException, DataFormatException {

        // Message and reset
        VP.info("Starting to parse world save to cache GT vein locations near spawn. This might take some time...");
        ServerCache.instance.resetSpawn(spawn, 0);

        // Only doing one, DIM0 = overworld
        AnalysisProgressTracker.setNumberOfDimensions(1);
        final DimensionAnalysis dimension = new DimensionAnalysis(0);
        dimension.processMinecraftWorld(world);
        AnalysisProgressTracker.dimensionProcessed();
        AnalysisProgressTracker.processingFinished();
        VP.info("Saving ore vein cache...");
        ServerCache.instance.saveVeinCache();
    }
}
