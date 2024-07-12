package com.sinthoras.visualprospecting.integration.journeymap.render;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.navigator.api.journeymap.render.JMInteractableLayerRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.model.steps.RenderStep;
import com.sinthoras.visualprospecting.integration.journeymap.drawsteps.ThaumcraftNodeDrawStep;
import com.sinthoras.visualprospecting.integration.model.layers.ThaumcraftNodeLayerManager;
import com.sinthoras.visualprospecting.integration.model.locations.ThaumcraftNodeLocation;

public class ThaumcraftNodeRenderer extends JMInteractableLayerRenderer {

    public static ThaumcraftNodeRenderer instance = new ThaumcraftNodeRenderer();

    public ThaumcraftNodeRenderer() {
        super(ThaumcraftNodeLayerManager.instance);
    }

    @Override
    protected List<? extends RenderStep> generateRenderSteps(List<? extends ILocationProvider> visibleElements) {
        final List<ThaumcraftNodeDrawStep> drawSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (ThaumcraftNodeLocation) element)
                .forEach(location -> drawSteps.add(new ThaumcraftNodeDrawStep(location)));
        return drawSteps;
    }
}
