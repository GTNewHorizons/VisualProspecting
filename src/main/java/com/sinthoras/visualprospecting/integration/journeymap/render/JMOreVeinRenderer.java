package com.sinthoras.visualprospecting.integration.journeymap.render;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.navigator.api.journeymap.render.JMInteractableLayerRenderer;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.model.steps.RenderStep;
import com.sinthoras.visualprospecting.integration.journeymap.drawsteps.OreVeinDrawStep;
import com.sinthoras.visualprospecting.integration.model.locations.OreVeinLocation;

public class JMOreVeinRenderer extends JMInteractableLayerRenderer {

    public JMOreVeinRenderer(InteractableLayerManager manager) {
        super(manager);
    }

    @Override
    protected List<? extends RenderStep> generateRenderSteps(List<? extends ILocationProvider> visibleElements) {
        final List<OreVeinDrawStep> drawSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (OreVeinLocation) element)
                .forEach(location -> drawSteps.add(new OreVeinDrawStep(location)));
        return drawSteps;
    }
}
