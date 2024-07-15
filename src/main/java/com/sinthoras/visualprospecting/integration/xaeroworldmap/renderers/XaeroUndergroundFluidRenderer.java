package com.sinthoras.visualprospecting.integration.xaeroworldmap.renderers;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.navigator.api.model.layers.LayerManager;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.xaero.renderers.XaeroLayerRenderer;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidLocation;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.rendersteps.UndergroundFluidRenderStep;

public class XaeroUndergroundFluidRenderer extends XaeroLayerRenderer {

    public XaeroUndergroundFluidRenderer(LayerManager manager) {
        super(manager);
    }

    @Override
    protected List<UndergroundFluidRenderStep> generateRenderSteps(List<? extends ILocationProvider> visibleElements) {
        final List<UndergroundFluidRenderStep> renderSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (UndergroundFluidLocation) element)
                .forEach(location -> renderSteps.add(new UndergroundFluidRenderStep(location)));
        return renderSteps;
    }
}
