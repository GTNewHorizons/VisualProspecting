package com.sinthoras.visualprospecting.database;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;

public abstract class WorldCache {

    protected final Map<Integer, DimensionCache> dimensions = new HashMap<>();
    private boolean needsSaving = false;
    protected File oreVeinCacheDirectory;
    protected File undergroundFluidCacheDirectory;
    private boolean isLoaded = false;

    protected abstract File getStorageDirectory();

    public boolean loadVeinCache(String worldId) {

        // Don't load if it's already loaded
        if (isLoaded) {
            return true;
        }

        // We're gonna load it, set now
        isLoaded = true;

        // Get the cache dirs
        final File worldCacheDirectory = new File(getStorageDirectory(), worldId);
        oreVeinCacheDirectory = new File(worldCacheDirectory, Tags.OREVEIN_DIR);
        undergroundFluidCacheDirectory = new File(worldCacheDirectory, Tags.UNDERGROUNDFLUID_DIR);
        oreVeinCacheDirectory.mkdirs();
        undergroundFluidCacheDirectory.mkdirs();

        // Also get the relevant buffers
        final Map<Integer, ByteBuffer> oreVeinDimensionBuffers = Utils.getDIMFiles(oreVeinCacheDirectory);
        final Map<Integer, ByteBuffer> undergroundFluidDimensionBuffers = Utils
                .getDIMFiles(undergroundFluidCacheDirectory);
        final Set<Integer> dimensionsIds = new HashSet<>();
        dimensionsIds.addAll(oreVeinDimensionBuffers.keySet());
        dimensionsIds.addAll(undergroundFluidDimensionBuffers.keySet());

        // Can't load an empty cache
        if (dimensionsIds.isEmpty()) {
            return false;
        }

        // For every dim...
        for (int dimensionId : dimensionsIds) {

            // Load it
            final DimensionCache dimension = new DimensionCache(dimensionId);
            dimension.loadCache(
                    oreVeinDimensionBuffers.get(dimensionId),
                    undergroundFluidDimensionBuffers.get(dimensionId));
            dimensions.put(dimensionId, dimension);
        }

        return true;
    }

    public void saveVeinCache() {
        if (needsSaving) {
            for (DimensionCache dimension : dimensions.values()) {
                final ByteBuffer oreVeinBuffer = dimension.saveOreChunks();
                if (oreVeinBuffer != null) {
                    Utils.appendToFile(
                            new File(oreVeinCacheDirectory.toPath() + "/DIM" + dimension.dimensionId),
                            oreVeinBuffer);
                }
                final ByteBuffer undergroundFluidBuffer = dimension.saveUndergroundFluids();
                if (undergroundFluidBuffer != null) {
                    Utils.appendToFile(
                            new File(undergroundFluidCacheDirectory.toPath() + "/DIM" + dimension.dimensionId),
                            undergroundFluidBuffer);
                }
            }
            needsSaving = false;
        }
    }

    public void reset() {

        dimensions.clear();
        needsSaving = false;
        isLoaded = false;
    }

    /**
     * Reset some chunks. Not all, and (usually) not none - but some. Input coords are in chunk coordinates, NOT block
     * coords. WARNING: This is a PERMANENT delete, not just a temporary clear. It will set needsSaving to true!
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
        }
        needsSaving = true;
        isLoaded = false;
    }

    private DimensionCache.UpdateResult updateSaveFlag(DimensionCache.UpdateResult updateResult) {
        needsSaving |= updateResult != DimensionCache.UpdateResult.AlreadyKnown;
        return updateResult;
    }

    protected DimensionCache.UpdateResult putOreVein(final OreVeinPosition oreVeinPosition) {
        DimensionCache dimension = dimensions.get(oreVeinPosition.dimensionId);
        if (dimension == null) {
            dimension = new DimensionCache(oreVeinPosition.dimensionId);
            dimensions.put(oreVeinPosition.dimensionId, dimension);
        }
        return updateSaveFlag(dimension.putOreVein(oreVeinPosition));
    }

    protected void toggleOreVein(int dimensionId, int chunkX, int chunkZ) {
        DimensionCache dimension = dimensions.get(dimensionId);
        if (dimension != null) {
            dimension.toggleOreVein(chunkX, chunkZ);
        }
        needsSaving = true;
    }

    /**
     * Gets ore vein on the server. If the dimension is null, it just returns a NO_VEIN. If there is a dimension, it
     * calls the appropriate DimensionCache.
     * 
     * @param dimensionId The dimension ID; if null, returns NO_VEIN
     * @param chunkX
     * @param chunkZ
     * @return The ore vein at that location (or NO_VEIN)
     */
    public OreVeinPosition getOreVein(int dimensionId, int chunkX, int chunkZ) {
        DimensionCache dimension = dimensions.get(dimensionId);
        if (dimension == null) {
            return new OreVeinPosition(dimensionId, chunkX, chunkZ, VeinType.NO_VEIN, true);
        }
        return dimension.getOreVein(chunkX, chunkZ);
    }

    protected DimensionCache.UpdateResult putUndergroundFluids(final UndergroundFluidPosition undergroundFluid) {
        DimensionCache dimension = dimensions.get(undergroundFluid.dimensionId);
        if (dimension == null) {
            dimension = new DimensionCache(undergroundFluid.dimensionId);
            dimensions.put(undergroundFluid.dimensionId, dimension);
        }
        return updateSaveFlag(dimension.putUndergroundFluid(undergroundFluid));
    }

    public UndergroundFluidPosition getUndergroundFluid(int dimensionId, int chunkX, int chunkZ) {
        DimensionCache dimension = dimensions.get(dimensionId);
        if (dimension == null) {
            return UndergroundFluidPosition.getNotProspected(dimensionId, chunkX, chunkZ);
        }
        return dimension.getUndergroundFluid(chunkX, chunkZ);
    }
}
