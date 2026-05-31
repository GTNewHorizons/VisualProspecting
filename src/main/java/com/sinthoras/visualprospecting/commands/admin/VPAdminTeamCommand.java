package com.sinthoras.visualprospecting.commands.admin;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.util.CommandUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.commands.CommandHelpers;
import com.sinthoras.visualprospecting.commands.common.VPTeamCommand;
import com.sinthoras.visualprospecting.network.RequestTeamUploadMessage;
import com.sinthoras.visualprospecting.teams.TeamProspectionData;

public class VPAdminTeamCommand {

    public static final String ARG_PLAYER = "player";

    private VPAdminTeamCommand() {}

    public static int cmdInfo(CommandContext<ICommandSender> ctx, boolean detailed) {
        ICommandSender sender = ctx.getSource();
        String playerName = StringArgumentType.getString(ctx, ARG_PLAYER);

        EntityPlayerMP target = CommandHelpers.requireOnlinePlayer(sender, playerName);
        if (target == null) {
            return CommandHelpers.FAILURE;
        } else {
            return VPTeamCommand.cmdInfo(ctx, detailed, target);
        }
    }

    public static int cmdClear(CommandContext<ICommandSender> ctx) {
        ICommandSender sender = ctx.getSource();
        String playerName = StringArgumentType.getString(ctx, ARG_PLAYER);

        EntityPlayerMP target = CommandHelpers.requireOnlinePlayer(sender, playerName);
        if (target == null) return CommandHelpers.FAILURE;

        Team team = CommandHelpers.resolveTeam(sender, target);
        if (team == null) return CommandHelpers.FAILURE;

        TeamProspectionData data = CommandHelpers.resolveData(sender, team);
        if (data == null) return CommandHelpers.FAILURE;

        int veins = 0, fluids = 0;
        for (int dim : data.knownDimensions()) {
            veins += data.getDiscoveredVeinKeys(dim).size();
            fluids += data.getDiscoveredFluidKeys(dim).size();
        }

        data.clear();
        team.markDirty();

        return CommandUtils.success(
                sender,
                "visualprospecting.command.vpadmin.team.clear.success",
                team.getTeamName(),
                veins,
                fluids);
    }

    public static int cmdUpload(CommandContext<ICommandSender> ctx) {
        ICommandSender sender = ctx.getSource();
        String playerName = StringArgumentType.getString(ctx, ARG_PLAYER);

        if (!Config.enableTeamSharing) {
            return CommandUtils.error(sender, "visualprospecting.command.sharing_disabled");
        }

        EntityPlayerMP target = CommandHelpers.requireOnlinePlayer(sender, playerName);
        if (target == null) return CommandHelpers.FAILURE;

        Team team = CommandHelpers.resolveTeam(sender, target);
        if (team == null) return CommandHelpers.FAILURE;

        VP.network.sendTo(new RequestTeamUploadMessage(), target);

        return CommandUtils.success(
                sender,
                "visualprospecting.command.vpadmin.team.upload.requested",
                target.getCommandSenderName(),
                team.getTeamName());
    }
}
