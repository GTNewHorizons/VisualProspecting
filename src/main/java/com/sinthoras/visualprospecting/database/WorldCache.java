package com.sinthoras.visualprospecting.database;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;

import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public abstract class WorldCache {

    protected final Int2ObjectMap<DimensionCache> dimensions = new Int2ObjectOpenHashMap<>();
    protected File worldCache;
    private boolean isLoaded = false;

    protected abstract File getStorageDirectory();

    public boolean loadVeinCache(String worldId) {
        if (isLoaded) return true;
        isLoaded = true;
        worldCache = new File(getStorageDirectory(), worldId);

        if (loadLegacyVeinCache(worldCache)) return true;

        final File[] dimensionFiles = worldCache.listFiles();
        if (dimensionFiles == null || dimensionFiles.length == 0) return false;

        boolean loadedAny = false;
        for (File dimensionFile : dimensionFiles) {
            final String fileName = dimensionFile.getName();
            if (!dimensionFile.isFile() || !fileName.endsWith(".dat")) {
                continue;
            }

            final NBTTagCompound dimCompound = Utils.readNBT(dimensionFile);
            if (dimCompound == null) continue;

            final int dimensionId = dimCompound.getInteger("dim");
            final DimensionCache dimension = dimensions.computeIfAbsent(dimensionId, DimensionCache::new);
            dimension.loadFromNbt(dimCompound);
            loadedAny = true;
        }

        return loadedAny;
    }

    private boolean loadLegacyVeinCache(File worldCacheDirectory) {
        File oreVeinCacheDirectory = new File(worldCacheDirectory, Tags.OREVEIN_DIR);
        File undergroundFluidCacheDirectory = new File(worldCacheDirectory, Tags.UNDERGROUNDFLUID_DIR);

        if (!oreVeinCacheDirectory.exists() && !undergroundFluidCacheDirectory.exists()) {
            return false;
        }

        final Map<Integer, ByteBuffer> oreVeinDimensionBuffers = Utils.getLegacyDimFiles(oreVeinCacheDirectory);
        final Map<Integer, ByteBuffer> undergroundFluidDimensionBuffers = Utils
                .getLegacyDimFiles(undergroundFluidCacheDirectory);
        final Set<Integer> dimensionsIds = new HashSet<>();
        dimensionsIds.addAll(oreVeinDimensionBuffers.keySet());
        dimensionsIds.addAll(undergroundFluidDimensionBuffers.keySet());
        dimensionsIds.addAll(dimensions.keySet());
        if (dimensionsIds.isEmpty()) {
            return false;
        }

        for (int dimensionId : dimensionsIds) {
            DimensionCache dimension = dimensions.get(dimensionId);
            if (dimension == null) {
                dimension = new DimensionCache(dimensionId);
            }
            dimension.loadLegacy(
                    oreVeinDimensionBuffers.get(dimensionId),
                    undergroundFluidDimensionBuffers.get(dimensionId));
            dimension.markDirty();
            dimensions.put(dimensionId, dimension);
        }

        Utils.deleteDirectoryRecursively(oreVeinCacheDirectory);
        Utils.deleteDirectoryRecursively(undergroundFluidCacheDirectory);
        saveVeinCache();
        return true;
    }

    public void saveVeinCache() {
        for (DimensionCache dimension : dimensions.values()) {
            if (!dimension.isDirty()) continue;
            File dimFile = new File(worldCache, "DIM" + dimension.dimensionId + ".dat");
            Utils.writeNBT(dimFile, dimension.saveToNbt());
        }
    }

    public void reset() {
        dimensions.clear();
        isLoaded = false;
    }

    /**
     * Reset some chunks. Not all, and (usually) not none - but some. Input coords are in chunk coordinates, NOT block
     * coords.
     *
     * @param dimID  The dimension ID.
     * @param startX The X coord of the starting chunk. Must be less than endX.
     * @param startZ The Z coord of the starting chunk. Must be less than endZ.
     * @param endX   The X coord of the ending chunk.
     * @param endZ   The Z coord of the ending chunk.
     */
    public void resetSome(int dimID, int startX, int startZ, int endX, int endZ) {

        DimensionCache dim = dimensions.get(dimID);
        if (dim != null) {
            dim.clearOreVeins(startX, startZ, endX, endZ);
            isLoaded = false;
        }
    }

    public void resetSpawnChunks(ChunkCoordinates spawn, int dimID) {

        int spawnChunkX = Utils.coordBlockToChunk(spawn.posX);
        int spawnChunkZ = Utils.coordBlockToChunk(spawn.posZ);

        int spawnChunksRadius = 8;
        int startX = spawnChunkX - spawnChunksRadius;
        int startZ = spawnChunkZ - spawnChunksRadius;
        int endX = spawnChunkX + spawnChunksRadius;
        int endZ = spawnChunkZ + spawnChunksRadius;

        resetSome(dimID, startX, startZ, endX, endZ);
    }

    protected DimensionCache.UpdateResult putOreVein(final OreVeinPosition oreVeinPosition) {
        DimensionCache dimension = dimensions.computeIfAbsent(oreVeinPosition.dimensionId, DimensionCache::new);
        return dimension.putOreVein(oreVeinPosition);
    }

    protected void toggleOreVein(int dimensionId, int chunkX, int chunkZ) {
        DimensionCache dimension = dimensions.get(dimensionId);
        if (dimension != null) {
            dimension.toggleOreVein(chunkX, chunkZ);
        }
    }

    public OreVeinPosition getOreVein(int dimensionId, int chunkX, int chunkZ) {
        DimensionCache dimension = dimensions.get(dimensionId);
        if (dimension == null) {
            return OreVeinPosition.EMPTY_VEIN;
        }
        return dimension.getOreVein(chunkX, chunkZ);
    }

    protected DimensionCache.UpdateResult putUndergroundFluids(final UndergroundFluidPosition undergroundFluid) {
        DimensionCache dimension = dimensions.computeIfAbsent(undergroundFluid.dimensionId, DimensionCache::new);
        return dimension.putUndergroundFluid(undergroundFluid);
    }

    public UndergroundFluidPosition getUndergroundFluid(int dimensionId, int chunkX, int chunkZ) {
        DimensionCache dimension = dimensions.get(dimensionId);
        if (dimension == null) {
            return UndergroundFluidPosition.NOT_PROSPECTED;
        }
        return dimension.getUndergroundFluid(chunkX, chunkZ);
    }
}
