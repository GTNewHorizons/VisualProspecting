package com.sinthoras.visualprospecting.integration.model.layers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;

import com.dyonovan.tcnodetracker.TCNodeTracker;
import com.dyonovan.tcnodetracker.lib.NodeList;
import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.locations.IWaypointAndLocationProvider;
import com.gtnewhorizons.navigator.api.model.waypoints.WaypointManager;
import com.sinthoras.visualprospecting.integration.journeymap.render.JMThaumcraftNodeRenderer;
import com.sinthoras.visualprospecting.integration.journeymap.waypoints.JMThaumcraftNodeWaypointManager;
import com.sinthoras.visualprospecting.integration.model.buttons.ThaumcraftNodeButtonManager;
import com.sinthoras.visualprospecting.integration.model.locations.ThaumcraftNodeLocation;
import com.sinthoras.visualprospecting.integration.xaerominimap.XaeroThaumcraftNodeWaypointManager;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.renderers.XaeroThaumcraftNodeRenderer;

public class ThaumcraftNodeLayerManager extends InteractableLayerManager {

    public static final ThaumcraftNodeLayerManager instance = new ThaumcraftNodeLayerManager();

    private int oldMinBlockX = 0;
    private int oldMinBlockZ = 0;
    private int oldMaxBlockX = 0;
    private int oldMaxBlockZ = 0;

    public ThaumcraftNodeLayerManager() {
        super(ThaumcraftNodeButtonManager.instance);
    }

    @Override
    protected boolean needsRegenerateVisibleElements(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        if (minBlockX != oldMinBlockX || minBlockZ != oldMinBlockZ
                || maxBlockX != oldMaxBlockX
                || maxBlockZ != oldMaxBlockZ) {
            oldMinBlockX = minBlockX;
            oldMinBlockZ = minBlockZ;
            oldMaxBlockX = maxBlockX;
            oldMaxBlockZ = maxBlockZ;
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    protected LayerRenderer addLayerRenderer(InteractableLayerManager manager, SupportedMods mod) {
        return switch (mod) {
            case JourneyMap -> new JMThaumcraftNodeRenderer(manager);
            case XaeroWorldMap -> new XaeroThaumcraftNodeRenderer(manager);
            default -> null;
        };
    }

    @Nullable
    @Override
    protected WaypointManager addWaypointManager(InteractableLayerManager manager, SupportedMods mod) {
        return switch (mod) {
            case JourneyMap -> new JMThaumcraftNodeWaypointManager(manager);
            case XaeroWorldMap -> new XaeroThaumcraftNodeWaypointManager(manager);
            default -> null;
        };
    }

    @Override
    protected List<? extends IWaypointAndLocationProvider> generateVisibleElements(int minBlockX, int minBlockZ,
            int maxBlockX, int maxBlockZ) {
        final int playerDimensionId = Minecraft.getMinecraft().thePlayer.dimension;

        ArrayList<ThaumcraftNodeLocation> thaumcraftNodeLocations = new ArrayList<>();

        for (NodeList node : TCNodeTracker.nodelist) {
            if (node.dim == playerDimensionId && node.x >= minBlockX
                    && node.x <= maxBlockX
                    && node.z >= minBlockZ
                    && node.z <= maxBlockZ) {
                thaumcraftNodeLocations.add(new ThaumcraftNodeLocation(node));
            }
        }

        return thaumcraftNodeLocations;
    }

    public void deleteNode(ThaumcraftNodeLocation thaumcraftNodeLocation) {
        TCNodeTracker.nodelist.removeIf(thaumcraftNodeLocation::belongsToNode);
        if (thaumcraftNodeLocation.isActiveAsWaypoint()) {
            clearActiveWaypoint();
        }
        forceRefresh();
    }
}
