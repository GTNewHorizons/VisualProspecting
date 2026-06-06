package com.sinthoras.visualprospecting.database.cachebuilder;

import java.util.Set;
import java.util.stream.IntStream;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import gregtech.api.interfaces.IOreMaterial;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

// Slower, but more sophisticated approach to identify overlapping veins
public class DetailedChunkAnalysis {

    private final int dimensionId;
    public final int chunkX;
    public final int chunkZ;
    // For each height we count how often a ore (short) has occured
    private final Reference2IntOpenHashMap<IOreMaterial>[] oresPerY = new Reference2IntOpenHashMap[VP.minecraftWorldHeight];
    private final String dimName;
    private VeinType resolvedVeinType = VeinType.NO_VEIN;

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

    // Resolve chunk vein against its neighbours and cache the result.
    public void resolve(final Long2IntMap veinChunkY) {
        cleanUpWithNeighbors(veinChunkY);
        resolvedVeinType = getMatchedVein();
    }

    public VeinType getResolvedVeinType() {
        return resolvedVeinType;
    }

    private void cleanUpWithNeighbors(final Long2IntMap veinChunkY) {
        // Remove ores that spilled over from neighbouring veins.
        for (int[] neighborOffset : NEIGHBOR_OFFSETS) {
            final int neighborChunkX = chunkX + neighborOffset[0];
            final int neighborChunkZ = chunkZ + neighborOffset[1];

            final long neighborKey = Utils.chunkCoordsToKey(
                    Utils.mapToCenterOreChunkCoord(neighborChunkX),
                    Utils.mapToCenterOreChunkCoord(neighborChunkZ));
            if (!veinChunkY.containsKey(neighborKey)) continue;
            final int neighborVeinBlockY = veinChunkY.get(neighborKey);

            final OreVeinPosition neighbor = ServerCache.instance
                    .getOreVein(dimensionId, neighborChunkX, neighborChunkZ);
            if (neighbor.veinType == VeinType.NO_VEIN) continue;

            final boolean atCoordinateAxis = Math.abs(neighbor.chunkX - chunkX) < 3
                    || Math.abs(neighbor.chunkZ - chunkZ) < 3;
            final boolean canOverlap = atCoordinateAxis
                    ? neighbor.veinType.canOverlapIntoNeighborOreChunkAtCoordinateAxis()
                    : neighbor.veinType.canOverlapIntoNeighborOreChunk();
            if (!canOverlap) continue;

            for (int layerBlockY = 0; layerBlockY < VeinType.veinHeight; layerBlockY++) {
                final int blockY = neighborVeinBlockY + layerBlockY;
                if (blockY > 255) break;

                if (oresPerY[blockY] != null) {
                    for (IOreMaterial ore : neighbor.veinType.getOresAtLayer(layerBlockY)) {
                        oresPerY[blockY].removeInt(ore);
                    }
                }
            }
        }
    }

    private VeinType getMatchedVein() {
        final ObjectSet<VeinType> matchedVeins = new ObjectOpenHashSet<>();

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
        }
        if (matchedVeins.size() >= 2) {
            matchedVeins.removeIf(
                    veinType -> IntStream.range(veinType.minBlockY, veinType.maxBlockY)
                            .noneMatch(blockY -> isOreVeinGeneratedAtHeight(veinType, blockY)));
            return matchedVeins.size() == 1 ? matchedVeins.iterator().next() : VeinType.NO_VEIN;
        }
        return matchIgnoringSporadic(allOres.keySet(), dominantOre);
    }

    private VeinType matchIgnoringSporadic(Set<IOreMaterial> foundOres, IOreMaterial dominantOre) {
        VeinType onlyMatch = VeinType.NO_VEIN;
        for (VeinType veinType : VeinTypeCaching.getVeinTypesForOre(dominantOre)) {
            if (veinType.matchesIgnoringSporadic(foundOres, dimName, dominantOre)) {
                if (onlyMatch != VeinType.NO_VEIN) {
                    return VeinType.NO_VEIN;
                }
                onlyMatch = veinType;
            }
        }
        return onlyMatch;
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
