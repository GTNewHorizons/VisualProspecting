package com.sinthoras.visualprospecting.integration.xaeroworldmap;

import com.gtnewhorizons.navigator.api.NavigatorApi;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.integration.xaerominimap.OreVeinWaypointManager;
import com.sinthoras.visualprospecting.integration.xaerominimap.ThaumcraftNodeWaypointManager;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.buttons.OreVeinButton;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.buttons.ThaumcraftNodeButton;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.buttons.UndergroundFluidButton;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.renderers.OreVeinRenderer;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.renderers.ThaumcraftNodeRenderer;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.renderers.UndergroundFluidChunkRenderer;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.renderers.UndergroundFluidRenderer;

public class XaeroIntegration {

    public static void init() {
        if (!Utils.isXaerosWorldMapInstalled()) return;
        NavigatorApi.registerLayerRenderer(OreVeinRenderer.instance);
        NavigatorApi.registerLayerRenderer(UndergroundFluidRenderer.instance);
        NavigatorApi.registerLayerRenderer(UndergroundFluidChunkRenderer.instance);

        NavigatorApi.registerLayerButton(OreVeinButton.instance);
        NavigatorApi.registerLayerButton(UndergroundFluidButton.instance);

        NavigatorApi.registerWaypointManager(OreVeinWaypointManager.instance);

        if (Utils.isTCNodeTrackerInstalled()) {
            NavigatorApi.registerLayerRenderer(ThaumcraftNodeRenderer.instance);
            NavigatorApi.registerLayerButton(ThaumcraftNodeButton.instance);
            NavigatorApi.registerWaypointManager(ThaumcraftNodeWaypointManager.instance);
        }
    }
}
