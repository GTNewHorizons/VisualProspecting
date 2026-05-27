package com.sinthoras.visualprospecting.teams;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.network.RequestTeamUploadMessage;

/**
 * {@code /vp_team_upload <player>}: ask {@code <player>}'s client to push its {@code ClientCache} into the team's
 * {@link TeamProspectionData}.
 */
public class TeamUploadCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "vp_team_upload";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return StatCollector.translateToLocal("visualprospecting.teamupload.command");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        if (sender instanceof EntityPlayerMP player) {
            if (Objects.equals(player.mcServer.getServerOwner(), sender.getCommandSenderName())) {
                return true;
            }
        }
        return super.canCommandSenderUseCommand(sender);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
        }
        return Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            sendError(sender, "visualprospecting.command.usage_error", getCommandName());
            return;
        }

        if (!Config.enableTeamSharing) {
            sendError(sender, "visualprospecting.teamcommand.sharing_disabled");
            return;
        }

        EntityPlayerMP target = MinecraftServer.getServer().getConfigurationManager().func_152612_a(args[0]);
        if (target == null) {
            sendError(sender, "visualprospecting.teamcommand.player_offline", args[0]);
            return;
        }

        Team team = TeamManager.getTeamByPlayer(target.getUniqueID());
        if (team == null) {
            sendError(sender, "visualprospecting.teamcommand.no_team", target.getCommandSenderName());
            return;
        }

        VP.network.sendTo(new RequestTeamUploadMessage(), target);

        IChatComponent msg = new ChatComponentTranslation(
                "visualprospecting.teamupload.requested",
                target.getCommandSenderName(),
                team.getTeamName());
        msg.getChatStyle().setColor(EnumChatFormatting.GREEN);
        sender.addChatMessage(msg);
    }

    private static void sendError(ICommandSender sender, String translationKey, Object... args) {
        IChatComponent msg = new ChatComponentTranslation(translationKey, args);
        msg.getChatStyle().setColor(EnumChatFormatting.RED);
        sender.addChatMessage(msg);
    }
}
