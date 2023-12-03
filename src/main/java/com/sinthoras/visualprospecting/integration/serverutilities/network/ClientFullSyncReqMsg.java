package com.sinthoras.visualprospecting.integration.serverutilities.network;

import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.integration.serverutilities.task.ClientSyncTaskBatcher;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class ClientFullSyncReqMsg implements IMessage {
    @Override
    public void fromBytes(ByteBuf buf) { }

    @Override
    public void toBytes(ByteBuf buf) { }

    public static class ClientHandler implements IMessageHandler<ClientFullSyncReqMsg, IMessage> {

        @Override
        public IMessage onMessage(ClientFullSyncReqMsg message, MessageContext ctx) {
            ClientSyncTaskBatcher.instance.addOreVeins(ClientCache.instance.getAllOreVeins());
            ClientSyncTaskBatcher.instance.addUndergroundFluids(ClientCache.instance.getAllUndergroundFluids());

            return null;
        }
    }
}
