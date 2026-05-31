package com.sinthoras.visualprospecting.integration.model.locations;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;

import gregtech.api.enums.UndergroundFluidNames;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

public class UndergroundFluidLocation implements ILocationProvider {

    private static final int NO_OVERRIDE = Integer.MIN_VALUE;
    private static Reference2IntMap<Fluid> renderColorOverrides;

    private final UndergroundFluidPosition undergroundFluidPosition;
    private final int color;
    private boolean active;

    public UndergroundFluidLocation(UndergroundFluidPosition undergroundFluidPosition) {
        this.undergroundFluidPosition = undergroundFluidPosition;
        this.color = resolveColor(undergroundFluidPosition.fluid);
    }

    public static int resolveColor(Fluid fluid) {
        if (fluid == null) return 0xFFFFFF;
        if (renderColorOverrides == null) {
            Reference2IntOpenHashMap<Fluid> map = new Reference2IntOpenHashMap<>();
            map.defaultReturnValue(NO_OVERRIDE);
            for (UndergroundFluidNames entry : UndergroundFluidNames.values()) {
                if (entry.renderColor == null) continue;
                Fluid f = FluidRegistry.getFluid(entry.name);
                if (f == null) continue;
                int packed = ((entry.renderColor[0] & 0xFF) << 16) | ((entry.renderColor[1] & 0xFF) << 8)
                        | (entry.renderColor[2] & 0xFF);
                map.put(f, packed);
            }
            renderColorOverrides = map;
        }
        int override = renderColorOverrides.getInt(fluid);
        return override != NO_OVERRIDE ? override : fluid.getColor();
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

    public int getColor() {
        return color;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
