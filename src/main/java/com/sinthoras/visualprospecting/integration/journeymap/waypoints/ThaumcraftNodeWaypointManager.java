package com.sinthoras.visualprospecting.integration.journeymap.waypoints;

import com.dyonovan.tcnodetracker.TCNodeTracker;
import com.gtnewhorizons.navigator.api.journeymap.waypoints.JMWaypointManager;
import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;
import com.sinthoras.visualprospecting.integration.model.layers.ThaumcraftNodeLayerManager;

public class ThaumcraftNodeWaypointManager extends JMWaypointManager {

    public static final ThaumcraftNodeWaypointManager instance = new ThaumcraftNodeWaypointManager();

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
}
