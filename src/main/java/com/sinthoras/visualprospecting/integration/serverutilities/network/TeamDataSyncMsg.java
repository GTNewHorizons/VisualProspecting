package com.sinthoras.visualprospecting.integration.serverutilities.network;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.integration.serverutilities.database.ForgeTeamCache;
import com.sinthoras.visualprospecting.integration.serverutilities.database.ForgeTeamDb;
import com.sinthoras.visualprospecting.utils.VPByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;

public class TeamDataSyncMsg implements IMessage {

    protected final List<OreVeinPosition> oreVeins = new ArrayList<>();
    protected final List<UndergroundFluidPosition> undergroundFluids = new ArrayList<>();
    private int byteSize = 2 * Integer.BYTES;

    /**
     * @return number of items added, may be less than size of list given.
     */
    public int addOreVeins(List<OreVeinPosition> oreVeins) {
        final int oreCapacity = getRemainingOreVeinCapacity();
        final int consumed = Math.min(oreVeins.size(), oreCapacity);

        this.oreVeins.addAll(oreVeins.subList(0, consumed));
        byteSize += consumed * OreVeinPosition.getMaxBytes();

        return consumed;
    }

    /**
     * @return number of items added, may be less than size of list given.
     */
    public int addUndergroundFluids(List<UndergroundFluidPosition> undergroundFluids) {
        final int fluidCapacity = getRemainingUndergroundFluidCapacity();
        final int consumed = Math.min(undergroundFluids.size(), fluidCapacity);

        this.undergroundFluids.addAll(undergroundFluids.subList(0, consumed));
        byteSize += consumed * UndergroundFluidPosition.BYTES;

        return consumed;
    }

    public int getRemainingByteCapacity() {
        return VP.uploadSizePerPacketInBytes - this.byteSize;
    }

    public int getRemainingOreVeinCapacity() {
        return getRemainingByteCapacity() / OreVeinPosition.getMaxBytes();
    }

    public int getRemainingUndergroundFluidCapacity() {
        return getRemainingByteCapacity() / UndergroundFluidPosition.BYTES;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        oreVeins.addAll(VPByteBufUtils.ReadOreVeinPositions(buf));
        undergroundFluids.addAll(VPByteBufUtils.ReadUndergroundFluidPositions(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        VPByteBufUtils.WriteOreVeinPositions(buf, oreVeins);
        VPByteBufUtils.WriteUndergroundFluidPositions(buf, undergroundFluids);
    }

    // When the client sends a full team sync to the server.
    public static class ServerHandler implements IMessageHandler<TeamDataSyncMsg, IMessage> {

        @Override
        public IMessage onMessage(TeamDataSyncMsg message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            ForgeTeamCache teamCache = ForgeTeamDb.instance.getByPlayer(player);
            if (teamCache == null) return null; // Player sent a full team sync to the server despite not being on a team, in general this shouldn't be possible, but handling it just in case.

            teamCache.putOreVeins(message.oreVeins);
            teamCache.putUndergroundFluids(message.undergroundFluids);

            return null;
        }
    }

    // Server sends a team sync containing data that the client is missing.
    public static class ClientHandler implements IMessageHandler<TeamDataSyncMsg, IMessage> {

        @Override
        public IMessage onMessage(TeamDataSyncMsg message, MessageContext ctx) {
            ClientCache.instance.putOreVeins(message.oreVeins);
            ClientCache.instance.putUndergroundFluids(message.undergroundFluids);

            return null;
        }
    }
}
