package com.sinthoras.visualprospecting.integration.journeymap.render;

import java.util.ArrayList;
import java.util.List;

import com.sinthoras.visualprospecting.integration.journeymap.drawsteps.DirtyChunkDrawStep;
import com.sinthoras.visualprospecting.integration.model.layers.DirtyChunkLayerManager;
import com.sinthoras.visualprospecting.integration.model.locations.DirtyChunkLocation;
import com.sinthoras.visualprospecting.integration.model.locations.ILocationProvider;

import journeymap.client.render.draw.DrawStep;

public class DirtyChunkRenderer extends LayerRenderer {

    public static final DirtyChunkRenderer instance = new DirtyChunkRenderer();

    public DirtyChunkRenderer() {
        super(DirtyChunkLayerManager.instance);
    }

    @Override
    public List<? extends DrawStep> mapLocationProviderToDrawStep(List<? extends ILocationProvider> visibleElements) {
        final List<DirtyChunkDrawStep> drawSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (DirtyChunkLocation) element)
                .forEach(location -> drawSteps.add(new DirtyChunkDrawStep(location)));
        return drawSteps;
    }
}
