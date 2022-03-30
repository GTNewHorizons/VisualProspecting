package com.sinthoras.visualprospecting.integration.model.layers;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.integration.model.buttons.DirtyChunkButtonManager;
import com.sinthoras.visualprospecting.integration.model.locations.DirtyChunkLocation;
import com.sinthoras.visualprospecting.integration.model.locations.ILocationProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class DirtyChunkLayerManager extends LayerManager {

    public static final DirtyChunkLayerManager instance = new DirtyChunkLayerManager();

    public DirtyChunkLayerManager() {
        super(DirtyChunkButtonManager.instance);
    }

    @Override
    protected boolean needsRegenerateVisibleElements(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        return true;
    }

    @Override
    protected List<? extends ILocationProvider> generateVisibleElements(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        final int minX = Utils.coordBlockToChunk(minBlockX);
        final int minZ = Utils.coordBlockToChunk(minBlockZ);
        final int maxX = Utils.coordBlockToChunk(maxBlockX);
        final int maxZ = Utils.coordBlockToChunk(maxBlockZ);
        final EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        final int playerDimensionId = player.dimension;
        final int renderDistance = Minecraft.getMinecraft().gameSettings.renderDistanceChunks;

        ArrayList<DirtyChunkLocation> dirtyChunks = new ArrayList<>();

        if (MinecraftServer.getServer() == null || MinecraftServer.getServer().worldServerForDimension(playerDimensionId) == null) {
            return dirtyChunks;
        }

        World w = MinecraftServer.getServer().worldServerForDimension(playerDimensionId);

        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                int dx = player.chunkCoordX - chunkX;
                int dz = player.chunkCoordZ - chunkZ;
                if (dx * dx + dz * dz > renderDistance * renderDistance) {
                    continue;
                }
                final boolean dirty = w.getChunkFromChunkCoords(chunkX, chunkZ).isModified;
                dirtyChunks.add(new DirtyChunkLocation(chunkX, chunkZ, playerDimensionId, dirty));
            }
        }

        return dirtyChunks;
    }
}
