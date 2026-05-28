package com.sinthoras.visualprospecting.network;

import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.task.SnapshotUploadTask;
import com.sinthoras.visualprospecting.task.TaskManager;
import com.sinthoras.visualprospecting.teams.TeamProspectionData;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Server → client trigger: ask the recipient to push their {@link ClientCache} to the server, routed into their team's
 * {@link TeamProspectionData}.
 */
public class RequestTeamUploadMessage implements IMessage {

    public RequestTeamUploadMessage() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class ClientHandler implements IMessageHandler<RequestTeamUploadMessage, IMessage> {

        @Override
        public IMessage onMessage(RequestTeamUploadMessage message, MessageContext ctx) {
            TaskManager.CLIENT_INSTANCE.addTask(new SnapshotUploadTask(true));
            return null;
        }
    }
}
