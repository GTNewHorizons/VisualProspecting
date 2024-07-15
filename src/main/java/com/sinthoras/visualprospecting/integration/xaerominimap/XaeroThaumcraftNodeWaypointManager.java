package com.sinthoras.visualprospecting.integration.xaerominimap;

import com.dyonovan.tcnodetracker.TCNodeTracker;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;
import com.gtnewhorizons.navigator.api.xaero.waypoints.XaeroWaypointManager;

public class XaeroThaumcraftNodeWaypointManager extends XaeroWaypointManager {

    public XaeroThaumcraftNodeWaypointManager(InteractableLayerManager manager) {
        super(manager);
    }

    @Override
    public void clearActiveWaypoint() {
        TCNodeTracker.yMarker = -1;
    }

    @Override
    public void updateActiveWaypoint(Waypoint waypoint) {
        TCNodeTracker.xMarker = waypoint.blockX;
        TCNodeTracker.yMarker = waypoint.blockY;
        TCNodeTracker.zMarker = waypoint.blockZ;
    }

    @Override
    public boolean hasWaypoint() {
        return false;
    }

    @Override
    protected String getSymbol(Waypoint waypoint) {
        return "@";
    }
}
