package com.sinthoras.visualprospecting.integration.journeymap.waypoints;

import com.gtnewhorizons.navigator.api.journeymap.waypoints.JMWaypointManager;
import com.sinthoras.visualprospecting.integration.model.layers.OreVeinLayerManager;

public class OreVeinWaypointManager extends JMWaypointManager {

    public static final OreVeinWaypointManager instance = new OreVeinWaypointManager();

    public OreVeinWaypointManager() {
        super(OreVeinLayerManager.instance);
    }
}
