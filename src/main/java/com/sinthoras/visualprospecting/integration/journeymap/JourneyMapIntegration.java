package com.sinthoras.visualprospecting.integration.journeymap;

import com.gtnewhorizons.navigator.api.NavigatorApi;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.integration.journeymap.render.OreVeinRenderer;
import com.sinthoras.visualprospecting.integration.journeymap.render.ThaumcraftNodeRenderer;
import com.sinthoras.visualprospecting.integration.journeymap.render.UndergroundFluidChunkRenderer;
import com.sinthoras.visualprospecting.integration.journeymap.render.UndergroundFluidRenderer;
import com.sinthoras.visualprospecting.integration.journeymap.waypoints.OreVeinWaypointManager;
import com.sinthoras.visualprospecting.integration.journeymap.waypoints.ThaumcraftNodeWaypointManager;

public class JourneyMapIntegration {

    public static void init() {
        if (!Utils.isJourneyMapInstalled()) return;

        NavigatorApi.registerLayerRenderer(OreVeinRenderer.instance);
        NavigatorApi.registerLayerRenderer(UndergroundFluidRenderer.instance);
        NavigatorApi.registerLayerRenderer(UndergroundFluidChunkRenderer.instance);

        NavigatorApi.registerWaypointManager(OreVeinWaypointManager.instance);

        if (Utils.isTCNodeTrackerInstalled()) {
            NavigatorApi.registerLayerRenderer(ThaumcraftNodeRenderer.instance);
            NavigatorApi.registerWaypointManager(ThaumcraftNodeWaypointManager.instance);
        }
    }
}
