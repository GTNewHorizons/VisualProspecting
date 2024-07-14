package com.sinthoras.visualprospecting.integration.xaerominimap;

import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;
import com.gtnewhorizons.navigator.api.xaero.waypoints.XaeroWaypointManager;
import com.sinthoras.visualprospecting.integration.model.layers.OreVeinLayerManager;

public class OreVeinWaypointManager extends XaeroWaypointManager {

    public static OreVeinWaypointManager instance = new OreVeinWaypointManager();

    public OreVeinWaypointManager() {
        super(OreVeinLayerManager.instance);
    }

    @Override
    protected String getSymbol(Waypoint waypoint) {
        return "!";
    }
}
