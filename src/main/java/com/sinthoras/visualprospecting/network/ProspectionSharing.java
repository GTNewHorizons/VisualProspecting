package com.sinthoras.visualprospecting.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.TransferCache;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.utils.VPByteBufUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class ProspectionSharing implements IMessage {

    private static final int BYTES_OVERHEAD = 2 * Byte.BYTES + 2 * Integer.BYTES;

    final List<OreVeinPosition> oreVeins = new ArrayList<>();
    final List<UndergroundFluidPosition> undergroundFluids = new ArrayList<>();
    private int bytesUsed = BYTES_OVERHEAD;
    boolean isFirstMessage = false;
    boolean isLastMessage = false;

    public ProspectionSharing() {}

    public int putOreVeins(List<OreVeinPosition> oreVeins) {
        final int availableBytes = VP.uploadSizePerPacketInBytes - bytesUsed;
        final int maxAddedOreVeins = availableBytes / OreVeinPosition.MAX_BYTES;
        final int addedOreVeins = Math.min(oreVeins.size(), maxAddedOreVeins);
        this.oreVeins.addAll(oreVeins.subList(0, addedOreVeins));
        bytesUsed += addedOreVeins * OreVeinPosition.MAX_BYTES;
        return addedOreVeins;
    }

    public int putOreUndergroundFluids(List<UndergroundFluidPosition> undergroundFluids) {
        final int availableBytes = VP.uploadSizePerPacketInBytes - bytesUsed;
        final int maxAddedUndergroundFluids = availableBytes / UndergroundFluidPosition.BYTES;
        final int addedUndergroundFluids = Math.min(undergroundFluids.size(), maxAddedUndergroundFluids);
        this.undergroundFluids.addAll(undergroundFluids.subList(0, addedUndergroundFluids));
        bytesUsed += addedUndergroundFluids * UndergroundFluidPosition.BYTES;
        return addedUndergroundFluids;
    }

    public void setFirstMessage(boolean isFirstMessage) {
        this.isFirstMessage = isFirstMessage;
    }

    public void setLastMessage(boolean isLastMessage) {
        this.isLastMessage = isLastMessage;
    }

    public int getBytes() {
        return BYTES_OVERHEAD + OreVeinPosition.MAX_BYTES * oreVeins.size()
                + UndergroundFluidPosition.BYTES * undergroundFluids.size();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        isFirstMessage = buf.readByte() > 0;
        isLastMessage = buf.readByte() > 0;

        oreVeins.addAll(VPByteBufUtils.ReadOreVeinPositions(buf));
        undergroundFluids.addAll(VPByteBufUtils.ReadUndergroundFluidPositions(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(isFirstMessage ? 1 : 0);
        buf.writeByte(isLastMessage ? 1 : 0);

        VPByteBufUtils.WriteOreVeinPositions(buf, oreVeins);
        VPByteBufUtils.WriteUndergroundFluidPositions(buf, undergroundFluids);
    }

    public static class ServerHandler implements IMessageHandler<ProspectionSharing, IMessage> {

        private static Map<EntityPlayerMP, List<OreVeinPosition>> oreVeins = new HashMap<>();
        private static Map<EntityPlayerMP, List<UndergroundFluidPosition>> undergroundFluids = new HashMap<>();

        @Override
        public IMessage onMessage(ProspectionSharing message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            // Optional todo: Integrate over time for proper checking
            if (message.getBytes() > VP.uploadSizePerPacketInBytes) {
                player.playerNetServerHandler.kickPlayerFromServer(
                        "Do not spam the server! Change your VisualProcessing configuration back to the servers!");
            }
            if (message.isFirstMessage) {
                oreVeins.put(player, new ArrayList<>());
                undergroundFluids.put(player, new ArrayList<>());
            }
            if (oreVeins.containsKey(player) == false || undergroundFluids.containsKey(player) == false) {
                return null;
            }
            oreVeins.get(player).addAll(message.oreVeins);
            undergroundFluids.get(player).addAll(message.undergroundFluids);
            if (message.isLastMessage) {
                TransferCache.instance.addClientProspectionData(
                        player.getPersistentID().toString(),
                        oreVeins.get(player),
                        undergroundFluids.get(player));
                oreVeins.remove(player);
                undergroundFluids.remove(player);
            }
            return null;
        }
    }

    public static class ClientHandler implements IMessageHandler<ProspectionSharing, IMessage> {

        private static List<OreVeinPosition> oreVeins;
        private static List<UndergroundFluidPosition> undergroundFluids;

        @Override
        public IMessage onMessage(ProspectionSharing message, MessageContext ctx) {
            if (message.isFirstMessage) {
                oreVeins = new ArrayList<>();
                undergroundFluids = new ArrayList<>();
            }
            if (oreVeins == null || undergroundFluids == null) {
                return null;
            }
            oreVeins.addAll(message.oreVeins);
            undergroundFluids.addAll(message.undergroundFluids);
            if (message.isLastMessage) {
                ClientCache.instance.putOreVeins(oreVeins);
                ClientCache.instance.putUndergroundFluids(undergroundFluids);
            }
            return null;
        }
    }
}
