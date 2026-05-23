package com.sinthoras.visualprospecting.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.teams.TeamProspectionDispatcher;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class VeinDepletionMessage implements IMessage {

    private int dimensionId;
    private int chunkX;
    private int chunkZ;
    private boolean depleted;

    public VeinDepletionMessage() {}

    public VeinDepletionMessage(int dimensionId, int chunkX, int chunkZ, boolean depleted) {
        this.dimensionId = dimensionId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.depleted = depleted;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimensionId = buf.readInt();
        chunkX = buf.readInt();
        chunkZ = buf.readInt();
        depleted = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimensionId);
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
        buf.writeBoolean(depleted);
    }

    public static class ServerHandler implements IMessageHandler<VeinDepletionMessage, IMessage> {

        @Override
        public IMessage onMessage(VeinDepletionMessage msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            TeamProspectionDispatcher
                    .handleDepletionToggle(player, msg.dimensionId, msg.chunkX, msg.chunkZ, msg.depleted);
            return null;
        }
    }

    public static class ClientHandler implements IMessageHandler<VeinDepletionMessage, IMessage> {

        @Override
        public IMessage onMessage(VeinDepletionMessage msg, MessageContext ctx) {
            OreVeinPosition vein = ClientCache.instance.getOreVein(msg.dimensionId, msg.chunkX, msg.chunkZ);
            if (vein == null || vein == OreVeinPosition.EMPTY_VEIN) return null;
            if (vein.isDepleted() != msg.depleted) {
                vein.toggleDepleted();
            }
            return null;
        }
    }
}
