package com.sinthoras.visualprospecting.integration.model;

import static com.sinthoras.visualprospecting.Utils.*;

import java.util.ArrayList;
import java.util.List;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.integration.model.buttons.ButtonManager;
import com.sinthoras.visualprospecting.integration.model.buttons.DirtyChunkButtonManager;
import com.sinthoras.visualprospecting.integration.model.buttons.OreVeinButtonManager;
import com.sinthoras.visualprospecting.integration.model.buttons.ThaumcraftNodeButtonManager;
import com.sinthoras.visualprospecting.integration.model.buttons.UndergroundFluidButtonManager;
import com.sinthoras.visualprospecting.integration.model.layers.DirtyChunkLayerManager;
import com.sinthoras.visualprospecting.integration.model.layers.LayerManager;
import com.sinthoras.visualprospecting.integration.model.layers.OreVeinLayerManager;
import com.sinthoras.visualprospecting.integration.model.layers.ThaumcraftNodeLayerManager;
import com.sinthoras.visualprospecting.integration.model.layers.UndergroundFluidChunkLayerManager;
import com.sinthoras.visualprospecting.integration.model.layers.UndergroundFluidLayerManager;
import com.sinthoras.visualprospecting.integration.tcnodetracker.NTNodeTrackerWaypointManager;
import com.sinthoras.visualprospecting.integration.xaerominimap.XaeroMiniMapState;

public class MapState {

    public static final MapState instance = new MapState();

    public final List<ButtonManager> buttons = new ArrayList<>();
    public final List<LayerManager> layers = new ArrayList<>();

    public MapState() {
        if (isTCNodeTrackerInstalled()) {
            buttons.add(ThaumcraftNodeButtonManager.instance);
            layers.add(ThaumcraftNodeLayerManager.instance);
            new NTNodeTrackerWaypointManager();
        }

        buttons.add(UndergroundFluidButtonManager.instance);
        layers.add(UndergroundFluidLayerManager.instance);
        layers.add(UndergroundFluidChunkLayerManager.instance);

        buttons.add(OreVeinButtonManager.instance);
        layers.add(OreVeinLayerManager.instance);

        if (Config.enableDeveloperOverlays) {
            buttons.add(DirtyChunkButtonManager.instance);
            layers.add(DirtyChunkLayerManager.instance);
        }

        if (isXaerosMinimapInstalled()) {
            // need to classload XaeroMiniMapState in order for its waypoint manager to get registered
            XaeroMiniMapState.instance.toString();
        }
    }
}
