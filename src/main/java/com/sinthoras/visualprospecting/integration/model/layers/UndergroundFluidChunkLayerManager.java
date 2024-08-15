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
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidChunkLocation;
import com.sinthoras.visualprospecting.integration.model.render.UndergroundFluidChunkRenderStep;

public class UndergroundFluidChunkLayerManager extends LayerManager {

    public static final UndergroundFluidChunkLayerManager instance = new UndergroundFluidChunkLayerManager();

    public UndergroundFluidChunkLayerManager() {
        super(UndergroundFluidButtonManager.instance);
    }

    @Nullable
    @Override
    protected LayerRenderer addLayerRenderer(LayerManager manager, SupportedMods mods) {
        return new UniversalLayerRenderer(manager).withRenderStep(
                location -> new UndergroundFluidChunkRenderStep((UndergroundFluidChunkLocation) location));
    }

    @Override
    protected ILocationProvider generateLocation(int chunkX, int chunkZ, int dim) {
        if (chunkX % VP.undergroundFluidSizeChunkX != 0 || chunkZ % VP.undergroundFluidSizeChunkZ != 0) {
            return null;
        }
        final UndergroundFluidPosition undergroundFluid = ClientCache.instance.getUndergroundFluid(dim, chunkX, chunkZ);
        if (!undergroundFluid.isProspected()) return null;
        generateExtraFluidChunks(chunkX, chunkZ, dim, undergroundFluid);
        return new UndergroundFluidChunkLocation(chunkX, chunkZ, dim, undergroundFluid, 0, 0);
    }

    private void generateExtraFluidChunks(int chunkX, int chunkZ, int dim, UndergroundFluidPosition undergroundFluid) {
        for (int offsetX = 0; offsetX < VP.undergroundFluidSizeChunkX; offsetX++) {
            for (int offsetZ = 0; offsetZ < VP.undergroundFluidSizeChunkZ; offsetZ++) {
                if (offsetX == 0 && offsetZ == 0) continue;
                addExtraLocation(
                        new UndergroundFluidChunkLocation(chunkX, chunkZ, dim, undergroundFluid, offsetX, offsetZ));
            }
        }
    }
}
