package com.sinthoras.visualprospecting.integration.xaerominimap;

import com.dyonovan.tcnodetracker.TCNodeTracker;
import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;
import com.gtnewhorizons.navigator.api.xaero.waypoints.XaeroWaypointManager;
import com.sinthoras.visualprospecting.integration.model.layers.ThaumcraftNodeLayerManager;

public class ThaumcraftNodeWaypointManager extends XaeroWaypointManager {

    public static ThaumcraftNodeWaypointManager instance = new ThaumcraftNodeWaypointManager();

    public ThaumcraftNodeWaypointManager() {
        super(ThaumcraftNodeLayerManager.instance);
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
