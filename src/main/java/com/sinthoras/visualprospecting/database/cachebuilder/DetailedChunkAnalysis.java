package com.sinthoras.visualprospecting.database.cachebuilder;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
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
        final OreVeinPosition[] neighbors = new OreVeinPosition[] {
                ServerCache.instance.getOreVein(dimensionId, chunkX - 3, chunkZ + 3),
                ServerCache.instance.getOreVein(dimensionId, chunkX, chunkZ + 3),
                ServerCache.instance.getOreVein(dimensionId, chunkX + 3, chunkZ + 3),
                ServerCache.instance.getOreVein(dimensionId, chunkX + 3, chunkZ),
                ServerCache.instance.getOreVein(dimensionId, chunkX + 3, chunkZ - 3),
                ServerCache.instance.getOreVein(dimensionId, chunkX, chunkZ - 3),
                ServerCache.instance.getOreVein(dimensionId, chunkX - 3, chunkZ - 3),
                ServerCache.instance.getOreVein(dimensionId, chunkX - 3, chunkZ) };
        final int[] neighborVeinBlockY = new int[] {
                veinChunkY.getOrDefault(
                        Utils.chunkCoordsToKey(
                                Utils.mapToCenterOreChunkCoord(chunkX - 3),
                                Utils.mapToCenterOreChunkCoord(chunkZ + 3)),
                        0),
                veinChunkY.getOrDefault(
                        Utils.chunkCoordsToKey(
                                Utils.mapToCenterOreChunkCoord(chunkX),
                                Utils.mapToCenterOreChunkCoord(chunkZ + 3)),
                        0),
                veinChunkY.getOrDefault(
                        Utils.chunkCoordsToKey(
                                Utils.mapToCenterOreChunkCoord(chunkX + 3),
                                Utils.mapToCenterOreChunkCoord(chunkZ + 3)),
                        0),
                veinChunkY.getOrDefault(
                        Utils.chunkCoordsToKey(
                                Utils.mapToCenterOreChunkCoord(chunkX + 3),
                                Utils.mapToCenterOreChunkCoord(chunkZ)),
                        0),
                veinChunkY.getOrDefault(
                        Utils.chunkCoordsToKey(
                                Utils.mapToCenterOreChunkCoord(chunkX + 3),
                                Utils.mapToCenterOreChunkCoord(chunkZ - 3)),
                        0),
                veinChunkY.getOrDefault(
                        Utils.chunkCoordsToKey(
                                Utils.mapToCenterOreChunkCoord(chunkX),
                                Utils.mapToCenterOreChunkCoord(chunkZ - 3)),
                        0),
                veinChunkY.getOrDefault(
                        Utils.chunkCoordsToKey(
                                Utils.mapToCenterOreChunkCoord(chunkX - 3),
                                Utils.mapToCenterOreChunkCoord(chunkZ - 3)),
                        0),
                veinChunkY.getOrDefault(
                        Utils.chunkCoordsToKey(
                                Utils.mapToCenterOreChunkCoord(chunkX - 3),
                                Utils.mapToCenterOreChunkCoord(chunkZ)),
                        0) };

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

        // spotless:off
        final Optional<IOreMaterial> dominantOre = allOres.reference2IntEntrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .map(Map.Entry::getKey)
            .findFirst();
        // spotless:on

        if (!dominantOre.isPresent()) return VeinType.NO_VEIN;

        for (VeinType veinType : VeinTypeCaching.getVeinTypes()) {
            if (veinType.matchesWithSpecificPrimaryOrSecondary(allOres.keySet(), dimName, dominantOre.get())) {
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
