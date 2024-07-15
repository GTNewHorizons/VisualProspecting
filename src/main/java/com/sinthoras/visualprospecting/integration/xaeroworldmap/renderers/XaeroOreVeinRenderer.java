package com.sinthoras.visualprospecting.integration.xaeroworldmap.renderers;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.xaero.renderers.XaeroInteractableLayerRenderer;
import com.sinthoras.visualprospecting.integration.model.locations.OreVeinLocation;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.rendersteps.OreVeinRenderStep;

public class XaeroOreVeinRenderer extends XaeroInteractableLayerRenderer {

    public XaeroOreVeinRenderer(InteractableLayerManager manager) {
        super(manager);
    }

    @Override
    protected List<OreVeinRenderStep> generateRenderSteps(List<? extends ILocationProvider> visibleElements) {
        final List<OreVeinRenderStep> renderSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (OreVeinLocation) element)
                .forEach(location -> renderSteps.add(new OreVeinRenderStep(location)));
        return renderSteps;
    }
}
