package com.sinthoras.visualprospecting.integration.xaeroworldmap.renderers;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.xaero.renderers.XaeroInteractableLayerRenderer;
import com.sinthoras.visualprospecting.integration.model.layers.ThaumcraftNodeLayerManager;
import com.sinthoras.visualprospecting.integration.model.locations.ThaumcraftNodeLocation;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.rendersteps.ThaumcraftNodeRenderStep;

public class ThaumcraftNodeRenderer extends XaeroInteractableLayerRenderer {

    public static ThaumcraftNodeRenderer instance = new ThaumcraftNodeRenderer();

    public ThaumcraftNodeRenderer() {
        super(ThaumcraftNodeLayerManager.instance);
    }

    @Override
    protected List<ThaumcraftNodeRenderStep> generateRenderSteps(List<? extends ILocationProvider> visibleElements) {
        final List<ThaumcraftNodeRenderStep> renderSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (ThaumcraftNodeLocation) element)
                .forEach(location -> renderSteps.add(new ThaumcraftNodeRenderStep(location)));
        return renderSteps;
    }
}
