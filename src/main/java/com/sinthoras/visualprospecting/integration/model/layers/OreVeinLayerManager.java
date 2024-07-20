package com.sinthoras.visualprospecting.integration.model.layers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizons.navigator.api.journeymap.waypoints.JMWaypointManager;
import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.locations.IWaypointAndLocationProvider;
import com.gtnewhorizons.navigator.api.model.waypoints.WaypointManager;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import com.sinthoras.visualprospecting.integration.journeymap.render.JMOreVeinRenderer;
import com.sinthoras.visualprospecting.integration.model.buttons.OreVeinButtonManager;
import com.sinthoras.visualprospecting.integration.model.locations.OreVeinLocation;
import com.sinthoras.visualprospecting.integration.xaerominimap.OreVeinWaypointManager;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.renderers.XaeroOreVeinRenderer;

public class OreVeinLayerManager extends InteractableLayerManager {

    public static final OreVeinLayerManager instance = new OreVeinLayerManager();

    private int oldMinOreChunkX = 0;
    private int oldMaxOreChunkX = 0;
    private int oldMinOreChunkZ = 0;
    private int oldMaxOreChunkZ = 0;

    public OreVeinLayerManager() {
        super(OreVeinButtonManager.instance);
    }

    @Override
    public void onOpenMap() {
        VeinTypeCaching.recalculateNEISearch();
    }

    @Override
    protected boolean needsRegenerateVisibleElements(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        final int minOreChunkX = Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(minBlockX));
        final int minOreChunkZ = Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(minBlockZ));
        final int maxOreChunkX = Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(maxBlockX));
        final int maxOreChunkZ = Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(maxBlockZ));

        if (minOreChunkX != oldMinOreChunkX || maxOreChunkX != oldMaxOreChunkX
                || minOreChunkZ != oldMinOreChunkZ
                || maxOreChunkZ != oldMaxOreChunkZ) {
            oldMinOreChunkX = minOreChunkX;
            oldMaxOreChunkX = maxOreChunkX;
            oldMinOreChunkZ = minOreChunkZ;
            oldMaxOreChunkZ = maxOreChunkZ;
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    protected LayerRenderer addLayerRenderer(InteractableLayerManager manager, SupportedMods mod) {
        return switch (mod) {
            case JourneyMap -> new JMOreVeinRenderer(manager);
            case XaeroWorldMap -> new XaeroOreVeinRenderer(manager);
            default -> null;
        };
    }

    @Nullable
    @Override
    protected WaypointManager addWaypointManager(InteractableLayerManager manager, SupportedMods mod) {
        return switch (mod) {
            case JourneyMap -> new JMWaypointManager(manager);
            case XaeroWorldMap -> new OreVeinWaypointManager(manager);
            default -> null;
        };
    }

    @Override
    protected List<? extends IWaypointAndLocationProvider> generateVisibleElements(int minBlockX, int minBlockZ,
            int maxBlockX, int maxBlockZ) {
        final int minOreChunkX = Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(minBlockX));
        final int minOreChunkZ = Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(minBlockZ));
        final int maxOreChunkX = Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(maxBlockX));
        final int maxOreChunkZ = Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(maxBlockZ));
        final int playerDimensionId = Minecraft.getMinecraft().thePlayer.dimension;

        ArrayList<OreVeinLocation> oreChunkLocations = new ArrayList<>();

        for (int chunkX = minOreChunkX; chunkX <= maxOreChunkX; chunkX = Utils.mapToCenterOreChunkCoord(chunkX + 3)) {
            for (int chunkZ = minOreChunkZ; chunkZ
                    <= maxOreChunkZ; chunkZ = Utils.mapToCenterOreChunkCoord(chunkZ + 3)) {
                final OreVeinPosition oreVeinPosition = ClientCache.instance
                        .getOreVein(playerDimensionId, chunkX, chunkZ);
                if (oreVeinPosition.veinType != VeinType.NO_VEIN) {
                    oreChunkLocations.add(new OreVeinLocation(oreVeinPosition));
                }
            }
        }

        return oreChunkLocations;
    }
}
