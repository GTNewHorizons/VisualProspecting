package com.sinthoras.visualprospecting.integration.model.locations;

import net.minecraftforge.fluids.Fluid;

import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;

public class UndergroundFluidLocation implements ILocationProvider {

    private final UndergroundFluidPosition undergroundFluidPosition;
    private boolean active;

    public UndergroundFluidLocation(UndergroundFluidPosition undergroundFluidPosition) {
        this.undergroundFluidPosition = undergroundFluidPosition;
    }

    @Override
    public int getDimensionId() {
        return undergroundFluidPosition.dimensionId;
    }

    @Override
    public double getBlockX() {
        return undergroundFluidPosition.getBlockX() + 0.5;
    }

    @Override
    public double getBlockZ() {
        return undergroundFluidPosition.getBlockZ() + 0.5;
    }

    public int getMinProduction() {
        return undergroundFluidPosition.getMinProduction();
    }

    public int getMaxProduction() {
        return undergroundFluidPosition.getMaxProduction();
    }

    public int[][] getChunks() {
        return undergroundFluidPosition.chunks;
    }

    public Fluid getFluid() {
        return undergroundFluidPosition.fluid;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
