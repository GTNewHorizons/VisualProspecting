package com.sinthoras.visualprospecting.integration.model.layers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.LayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.integration.journeymap.render.JMUndergroundFluidRenderer;
import com.sinthoras.visualprospecting.integration.model.buttons.UndergroundFluidButtonManager;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidLocation;
import com.sinthoras.visualprospecting.integration.xaeroworldmap.renderers.XaeroUndergroundFluidRenderer;

public class UndergroundFluidLayerManager extends LayerManager {

    public static final UndergroundFluidLayerManager instance = new UndergroundFluidLayerManager();

    private int oldMinUndergroundFluidX = 0;
    private int oldMaxUndergroundFluidX = 0;
    private int oldMinUndergroundFluidZ = 0;
    private int oldMaxUndergroundFluidZ = 0;

    public UndergroundFluidLayerManager() {
        super(UndergroundFluidButtonManager.instance);
    }

    @Override
    protected boolean needsRegenerateVisibleElements(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        final int minUndergroundFluidX = Utils
                .mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(minBlockX));
        final int minUndergroundFluidZ = Utils
                .mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(minBlockZ));
        final int maxUndergroundFluidX = Utils
                .mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(maxBlockX));
        final int maxUndergroundFluidZ = Utils
                .mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(maxBlockZ));
        if (minUndergroundFluidX != oldMinUndergroundFluidX || maxUndergroundFluidX != oldMaxUndergroundFluidX
                || minUndergroundFluidZ != oldMinUndergroundFluidZ
                || maxUndergroundFluidZ != oldMaxUndergroundFluidZ) {
            oldMinUndergroundFluidX = minUndergroundFluidX;
            oldMaxUndergroundFluidX = maxUndergroundFluidX;
            oldMinUndergroundFluidZ = minUndergroundFluidZ;
            oldMaxUndergroundFluidZ = maxUndergroundFluidZ;
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    protected LayerRenderer addLayerRenderer(LayerManager manager, SupportedMods mod) {
        return switch (mod) {
            case JourneyMap -> new JMUndergroundFluidRenderer(manager);
            case XaeroWorldMap -> new XaeroUndergroundFluidRenderer(manager);
            default -> null;
        };
    }

    @Override
    protected List<? extends ILocationProvider> generateVisibleElements(int minBlockX, int minBlockZ, int maxBlockX,
            int maxBlockZ) {
        final int minUndergroundFluidX = Utils
                .mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(minBlockX));
        final int minUndergroundFluidZ = Utils
                .mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(minBlockZ));
        final int maxUndergroundFluidX = Utils
                .mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(maxBlockX));
        final int maxUndergroundFluidZ = Utils
                .mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(maxBlockZ));
        final int playerDimensionId = Minecraft.getMinecraft().thePlayer.dimension;

        ArrayList<UndergroundFluidLocation> undergroundFluidLocations = new ArrayList<>();

        for (int chunkX = minUndergroundFluidX; chunkX
                <= maxUndergroundFluidX; chunkX += VP.undergroundFluidSizeChunkX) {
            for (int chunkZ = minUndergroundFluidZ; chunkZ
                    <= maxUndergroundFluidZ; chunkZ += VP.undergroundFluidSizeChunkZ) {
                final UndergroundFluidPosition undergroundFluid = ClientCache.instance
                        .getUndergroundFluid(playerDimensionId, chunkX, chunkZ);
                if (undergroundFluid.isProspected()) {
                    undergroundFluidLocations.add(new UndergroundFluidLocation(undergroundFluid));
                }
            }
        }

        return undergroundFluidLocations;
    }
}
