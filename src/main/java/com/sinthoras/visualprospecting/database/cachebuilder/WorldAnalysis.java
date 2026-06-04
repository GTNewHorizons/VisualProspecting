package com.sinthoras.visualprospecting.database.cachebuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.apache.commons.io.FileUtils;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.VeinSource;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;

public class WorldAnalysis {

    private static final EnumSet<VeinSource> RESCAN_PROTECTED = EnumSet.of(VeinSource.GENERATED, VeinSource.API);

    private final File worldFolder;

    public WorldAnalysis(File worldDirectory) throws IOException {
        this.worldFolder = worldDirectory;
    }

    public void cacheOverworldSpawnVeins(ChunkCoordinates spawn) throws IOException, DataFormatException {

        VP.info("Starting to parse world save to cache GT vein locations near spawn. This might take some time...");
        ServerCache.instance.resetSpawnChunks(spawn, 0, RESCAN_PROTECTED);

        cacheVeins(IntSets.singleton(0));
    }

    public void cacheVeins() throws IOException, DataFormatException {

        VP.info("Starting to parse world save to cache GT vein locations. This might take some time...");
        ServerCache.instance.resetExcept(RESCAN_PROTECTED);

        cacheVeins(getDimensionIds());
    }

    private void cacheVeins(IntSet dimensionIds) {

        AnalysisProgressTracker.setNumberOfDimensions(dimensionIds.size());
        for (int dimensionId : dimensionIds) {
            final DimensionAnalysis dimension = new DimensionAnalysis(dimensionId);

            // Skip dimensions in with no known GT OreMixes
            if (!VeinTypeCaching.hasVeinsInDimension(dimension.dimensionName)) {
                VP.LOG.info(
                        "Skipping dimensionId={} name=\"{}\" - no ore veins generate in this dimension.",
                        dimensionId,
                        dimension.dimensionName);
                AnalysisProgressTracker.dimensionProcessed();
                continue;
            }

            final Collection<File> regionFiles = getRegionFilesForDim(dimensionId);
            dimension.processMinecraftWorld(regionFiles);
            AnalysisProgressTracker.dimensionProcessed();
        }

        AnalysisProgressTracker.processingFinished();
        VP.info("Saving ore vein cache...");
        ServerCache.instance.saveVeinCache();
    }

    public IntSet getDimensionIds() throws IOException {
        IntSet dimensionIds = new IntOpenHashSet();
        dimensionIds.addAll(Arrays.asList(DimensionManager.getIDs()));
        try (Stream<Path> files = Files.walk(worldFolder.toPath(), 1)) {
            files.filter(Files::isDirectory).map(path -> path.getFileName().toString())
                    .filter(path -> path.startsWith("DIM")).map(path -> path.substring(3)).forEach(dim -> {
                        try {
                            dimensionIds.add(Integer.parseInt(dim));
                        } catch (NumberFormatException ignored) {}
                    });
        }
        return dimensionIds;
    }

    public Collection<File> getRegionFilesForDim(int dimensionId) {
        WorldServer world = DimensionManager.getWorld(dimensionId);
        File dimRegionDir;
        if (world != null) {
            dimRegionDir = new File(world.getChunkSaveLocation(), "region");
        } else {
            final String subfolder = dimensionId == 0 ? "" : "/DIM" + dimensionId;
            dimRegionDir = new File(worldFolder, subfolder + "/region");
        }

        if (dimRegionDir.exists()) {
            return FileUtils.listFiles(dimRegionDir, new String[] { "mca" }, false);
        }
        return new ArrayList<>();
    }
}
