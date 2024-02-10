package com.sinthoras.visualprospecting.integration.model.layers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.sinthoras.visualprospecting.integration.model.SupportedMods;
import com.sinthoras.visualprospecting.integration.model.buttons.ButtonManager;
import com.sinthoras.visualprospecting.integration.model.locations.IWaypointAndLocationProvider;
import com.sinthoras.visualprospecting.integration.model.waypoints.Waypoint;
import com.sinthoras.visualprospecting.integration.model.waypoints.WaypointManager;

import journeymap.client.model.BlockCoordIntPair;

public abstract class WaypointProviderManager extends LayerManager {

    private List<? extends IWaypointAndLocationProvider> visibleElements = new ArrayList<>();
    private Map<SupportedMods, WaypointManager> waypointManagers = new EnumMap<>(SupportedMods.class);

    protected Waypoint activeWaypoint = null;

    public WaypointProviderManager(ButtonManager buttonManager) {
        super(buttonManager);
    }

    public void setActiveWaypoint(Waypoint waypoint) {
        activeWaypoint = waypoint;
        visibleElements.forEach(element -> element.onWaypointUpdated(waypoint));
        waypointManagers.values().forEach(translator -> translator.updateActiveWaypoint(waypoint));
    }

    public void clearActiveWaypoint() {
        activeWaypoint = null;
        visibleElements.forEach(IWaypointAndLocationProvider::onWaypointCleared);
        waypointManagers.values().forEach(WaypointManager::clearActiveWaypoint);
    }

    public boolean hasActiveWaypoint() {
        return activeWaypoint != null;
    }

    public boolean doActionOutsideLayer(BlockCoordIntPair blockCoord) {
        return false;
    }

    public void registerWaypointManager(SupportedMods map, WaypointManager waypointManager) {
        waypointManagers.put(map, waypointManager);
    }

    public WaypointManager getWaypointManager(SupportedMods map) {
        return waypointManagers.get(map);
    }

    protected abstract List<? extends IWaypointAndLocationProvider> generateVisibleElements(int minBlockX,
            int minBlockZ, int maxBlockX, int maxBlockZ);

    @Override
    protected void checkAndUpdateElements(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        if (forceRefresh || needsRegenerateVisibleElements(minBlockX, minBlockZ, maxBlockX, maxBlockZ)) {
            visibleElements = generateVisibleElements(minBlockX, minBlockZ, maxBlockX, maxBlockZ);

            if (hasActiveWaypoint()) {
                for (IWaypointAndLocationProvider element : visibleElements) {
                    element.onWaypointUpdated(activeWaypoint);
                }
            }

            layerRenderer.values().forEach(layer -> layer.updateVisibleElements(visibleElements));
            forceRefresh = false;
        }
    }
}
