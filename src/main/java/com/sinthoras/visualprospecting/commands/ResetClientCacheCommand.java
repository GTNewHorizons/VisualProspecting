package com.sinthoras.visualprospecting.commands;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import com.sinthoras.visualprospecting.database.ClientCache;

public class ResetClientCacheCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "vp_client_cache_reset";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return I18n.format("visualprospecting.command.client.resetcache.usage");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] parameters) {
        ClientCache.instance.resetPlayerProgression();
        final IChatComponent confirmation = new ChatComponentTranslation(
                "visualprospecting.command.client.resetcache.confirmation");
        confirmation.getChatStyle().setItalic(true);
        sender.addChatMessage(confirmation);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
