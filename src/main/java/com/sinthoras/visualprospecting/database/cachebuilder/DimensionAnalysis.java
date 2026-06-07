package com.sinthoras.visualprospecting.database.cachebuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import com.github.bsideup.jabel.Desugar;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.VeinSource;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;

import cpw.mods.fml.common.Optional;
import gregtech.api.enums.Mods;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.prefab.world.gen.WorldProviderSpace;

public class DimensionAnalysis {

    private static final Pattern REGION_FILE_NAME = Pattern.compile("^r\\.-?\\d+\\.-?\\d+\\.mca$");
    private static final int INVALID_REGION_COORD = Integer.MIN_VALUE;

    public final int dimensionId;
    public final String dimensionName;

    public DimensionAnalysis(int dimensionId) {
        this.dimensionId = dimensionId;
        this.dimensionName = getDimensionName();
    }

    private interface IChunkHandler {

        void processChunk(PartiallyLoadedChunk chunk, int chunkX, int chunkZ);
    }

    public void processMinecraftWorld(Collection<File> regionFiles) {
        if (regionFiles.isEmpty()) return;

        AnalysisProgressTracker.announceDimension(dimensionId);

        // Group region files by rows, skipping any with malformed names
        final Int2ObjectAVLTreeMap<List<File>> regionFilesByRow = new Int2ObjectAVLTreeMap<>();
        int malformedRegionFileCount = 0;
        for (File regionFile : regionFiles) {
            final int regionZ = parseRegionZ(regionFile);
            if (regionZ == INVALID_REGION_COORD) {
                VP.LOG.warn("Invalid region file found! {} continuing", regionFile.getAbsolutePath());
                malformedRegionFileCount++;
            } else {
                regionFilesByRow.computeIfAbsent(regionZ, value -> new ArrayList<>()).add(regionFile);
            }
        }

        final long maxRowSizeMBForHolding = Config.maxRegionRowFileMBForInMemoryScan;
        int totalRegionFilePasses = regionFiles.size() - malformedRegionFileCount;
        for (List<File> row : regionFilesByRow.values()) {
            if (rowSizeMB(row) > maxRowSizeMBForHolding) {
                totalRegionFilePasses += row.size();
            }
        }
        AnalysisProgressTracker.setNumberOfRegionFiles(totalRegionFilePasses);

        final Long2IntOpenHashMap veinBlockY = new Long2IntOpenHashMap();

        final int[] rows = regionFilesByRow.keySet().toIntArray();
        final Int2ObjectOpenHashMap<RegionRowResult> heldRows = new Int2ObjectOpenHashMap<>();
        int rowToFinalize = 0;
        for (int i = 0; i < rows.length; i++) {
            final int regionZ = rows[i];
            heldRows.put(regionZ, fastPassRow(regionFilesByRow.get(regionZ), veinBlockY, maxRowSizeMBForHolding));

            // Once both below and above rows are processed it's safe to finalize the middle one.
            while (rowToFinalize < i && rows[rowToFinalize] + 1 <= regionZ) {
                finalizeRow(heldRows.remove(rows[rowToFinalize]), veinBlockY);
                rowToFinalize++;
            }
        }
        while (rowToFinalize < rows.length) {
            finalizeRow(heldRows.remove(rows[rowToFinalize]), veinBlockY);
            rowToFinalize++;
        }
    }

    // Fast-pass one region row. write single vein match, hold on ambiguous matches for later.
    private RegionRowResult fastPassRow(List<File> rowRegionFiles, Long2IntMap veinBlockY,
            long maxRowSizeMBForHolding) {
        final boolean holdInMemory = rowSizeMB(rowRegionFiles) <= maxRowSizeMBForHolding;
        final Queue<PendingOreVein> singleMatches = new ConcurrentLinkedQueue<>();
        final Queue<DetailedChunkAnalysis> ambiguousChunks = holdInMemory ? new ConcurrentLinkedQueue<>() : null;

        rowRegionFiles.parallelStream()
                .forEach(regionFile -> executeForEachGeneratedOreChunk(regionFile, (chunk, chunkX, chunkZ) -> {
                    final ChunkAnalysis analysis = new ChunkAnalysis(dimensionName);
                    analysis.processMinecraftChunk(chunk);

                    if (analysis.matchesSingleVein()) {
                        final VeinType veinType = analysis.getMatchedVein();
                        if (veinType != VeinType.NO_VEIN) {
                            singleMatches.add(new PendingOreVein(chunkX, chunkZ, veinType, analysis.getVeinBlockY()));
                        }
                    } else if (holdInMemory) {
                        final DetailedChunkAnalysis detailedChunk = new DetailedChunkAnalysis(
                                dimensionId,
                                dimensionName,
                                chunkX,
                                chunkZ);
                        detailedChunk.processMinecraftChunk(chunk);
                        ambiguousChunks.add(detailedChunk);
                    }
                }));

        for (PendingOreVein singleMatch : singleMatches) {
            ServerCache.instance.notifyOreVeinGeneration(
                    dimensionId,
                    singleMatch.chunkX,
                    singleMatch.chunkZ,
                    singleMatch.veinType,
                    VeinSource.RESCAN);
            veinBlockY.put(Utils.chunkCoordsToKey(singleMatch.chunkX, singleMatch.chunkZ), singleMatch.veinBlockY);
        }

        return holdInMemory ? RegionRowResult.held(ambiguousChunks) : RegionRowResult.reRead(rowRegionFiles);
    }

    // Resolve a row's overlapping chunks against their neighbours
    private void finalizeRow(RegionRowResult row, Long2IntMap veinBlockY) {
        if (row == null) return;

        if (row.heldAmbiguousChunks != null) {
            row.heldAmbiguousChunks.parallelStream().forEach(detailedChunk -> detailedChunk.resolve(veinBlockY));
            for (DetailedChunkAnalysis detailedChunk : row.heldAmbiguousChunks) {
                ServerCache.instance.notifyOreVeinGeneration(
                        dimensionId,
                        detailedChunk.chunkX,
                        detailedChunk.chunkZ,
                        detailedChunk.getResolvedVeinType(),
                        VeinSource.RESCAN);
            }
        } else {
            final Queue<PendingOreVein> resolved = new ConcurrentLinkedQueue<>();
            row.rowRegionFiles.parallelStream()
                    .forEach(regionFile -> executeForEachGeneratedOreChunk(regionFile, (chunk, chunkX, chunkZ) -> {
                        if (ServerCache.instance.getOreVein(dimensionId, chunkX, chunkZ).veinType == VeinType.NO_VEIN) {
                            final DetailedChunkAnalysis detailedChunk = new DetailedChunkAnalysis(
                                    dimensionId,
                                    dimensionName,
                                    chunkX,
                                    chunkZ);
                            detailedChunk.processMinecraftChunk(chunk);
                            detailedChunk.resolve(veinBlockY);
                            if (detailedChunk.getResolvedVeinType() != VeinType.NO_VEIN) {
                                resolved.add(
                                        new PendingOreVein(chunkX, chunkZ, detailedChunk.getResolvedVeinType(), 0));
                            }
                        }
                    }));
            for (PendingOreVein result : resolved) {
                ServerCache.instance.notifyOreVeinGeneration(
                        dimensionId,
                        result.chunkX,
                        result.chunkZ,
                        result.veinType,
                        VeinSource.RESCAN);
            }
        }
    }

    private static int parseRegionZ(File regionFile) {
        final Matcher matcher = REGION_FILE_NAME.matcher(regionFile.getName());
        if (!matcher.matches()) return INVALID_REGION_COORD;
        try {
            return Integer.parseInt(regionFile.getName().split("\\.")[2]);
        } catch (NumberFormatException ignored) {
            return INVALID_REGION_COORD;
        }
    }

    private static long rowSizeMB(List<File> regionFiles) {
        return regionFiles.stream().mapToLong(File::length).sum() >> 20;
    }

    @Desugar
    private record RegionRowResult(Collection<DetailedChunkAnalysis> heldAmbiguousChunks, List<File> rowRegionFiles) {

        static RegionRowResult held(Collection<DetailedChunkAnalysis> heldAmbiguousChunks) {
            return new RegionRowResult(heldAmbiguousChunks, null);
        }

        static RegionRowResult reRead(List<File> rowRegionFiles) {
            return new RegionRowResult(null, rowRegionFiles);
        }
    }

    @Desugar
    private record PendingOreVein(int chunkX, int chunkZ, VeinType veinType, int veinBlockY) {

    }

    private void executeForEachGeneratedOreChunk(File regionFile, IChunkHandler chunkHandler) {
        try {
            final String[] parts = regionFile.getName().split("\\.");
            final int regionChunkX = Integer.parseInt(parts[1]) << 5;
            final int regionChunkZ = Integer.parseInt(parts[2]) << 5;
            try (RegionReader region = new RegionReader(regionFile)) {
                for (int localChunkX = 0; localChunkX < VP.chunksPerRegionFileX; localChunkX++) {
                    for (int localChunkZ = 0; localChunkZ < VP.chunksPerRegionFileZ; localChunkZ++) {
                        final int chunkX = regionChunkX + localChunkX;
                        final int chunkZ = regionChunkZ + localChunkZ;

                        // Only process ore chunks
                        if (chunkX != Utils.mapToCenterOreChunkCoord(chunkX)) continue;
                        if (chunkZ != Utils.mapToCenterOreChunkCoord(chunkZ)) continue;

                        // Skip chunks already cached with higher trustLevel than VeinSource.RESCAN
                        if (!VeinSource.RESCAN.canOverwrite(
                                ServerCache.instance.getOreVein(dimensionId, chunkX, chunkZ).getSource())) {
                            continue;
                        }

                        NBTTagCompound chunk = region.getChunk(localChunkX, localChunkZ);

                        if (chunk == null) continue;

                        PartiallyLoadedChunk loadedChunk = new PartiallyLoadedChunk();
                        loadedChunk.load(chunk, chunkX, chunkZ);

                        chunkHandler.processChunk(loadedChunk, chunkX, chunkZ);
                    }
                }
            }
            AnalysisProgressTracker.regionFileProcessed();
        } catch (DataFormatException | IOException e) {
            AnalysisProgressTracker.notifyCorruptFile(regionFile);
        }
    }

    // Resolves the dimension name which will help filter ore veins
    private String getDimensionName() {
        final WorldServer world = DimensionManager.getWorld(dimensionId);
        WorldProvider provider = world != null ? world.provider : null;
        if (provider == null) {
            try {
                provider = DimensionManager.createProviderFor(dimensionId);
            } catch (Throwable t) {
                VP.LOG.warn("Could not resolve a provider for dimensionId={}: {}", dimensionId, t);
                return "";
            }
        }

        final String celestialName = Mods.GalacticraftCore.isModLoaded() ? getCelestialBodyName(provider) : null;
        if (celestialName != null) return celestialName;

        final String name = provider.getDimensionName();
        return name == null ? "" : name;
    }

    @Optional.Method(modid = Mods.ModIDs.GALACTICRAFT_CORE)
    private static String getCelestialBodyName(WorldProvider provider) {
        if (provider instanceof WorldProviderSpace space) {
            final CelestialBody body = space.getCelestialBody();
            return body == null ? null : body.getName();
        }
        return null;
    }
}
