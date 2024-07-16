package com.sinthoras.visualprospecting.integration.journeymap.waypoints;

import com.dyonovan.tcnodetracker.TCNodeTracker;
import com.gtnewhorizons.navigator.api.journeymap.waypoints.JMWaypointManager;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;

public class JMThaumcraftNodeWaypointManager extends JMWaypointManager {

    public JMThaumcraftNodeWaypointManager(InteractableLayerManager manager) {
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
}
