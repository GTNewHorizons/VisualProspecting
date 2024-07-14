package com.sinthoras.visualprospecting.integration.xaeroworldmap.renderers;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.xaero.renderers.XaeroLayerRenderer;
import com.sinthoras.visualprospecting.integration.model.layers.UndergroundFluidChunkLayerManager;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidChunkLocation;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.rendersteps.UndergroundFluidChunkRenderStep;

public class UndergroundFluidChunkRenderer extends XaeroLayerRenderer {

    public static UndergroundFluidChunkRenderer instance = new UndergroundFluidChunkRenderer();

    public UndergroundFluidChunkRenderer() {
        super(UndergroundFluidChunkLayerManager.instance);
    }

    @Override
    protected List<UndergroundFluidChunkRenderStep> generateRenderSteps(
            List<? extends ILocationProvider> visibleElements) {
        final List<UndergroundFluidChunkRenderStep> renderSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (UndergroundFluidChunkLocation) element)
                .forEach(location -> renderSteps.add(new UndergroundFluidChunkRenderStep(location)));
        return renderSteps;
    }
}
