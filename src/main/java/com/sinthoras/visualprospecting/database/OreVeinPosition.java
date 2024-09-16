package com.sinthoras.visualprospecting.database;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;

public class OreVeinPosition {

    public static final int MAX_BYTES = 3 * Integer.BYTES + Short.BYTES;
    public static final OreVeinPosition EMPTY_VEIN = new OreVeinPosition(0, 0, 0, VeinType.NO_VEIN, true);

    public final int dimensionId;
    public final int chunkX;
    public final int chunkZ;
    public final VeinType veinType;

    private boolean depleted = false;

    public OreVeinPosition(int dimensionId, int chunkX, int chunkZ, VeinType veinType) {
        this.dimensionId = dimensionId;
        this.chunkX = Utils.mapToCenterOreChunkCoord(chunkX);
        this.chunkZ = Utils.mapToCenterOreChunkCoord(chunkZ);
        this.veinType = veinType;
    }

    public OreVeinPosition(int dimensionId, int chunkX, int chunkZ, VeinType veinType, boolean depleted) {
        this.dimensionId = dimensionId;
        this.chunkX = Utils.mapToCenterOreChunkCoord(chunkX);
        this.chunkZ = Utils.mapToCenterOreChunkCoord(chunkZ);
        this.veinType = veinType;
        this.depleted = depleted;
    }

    public int getBlockX() {
        return Utils.coordChunkToBlock(chunkX) + 8;
    }

    public int getBlockZ() {
        return Utils.coordChunkToBlock(chunkZ) + 8;
    }

    public boolean isDepleted() {
        return depleted;
    }

    public void toggleDepleted() {
        depleted = !depleted;
    }

    public OreVeinPosition joinDepletedState(final OreVeinPosition other) {
        depleted = depleted || other.depleted;
        return this;
    }

    // Leaving this here for compatability sake
    public static int getMaxBytes() {
        return MAX_BYTES;
    }
}
