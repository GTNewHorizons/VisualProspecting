package com.sinthoras.visualprospecting.integration.journeymap;

import static com.sinthoras.visualprospecting.Utils.isTCNodeTrackerInstalled;

import java.util.ArrayList;
import java.util.List;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.integration.journeymap.buttons.DirtyChunkButton;
import com.sinthoras.visualprospecting.integration.journeymap.buttons.LayerButton;
import com.sinthoras.visualprospecting.integration.journeymap.buttons.OreVeinButton;
import com.sinthoras.visualprospecting.integration.journeymap.buttons.ThaumcraftNodeButton;
import com.sinthoras.visualprospecting.integration.journeymap.buttons.UndergroundFluidButton;
import com.sinthoras.visualprospecting.integration.journeymap.render.DirtyChunkRenderer;
import com.sinthoras.visualprospecting.integration.journeymap.render.LayerRenderer;
import com.sinthoras.visualprospecting.integration.journeymap.render.OreVeinRenderer;
import com.sinthoras.visualprospecting.integration.journeymap.render.ThaumcraftNodeRenderer;
import com.sinthoras.visualprospecting.integration.journeymap.render.UndergroundFluidChunkRenderer;
import com.sinthoras.visualprospecting.integration.journeymap.render.UndergroundFluidRenderer;
import com.sinthoras.visualprospecting.integration.journeymap.waypoints.OreVeinWaypointManager;
import com.sinthoras.visualprospecting.integration.journeymap.waypoints.ThaumcraftNodeWaypointManager;
import com.sinthoras.visualprospecting.integration.journeymap.waypoints.WaypointManager;
import com.sinthoras.visualprospecting.mixins.late.journeymap.FullscreenAccessor;

import journeymap.client.render.map.GridRenderer;

public class JourneyMapState {

    public static JourneyMapState instance = new JourneyMapState();

    public final List<LayerButton> buttons = new ArrayList<>();
    public final List<LayerRenderer> renderers = new ArrayList<>();
    public final List<WaypointManager> waypointManagers = new ArrayList<>();

    public JourneyMapState() {
        if (isTCNodeTrackerInstalled()) {
            buttons.add(ThaumcraftNodeButton.instance);
            renderers.add(ThaumcraftNodeRenderer.instance);
            waypointManagers.add(ThaumcraftNodeWaypointManager.instance);
        }

        buttons.add(UndergroundFluidButton.instance);
        renderers.add(UndergroundFluidRenderer.instance);
        renderers.add(UndergroundFluidChunkRenderer.instance);

        buttons.add(OreVeinButton.instance);
        renderers.add(OreVeinRenderer.instance);
        waypointManagers.add(OreVeinWaypointManager.instance);

        if (Config.enableDeveloperOverlays) {
            buttons.add(DirtyChunkButton.instance);
            renderers.add(DirtyChunkRenderer.instance);
        }
    }

    public void openJourneyMapAt(int blockX, int blockZ) {
        final GridRenderer gridRenderer = FullscreenAccessor.getGridRenderer();
        assert gridRenderer != null;
        gridRenderer.center(gridRenderer.getMapType(), blockX, blockZ, gridRenderer.getZoom());
    }

    public void openJourneyMapAt(int blockX, int blockZ, int zoom) {
        final GridRenderer gridRenderer = FullscreenAccessor.getGridRenderer();
        assert gridRenderer != null;
        gridRenderer.center(gridRenderer.getMapType(), blockX, blockZ, zoom);
    }
}
