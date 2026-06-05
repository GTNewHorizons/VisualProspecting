package com.sinthoras.visualprospecting.database.cachebuilder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import gregtech.api.interfaces.IOreMaterial;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

// Slower, but more sophisticated approach to identify overlapping veins
public class DetailedChunkAnalysis {

    private final int dimensionId;
    public final int chunkX;
    public final int chunkZ;
    // For each height we count how often a ore (short) has occured
    private final Reference2IntOpenHashMap<IOreMaterial>[] oresPerY = new Reference2IntOpenHashMap[VP.minecraftWorldHeight];
    private final String dimName;

    // Surrounding ore-chunk neighbours, to resolve overlap
    private static final int[][] NEIGHBOR_OFFSETS = { { -3, 3 }, { 0, 3 }, { 3, 3 }, { 3, 0 }, { 3, -3 }, { 0, -3 },
            { -3, -3 }, { -3, 0 } };

    public DetailedChunkAnalysis(int dimensionId, String dimName, int chunkX, int chunkZ) {
        this.dimensionId = dimensionId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.dimName = dimName;
    }

    public void processMinecraftChunk(final PartiallyLoadedChunk chunk) {
        chunk.forEachOre((x, y, z, info) -> {
            if (oresPerY[y] == null) {
                oresPerY[y] = new Reference2IntOpenHashMap<>();
            }

            oresPerY[y].addTo(info.material, 1);
        });
    }

    public void cleanUpWithNeighbors(final Map<Long, Integer> veinChunkY) {
        final OreVeinPosition[] neighbors = new OreVeinPosition[NEIGHBOR_OFFSETS.length];
        final int[] neighborVeinBlockY = new int[NEIGHBOR_OFFSETS.length];
        for (int neighborId = 0; neighborId < NEIGHBOR_OFFSETS.length; neighborId++) {
            final int neighborChunkX = chunkX + NEIGHBOR_OFFSETS[neighborId][0];
            final int neighborChunkZ = chunkZ + NEIGHBOR_OFFSETS[neighborId][1];
            neighbors[neighborId] = ServerCache.instance.getOreVein(dimensionId, neighborChunkX, neighborChunkZ);
            neighborVeinBlockY[neighborId] = veinChunkY.getOrDefault(
                    Utils.chunkCoordsToKey(
                            Utils.mapToCenterOreChunkCoord(neighborChunkX),
                            Utils.mapToCenterOreChunkCoord(neighborChunkZ)),
                    0);
        }

        // Remove all generated ores from neighbors. They could also be generated in the same chunk,
        // but that case is rare and therefore, neglected
        for (int neighborId = 0; neighborId < neighbors.length; neighborId++) {
            final OreVeinPosition neighbor = neighbors[neighborId];
            if (neighbor.veinType == VeinType.NO_VEIN) continue;

            final boolean atCoordinateAxis = Math.abs(neighbor.chunkX - chunkX) < 3
                    || Math.abs(neighbor.chunkZ - chunkZ) < 3;
            final boolean canOverlap = atCoordinateAxis
                    ? neighbor.veinType.canOverlapIntoNeighborOreChunkAtCoordinateAxis()
                    : neighbor.veinType.canOverlapIntoNeighborOreChunk();
            if (canOverlap) {
                final int veinBlockY = neighborVeinBlockY[neighborId];

                for (int layerBlockY = 0; layerBlockY < VeinType.veinHeight; layerBlockY++) {
                    final int blockY = veinBlockY + layerBlockY;

                    if (blockY > 255) {
                        break;
                    }

                    if (oresPerY[blockY] != null) {
                        for (IOreMaterial ore : neighbor.veinType.getOresAtLayer(layerBlockY)) {
                            oresPerY[blockY].removeInt(ore);
                        }
                    }
                }
            }
        }
    }

    public VeinType getMatchedVein() {
        final Set<VeinType> matchedVeins = new HashSet<>();

        final Reference2IntOpenHashMap<IOreMaterial> allOres = new Reference2IntOpenHashMap<>();

        for (Reference2IntOpenHashMap<IOreMaterial> oreLevel : oresPerY) {
            if (oreLevel == null || oreLevel.isEmpty()) continue;

            for (var entry : oreLevel.reference2IntEntrySet()) {
                allOres.addTo(entry.getKey(), entry.getIntValue());
            }
        }

        if (allOres.isEmpty()) return VeinType.NO_VEIN;

        IOreMaterial dominantOre = null;
        int highestOreCount = -1;
        for (var entry : allOres.reference2IntEntrySet()) {
            if (entry.getIntValue() > highestOreCount) {
                highestOreCount = entry.getIntValue();
                dominantOre = entry.getKey();
            }
        }

        if (dominantOre == null) return VeinType.NO_VEIN;

        for (VeinType veinType : VeinTypeCaching.getVeinTypesForOre(dominantOre)) {
            if (veinType.matchesWithSpecificPrimaryOrSecondary(allOres.keySet(), dimName, dominantOre)) {
                matchedVeins.add(veinType);
            }
        }

        if (matchedVeins.size() == 1) {
            return matchedVeins.iterator().next();
        } else if (matchedVeins.size() >= 2) {
            matchedVeins.removeIf(
                    veinType -> IntStream.range(veinType.minBlockY, veinType.maxBlockY)
                            .noneMatch(blockY -> isOreVeinGeneratedAtHeight(veinType, blockY)));

            if (matchedVeins.size() == 1) {
                return matchedVeins.iterator().next();
            }
        }
        return VeinType.NO_VEIN;
    }

    private boolean isOreVeinGeneratedAtHeight(VeinType veinType, int blockY) {
        for (int layer = 0; layer < VeinType.veinHeight; layer++) {
            if (oresPerY[blockY + layer] == null
                    || !oresPerY[blockY + layer].keySet().containsAll(veinType.getOresAtLayer(layer))) {
                return false;
            }
        }
        return true;
    }
}
