package com.sinthoras.visualprospecting.database;

import java.util.Arrays;

import net.minecraftforge.fluids.Fluid;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;

public class UndergroundFluidPosition {

    public static final int BYTES = (3 + 1 + VP.undergroundFluidSizeChunkX * VP.undergroundFluidSizeChunkZ)
            * Integer.BYTES;
    public static final UndergroundFluidPosition NOT_PROSPECTED = new UndergroundFluidPosition(0, 0, 0, null, null);

    public final int dimensionId;
    public final int chunkX;
    public final int chunkZ;
    public final Fluid fluid;
    public final int[][] chunks;
    private final int minProduction;
    private final int maxProduction;

    public UndergroundFluidPosition(int dimensionId, int chunkX, int chunkZ, Fluid fluid, int[][] chunks) {
        this.dimensionId = dimensionId;
        this.chunkX = Utils.mapToCornerUndergroundFluidChunkCoord(chunkX);
        this.chunkZ = Utils.mapToCornerUndergroundFluidChunkCoord(chunkZ);
        this.fluid = fluid;
        this.chunks = chunks;

        int smallest = Integer.MAX_VALUE;
        int largest = Integer.MIN_VALUE;
        if (chunks != null) {
            for (int cx = 0; cx < VP.undergroundFluidSizeChunkX; cx++) {
                for (int cz = 0; cz < VP.undergroundFluidSizeChunkZ; cz++) {
                    int amount = chunks[cx][cz];
                    if (amount < smallest) smallest = amount;
                    if (amount > largest) largest = amount;
                }
            }
        } else {
            smallest = 0;
            largest = 0;
        }
        this.minProduction = smallest;
        this.maxProduction = largest;
    }

    public int getBlockX() {
        return Utils.coordChunkToBlock(chunkX);
    }

    public int getBlockZ() {
        return Utils.coordChunkToBlock(chunkZ);
    }

    public int getMinProduction() {
        return minProduction;
    }

    public int getMaxProduction() {
        return maxProduction;
    }

    public boolean isProspected() {
        return fluid != null;
    }

    public boolean equals(UndergroundFluidPosition other) {
        return dimensionId == other.dimensionId && chunkX == other.chunkX
                && chunkZ == other.chunkZ
                && fluid == other.fluid
                && Arrays.deepEquals(chunks, other.chunks);
    }
}
