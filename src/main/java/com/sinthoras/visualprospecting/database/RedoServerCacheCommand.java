package com.sinthoras.visualprospecting.database;

import java.io.IOException;
import java.util.Objects;
import java.util.zip.DataFormatException;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.cachebuilder.WorldAnalysis;

import cpw.mods.fml.server.FMLServerHandler;

public class RedoServerCacheCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "visualprospectingredoservercache";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return StatCollector.translateToLocal("visualprospecting.redoservercache.command");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] parameters) {
        MinecraftServer server;
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            server = player.mcServer;
        } else {
            server = FMLServerHandler.instance().getServer();
        }
        if (server == null) {
            VP.info("Server not found");
            return;
        }
        final IChatComponent start = new ChatComponentTranslation("visualprospecting.redoservercache.start");
        start.getChatStyle().setItalic(true);
        sender.addChatMessage(start);
        try {
            WorldAnalysis world = new WorldAnalysis(server.getEntityWorld().getSaveHandler().getWorldDirectory());
            world.cacheVeins();
        } catch (IOException | DataFormatException e) {
            VP.info("Could not load world save files to build vein cache!");
            e.printStackTrace();
            final IChatComponent failure = new ChatComponentTranslation("visualprospecting.redoservercache.failure");
            failure.getChatStyle().setItalic(true);
            sender.addChatMessage(failure);
        }
        final IChatComponent confirmation = new ChatComponentTranslation(
                "visualprospecting.redoservercache.confirmation");
        confirmation.getChatStyle().setItalic(true);
        sender.addChatMessage(confirmation);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        // Only SP owner, or OP are allowed
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            if (Objects.equals(player.mcServer.getServerOwner(), sender.getCommandSenderName())) {
                return true;
            }
        }
        return super.canCommandSenderUseCommand(sender);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }
}
