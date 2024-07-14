package com.sinthoras.visualprospecting.integration.journeymap.render;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.navigator.api.journeymap.render.JMLayerRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.model.steps.RenderStep;
import com.sinthoras.visualprospecting.integration.journeymap.drawsteps.UndergroundFluidDrawStep;
import com.sinthoras.visualprospecting.integration.model.layers.UndergroundFluidLayerManager;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidLocation;

public class UndergroundFluidRenderer extends JMLayerRenderer {

    public static final UndergroundFluidRenderer instance = new UndergroundFluidRenderer();

    public UndergroundFluidRenderer() {
        super(UndergroundFluidLayerManager.instance);
    }

    @Override
    protected List<? extends RenderStep> generateRenderSteps(List<? extends ILocationProvider> visibleElements) {
        final List<UndergroundFluidDrawStep> drawSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (UndergroundFluidLocation) element)
                .forEach(location -> drawSteps.add(new UndergroundFluidDrawStep(location)));
        return drawSteps;
    }
}
