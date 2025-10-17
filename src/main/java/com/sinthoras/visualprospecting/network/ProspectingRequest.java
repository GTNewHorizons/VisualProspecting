package com.sinthoras.visualprospecting.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gregtech.api.interfaces.IOreMaterial;
import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;
import io.netty.buffer.ByteBuf;

public class ProspectingRequest implements IMessage {

    public static long timestampLastRequest = 0;

    private int dimensionId;
    private int blockX;
    private int blockY;
    private int blockZ;
    private IOreMaterial foundOre;

    public ProspectingRequest() {}

    public ProspectingRequest(int dimensionId, int blockX, int blockY, int blockZ, IOreMaterial foundOre) {
        this.dimensionId = dimensionId;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.foundOre = foundOre;
    }

    public static boolean canSendRequest() {
        final long timestamp = System.currentTimeMillis();
        if (timestamp - timestampLastRequest > Config.minDelayBetweenVeinRequests) {
            timestampLastRequest = timestamp;
            return true;
        }
        return false;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimensionId = buf.readInt();
        blockX = buf.readInt();
        blockY = buf.readInt();
        blockZ = buf.readInt();
        foundOre = IOreMaterial.findMaterial(ByteBufUtils.readUTF8String(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimensionId);
        buf.writeInt(blockX);
        buf.writeInt(blockY);
        buf.writeInt(blockZ);
        ByteBufUtils.writeUTF8String(buf, foundOre.getInternalName());
    }

    public static class Handler implements IMessageHandler<ProspectingRequest, IMessage> {

        private static final Map<UUID, Long> lastRequestPerPlayer = new HashMap<>();

        @Override
        public IMessage onMessage(ProspectingRequest message, MessageContext ctx) {
            // Check if request is valid/not tempered with
            final UUID uuid = ctx.getServerHandler().playerEntity.getUniqueID();

            final long timestamp = System.currentTimeMillis();

            final long lastRequest = lastRequestPerPlayer.containsKey(uuid) ? lastRequestPerPlayer.get(uuid) : 0;
            lastRequestPerPlayer.put(uuid, timestamp);

            if (timestamp - lastRequest < Config.minDelayBetweenVeinRequests) return null;

            final float distanceSquared = ctx.getServerHandler().playerEntity.getPlayerCoordinates()
                    .getDistanceSquared(message.blockX, message.blockY, message.blockZ);
            final World world = ctx.getServerHandler().playerEntity.getEntityWorld();
            final int chunkX = Utils.coordBlockToChunk(message.blockX);
            final int chunkZ = Utils.coordBlockToChunk(message.blockZ);
            final boolean isChunkLoaded = world.getChunkProvider().chunkExists(chunkX, chunkZ);

            if (ctx.getServerHandler().playerEntity.dimension != message.dimensionId) return null;
            // max 32 blocks distance
            if (distanceSquared > 32 * 32) return null;

            if (!isChunkLoaded) return null;

            return prospect(message, world);
        }
    }

    public static @Nullable ProspectingNotification prospect(ProspectingRequest message, World world) {
        final int chunkX = Utils.coordBlockToChunk(message.blockX);
        final int chunkZ = Utils.coordBlockToChunk(message.blockZ);

        try (OreInfo<IOreMaterial> info = OreManager
                .getOreInfo(world, message.blockX, message.blockY, message.blockZ)) {
            if (info == null || info.isSmall || info.material != message.foundOre) return null;

            // Prioritise center vein
            final OreVeinPosition centerOreVeinPosition = ServerCache.instance
                    .getOreVein(message.dimensionId, chunkX, chunkZ);

            if (centerOreVeinPosition.veinType.containsOre(message.foundOre)) {
                return new ProspectingNotification(centerOreVeinPosition);
            }

            // Check if neighboring veins could fit
            final int centerChunkX = Utils.mapToCenterOreChunkCoord(chunkX);
            final int centerChunkZ = Utils.mapToCenterOreChunkCoord(chunkZ);

            for (int offsetChunkX = -3; offsetChunkX <= 3; offsetChunkX += 3) {
                for (int offsetChunkZ = -3; offsetChunkZ <= 3; offsetChunkZ += 3) {
                    if (offsetChunkX != 0 || offsetChunkZ != 0) {

                        final int neighborChunkX = centerChunkX + offsetChunkX;
                        final int neighborChunkZ = centerChunkZ + offsetChunkZ;

                        final int distanceBlocks = Math
                                .max(Math.abs(neighborChunkX - chunkX), Math.abs(neighborChunkZ - chunkZ));

                        final OreVeinPosition neighborOreVeinPosition = ServerCache.instance
                                .getOreVein(message.dimensionId, neighborChunkX, neighborChunkZ);

                        // Equals to: ceil(blockSize / 16.0) + 1
                        final int maxDistance = ((neighborOreVeinPosition.veinType.blockSize + 16) >> 4) + 1;

                        if (neighborOreVeinPosition.veinType.containsOre(message.foundOre)
                                && distanceBlocks <= maxDistance) {
                            return new ProspectingNotification(neighborOreVeinPosition);
                        }
                    }
                }
            }
        }

        return null;
    }
}
