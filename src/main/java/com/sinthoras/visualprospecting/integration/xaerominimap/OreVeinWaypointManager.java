package com.sinthoras.visualprospecting.integration.xaerominimap;

import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;
import com.gtnewhorizons.navigator.api.xaero.waypoints.XaeroWaypointManager;

public class OreVeinWaypointManager extends XaeroWaypointManager {

    public OreVeinWaypointManager(InteractableLayerManager manager) {
        super(manager);
    }

    @Override
    protected String getSymbol(Waypoint waypoint) {
        return "!";
    }
}
