package com.sinthoras.visualprospecting.integration.model;

import static com.sinthoras.visualprospecting.Utils.isTCNodeTrackerInstalled;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.integration.model.buttons.*;
import com.sinthoras.visualprospecting.integration.model.layers.*;
import com.sinthoras.visualprospecting.integration.tcnodetracker.NTNodeTrackerWaypointManager;
import java.util.ArrayList;
import java.util.List;

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
    }
}
