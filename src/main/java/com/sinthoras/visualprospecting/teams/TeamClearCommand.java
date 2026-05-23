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

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * {@code /vp_team_clear <player>} : Delete that player team prospection record.
 * <p>
 * Op/console only.
 * <p>
 * Note: players local {@code ClientCache} is untouched.
 */
public class TeamClearCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "vp_team_clear";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return StatCollector.translateToLocal("visualprospecting.teamclear.command");
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
            sendMessage(sender, "visualprospecting.command.usage_error", getCommandName());
            return;
        }

        EntityPlayerMP target = MinecraftServer.getServer().getConfigurationManager().func_152612_a(args[0]);
        if (target == null) {
            sendMessage(sender, "visualprospecting.teamcommand.player_offline", args[0]);
            return;
        }

        Team team = TeamManager.getTeamByPlayer(target.getUniqueID());
        if (team == null) {
            sendMessage(sender, "visualprospecting.teamcommand.no_team", target.getCommandSenderName());
            return;
        }

        TeamProspectionData data = (TeamProspectionData) team.getData(TeamProspectionData.DATA_KEY);
        if (data == null) {
            sendMessage(sender, "visualprospecting.teaminfo.no_data");
            return;
        }

        IntSet dims = data.knownDimensions();
        long totalVeins = 0, totalFluids = 0;
        for (int dim : dims) {
            totalVeins += data.getDiscoveredVeinKeys(dim).size();
            totalFluids += data.getDiscoveredFluidKeys(dim).size();
        }

        data.clear();
        team.markDirty();

        IChatComponent msg = new ChatComponentTranslation(
                "visualprospecting.teamclear.success",
                team.getTeamName(),
                totalVeins,
                totalFluids);
        msg.getChatStyle().setColor(EnumChatFormatting.GREEN);
        sender.addChatMessage(msg);
    }

    private static void sendMessage(ICommandSender sender, String translationKey, Object... args) {
        IChatComponent msg = new ChatComponentTranslation(translationKey, args);
        msg.getChatStyle().setColor(EnumChatFormatting.RED);
        sender.addChatMessage(msg);
    }
}
