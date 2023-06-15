package com.sinthoras.visualprospecting.database.cachebuilder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.zip.DataFormatException;

import net.minecraft.util.ChunkCoordinates;

import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ServerCache;

import io.xol.enklume.MinecraftWorld;

public class WorldAnalysis {

    private final MinecraftWorld world;

    public WorldAnalysis(File worldDirectory) throws IOException {
        world = new MinecraftWorld(worldDirectory);
    }

    public void cacheOverworldSpawnVeins(ChunkCoordinates spawn) throws IOException, DataFormatException {

        VP.info("Starting to parse world save to cache GT vein locations near spawn. This might take some time...");
        ServerCache.instance.resetSpawnChunks(spawn, 0);

        cacheVeins(Collections.singletonList(Tags.overworldId));
    }

    public void cacheVeins() throws IOException, DataFormatException {

        VP.info("Starting to parse world save to cache GT vein locations. This might take some time...");
        ServerCache.instance.reset();

        cacheVeins(world.getDimensionIds());
    }

    private void cacheVeins(List<Integer> dimensionIds) throws IOException, DataFormatException {

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
}
