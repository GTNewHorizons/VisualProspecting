package com.sinthoras.visualprospecting.integration.model.locations;

import net.minecraftforge.fluids.Fluid;

import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;

public class UndergroundFluidChunkLocation implements ILocationProvider {

    private final int blockX;
    private final int blockZ;
    private final int dimensionId;
    private final int offsetX;
    private final int offsetZ;
    private final UndergroundFluidPosition undergroundFluid;

    public UndergroundFluidChunkLocation(int chunkX, int chunkZ, int dimensionId, UndergroundFluidPosition fluid,
            int offsetX, int offsetZ) {
        blockX = Utils.coordChunkToBlock(chunkX + offsetX);
        blockZ = Utils.coordChunkToBlock(chunkZ + offsetZ);
        this.dimensionId = dimensionId;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
        this.undergroundFluid = fluid;
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

    public String getFluidAmountFormatted() {
        if (getFluidAmount() >= 1000) {
            return (getFluidAmount() / 1000) + "kL";
        }
        return getFluidAmount() + "L";
    }

    public int getFluidAmount() {
        return undergroundFluid.chunks[offsetX][offsetZ];
    }

    public Fluid getFluid() {
        return undergroundFluid.fluid;
    }

    public int getMaxAmountInField() {
        return undergroundFluid.getMaxProduction();
    }

    public int getMinAmountInField() {
        return undergroundFluid.getMinProduction();
    }
}
