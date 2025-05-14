package com.sinthoras.visualprospecting.integration.model.layers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.LayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.layers.UniversalLayerRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.integration.model.buttons.UndergroundFluidButtonManager;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidLocation;
import com.sinthoras.visualprospecting.integration.model.render.UndergroundFluidRenderStep;

import gregtech.api.enums.UndergroundFluidNames;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class UndergroundFluidLayerManager extends LayerManager {

    public static final UndergroundFluidLayerManager instance = new UndergroundFluidLayerManager();
    private static final ObjectSet<Fluid> highlightedFluids = new ObjectOpenHashSet<>();

    public UndergroundFluidLayerManager() {
        super(UndergroundFluidButtonManager.instance);
        setHasSearchField(true);
    }

    @Nullable
    @Override
    protected LayerRenderer addLayerRenderer(LayerManager manager, SupportedMods mod) {
        return new UniversalLayerRenderer(manager)
                .withRenderStep(location -> new UndergroundFluidRenderStep((UndergroundFluidLocation) location));
    }

    @Override
    public void onOpenMap() {
        highlightedFluids.clear();
        computeSearch(Utils.getNEISearchPattern());
    }

    private static List<String> getFluidNames(Fluid fluid) {
        List<String> names = new ArrayList<>();
        names.add(fluid.getLocalizedName());
        names.add(fluid.getUnlocalizedName());
        names.add(fluid.getName());
        return names;
    }

    @Override
    public void updateElement(ILocationProvider location) {
        UndergroundFluidLocation fluidLocation = (UndergroundFluidLocation) location;
        fluidLocation.setActive(true);
        if (isSearchActive()) {
            boolean highlighted = highlightedFluids.contains(fluidLocation.getFluid())
                    && fluidLocation.getMaxProduction() > 0;
            fluidLocation.setActive(highlighted);
        }
    }

    @Override
    protected ILocationProvider generateLocation(int chunkX, int chunkZ, int dim) {
        if (chunkX % VP.undergroundFluidSizeChunkX != 0 || chunkZ % VP.undergroundFluidSizeChunkZ != 0) {
            return null;
        }

        UndergroundFluidPosition undergroundFluid = ClientCache.instance.getUndergroundFluid(dim, chunkX, chunkZ);
        if (undergroundFluid.isProspected()) {
            return new UndergroundFluidLocation(undergroundFluid);
        }

        return null;
    }

    private void computeSearch(@Nullable Pattern filterPattern) {
        if (filterPattern != null) {
            for (UndergroundFluidNames fluidName : UndergroundFluidNames.values()) {
                Fluid fluid = FluidRegistry.getFluid(fluidName.name);
                if (fluid == null) continue;
                for (String name : getFluidNames(fluid)) {
                    if (name != null && filterPattern.matcher(name.toLowerCase()).find()) {
                        highlightedFluids.add(fluid);
                    }
                }
            }
        }
    }

    @Override
    public void onSearch(@NotNull String searchString) {
        if (searchString.isEmpty()) {
            highlightedFluids.clear();
        } else {
            computeSearch(Utils.getSearchPattern(searchString));
        }
    }

    @Override
    public int getElementSize() {
        return 8;
    }

    public boolean isSearchActive() {
        return !highlightedFluids.isEmpty();
    }
}
