package com.sinthoras.visualprospecting.integration.model.layers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizons.navigator.api.journeymap.waypoints.JMWaypointManager;
import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.layers.UniversalInteractableRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.model.waypoints.WaypointManager;
import com.gtnewhorizons.navigator.api.xaero.waypoints.XaeroWaypointManager;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import com.sinthoras.visualprospecting.integration.model.buttons.OreVeinButtonManager;
import com.sinthoras.visualprospecting.integration.model.locations.OreVeinLocation;
import com.sinthoras.visualprospecting.integration.model.render.OreVeinRenderStep;

public class OreVeinLayerManager extends InteractableLayerManager {

    public static final OreVeinLayerManager instance = new OreVeinLayerManager();

    public OreVeinLayerManager() {
        super(OreVeinButtonManager.instance);
        setHasSearchField(true);
    }

    @Override
    public void onOpenMap() {
        VeinTypeCaching.recalculateSearch(Utils.getNEISearchPattern());
    }

    @Nullable
    @Override
    protected LayerRenderer addLayerRenderer(InteractableLayerManager manager, SupportedMods mod) {
        return new UniversalInteractableRenderer(manager)
                .withRenderStep(location -> new OreVeinRenderStep((OreVeinLocation) location));
    }

    @Nullable
    @Override
    protected WaypointManager addWaypointManager(InteractableLayerManager manager, SupportedMods mod) {
        return switch (mod) {
            case JourneyMap -> new JMWaypointManager(manager);
            case XaeroWorldMap -> new XaeroWaypointManager(manager, "!");
            default -> null;
        };
    }

    @Override
    protected ILocationProvider generateLocation(int chunkX, int chunkZ, int dim) {
        int oreChunkX = Utils.mapToCenterOreChunkCoord(chunkX);
        int oreChunkZ = Utils.mapToCenterOreChunkCoord(chunkZ);
        if (chunkX % oreChunkX != 0 || chunkZ % oreChunkZ != 0) {
            return null;
        }
        OreVeinPosition oreVeinPosition = ClientCache.instance.getOreVein(dim, oreChunkX, oreChunkZ);
        if (oreVeinPosition.veinType != VeinType.NO_VEIN) {
            return new OreVeinLocation(oreVeinPosition);
        }

        return null;
    }

    @Override
    public void onSearch(@NotNull String searchString) {
        VeinTypeCaching.recalculateSearch(Utils.getSearchPattern(searchString));
    }
}
