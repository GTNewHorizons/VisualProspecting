package com.sinthoras.visualprospecting.integration.journeymap.render;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.navigator.api.journeymap.render.JMLayerRenderer;
import com.gtnewhorizons.navigator.api.model.layers.LayerManager;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.model.steps.RenderStep;
import com.sinthoras.visualprospecting.integration.journeymap.drawsteps.UndergroundFluidChunkDrawStep;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidChunkLocation;

public class JMUndergroundFluidChunkRenderer extends JMLayerRenderer {

    public JMUndergroundFluidChunkRenderer(LayerManager manager) {
        super(manager);
    }

    @Override
    protected List<? extends RenderStep> generateRenderSteps(List<? extends ILocationProvider> visibleElements) {
        final List<UndergroundFluidChunkDrawStep> drawSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (UndergroundFluidChunkLocation) element)
                .forEach(location -> drawSteps.add(new UndergroundFluidChunkDrawStep(location)));
        return drawSteps;
    }
}
