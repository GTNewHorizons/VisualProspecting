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
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.teams.TeamProspectionDispatcher;

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

            ProspectingNotification response = prospect(message, world);
            if (response != null) {
                TeamProspectionDispatcher.deliverProspectingResults(ctx.getServerHandler().playerEntity, response);
            }
            return null;
        }
    }

    public static @Nullable ProspectingNotification prospect(ProspectingRequest message, World world) {
        final int chunkX = Utils.coordBlockToChunk(message.blockX);
        final int chunkZ = Utils.coordBlockToChunk(message.blockZ);

        try (OreInfo<IOreMaterial> info = OreManager
                .getOreInfo(world, message.blockX, message.blockY, message.blockZ)) {
            if (info == null || !info.isNatural || info.isSmall || info.material != message.foundOre) return null;

            final OreVeinPosition vein = ServerCache.instance
                    .resolveVeinForOre(message.dimensionId, chunkX, chunkZ, message.foundOre);
            if (vein.veinType != VeinType.NO_VEIN) return new ProspectingNotification(vein);
        }

        return null;
    }
}
