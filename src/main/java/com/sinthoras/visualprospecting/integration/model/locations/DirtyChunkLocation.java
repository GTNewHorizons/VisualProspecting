package com.sinthoras.visualprospecting.integration.model.locations;

import com.sinthoras.visualprospecting.Utils;

public class DirtyChunkLocation implements ILocationProvider {

    private final int blockX;
    private final int blockZ;
    private final int dimensionId;
    private final boolean dirty;

    public DirtyChunkLocation(int chunkX, int chunkZ, int dimensionId, boolean dirty) {
        blockX = Utils.coordChunkToBlock(chunkX);
        blockZ = Utils.coordChunkToBlock(chunkZ);
        this.dimensionId = dimensionId;
        this.dirty = dirty;
    }

    public double getBlockX() {
        return blockX + 0.5;
    }

    public double getBlockZ() {
        return blockZ + 0.5;
    }

    public int getDimensionId() {
        return dimensionId;
    }

    public boolean isDirty() {
        return dirty;
    }
}
