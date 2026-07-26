package com.sinthoras.visualprospecting.integration.model.layers;

import java.util.ArrayList;
import java.util.Collection;

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
import com.sinthoras.visualprospecting.integration.model.render.OreVeinMapMarker;
import com.sinthoras.visualprospecting.integration.model.render.OreVeinRenderStep;

public class OreVeinLayerManager extends InteractableLayerManager {

    public static final OreVeinLayerManager instance = new OreVeinLayerManager();

    public OreVeinLayerManager() {
        super(OreVeinButtonManager.instance);
        setHasSearchField(true);
    }

    @Override
    public void onOpenMap() {
        VeinTypeCaching.recalculateSearch(Utils.getNEISearchPattern(), Utils.getNEISearchItemFilter());
    }

    @Nullable
    @Override
    protected LayerRenderer addLayerRenderer(InteractableLayerManager manager, SupportedMods mod) {
        return new UniversalInteractableRenderer(manager)
                .withRenderStep(location -> new OreVeinRenderStep((OreVeinLocation) location))
                .withMapMarker(location -> OreVeinMapMarker.create((OreVeinLocation) location));
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
    protected Collection<? extends ILocationProvider> generateVisibleLocations(int minBlockX, int minBlockZ,
            int maxBlockX, int maxBlockZ, int dimension) {
        Collection<OreVeinLocation> locations = new ArrayList<>();
        for (OreVeinPosition vein : ClientCache.instance.getAllOreVeins()) {
            int blockX = vein.getBlockX();
            int blockZ = vein.getBlockZ();
            if (vein.veinType != VeinType.NO_VEIN && vein.dimensionId == dimension
                    && blockX >= minBlockX
                    && blockX <= maxBlockX
                    && blockZ >= minBlockZ
                    && blockZ <= maxBlockZ) {
                locations.add(new OreVeinLocation(vein));
            }
        }
        return locations;
    }

    @Override
    public void onSearch(@NotNull String searchString) {
        VeinTypeCaching.recalculateSearch(Utils.getSearchPattern(searchString), Utils.getItemFilter(searchString));
    }
}
