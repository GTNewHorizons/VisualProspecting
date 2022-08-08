package com.sinthoras.visualprospecting.integration.model.layers;

import com.dyonovan.tcnodetracker.TCNodeTracker;
import com.dyonovan.tcnodetracker.lib.NodeList;
import com.sinthoras.visualprospecting.integration.model.buttons.ThaumcraftNodeButtonManager;
import com.sinthoras.visualprospecting.integration.model.locations.IWaypointAndLocationProvider;
import com.sinthoras.visualprospecting.integration.model.locations.ThaumcraftNodeLocation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public class ThaumcraftNodeLayerManager extends WaypointProviderManager {

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
        if (minBlockX != oldMinBlockX
                || minBlockZ != oldMinBlockZ
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

    @Override
    protected List<? extends IWaypointAndLocationProvider> generateVisibleElements(
            int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        final int playerDimensionId = Minecraft.getMinecraft().thePlayer.dimension;

        ArrayList<ThaumcraftNodeLocation> thaumcraftNodeLocations = new ArrayList<>();

        for (NodeList node : TCNodeTracker.nodelist) {
            if (node.dim == playerDimensionId
                    && node.x >= minBlockX
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
