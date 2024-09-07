package com.sinthoras.visualprospecting.integration.model.layers;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.LayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.layers.UniversalLayerRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.integration.model.buttons.UndergroundFluidButtonManager;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidLocation;
import com.sinthoras.visualprospecting.integration.model.render.UndergroundFluidRenderStep;

public class UndergroundFluidLayerManager extends LayerManager {

    public static final UndergroundFluidLayerManager instance = new UndergroundFluidLayerManager();

    public UndergroundFluidLayerManager() {
        super(UndergroundFluidButtonManager.instance);
    }

    @Nullable
    @Override
    protected LayerRenderer addLayerRenderer(LayerManager manager, SupportedMods mod) {
        return new UniversalLayerRenderer(manager)
                .withRenderStep(location -> new UndergroundFluidRenderStep((UndergroundFluidLocation) location))
                .withRenderPriority(1);
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

    @Override
    public int getElementSize() {
        return 8;
    }
}
