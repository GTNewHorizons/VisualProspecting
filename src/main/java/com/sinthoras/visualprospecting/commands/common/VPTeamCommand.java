package com.sinthoras.visualprospecting.commands.common;

import java.util.Arrays;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;
import com.gtnewhorizon.gtnhlib.util.CommandUtils;
import com.mojang.brigadier.context.CommandContext;
import com.sinthoras.visualprospecting.commands.CommandHelpers;
import com.sinthoras.visualprospecting.teams.TeamProspectionData;

import it.unimi.dsi.fastutil.ints.IntSet;

public final class VPTeamCommand {

    private VPTeamCommand() {}

    public static int cmdInfo(CommandContext<ICommandSender> ctx, boolean detailed) {
        return cmdInfo(ctx, detailed, null);
    }

    public static int cmdInfo(CommandContext<ICommandSender> ctx, boolean detailed, EntityPlayerMP target) {
        ICommandSender sender = ctx.getSource();

        EntityPlayerMP subject = target;
        if (subject == null) {
            if (!(sender instanceof EntityPlayerMP self)) {
                return CommandUtils.error(sender, "visualprospecting.command.console_needs_player");
            }
            subject = self;
        }

        Team team = CommandHelpers.resolveTeam(sender, subject);
        if (team == null) return CommandHelpers.FAILURE;

        TeamProspectionData data = CommandHelpers.resolveData(sender, team);
        if (data == null) return CommandHelpers.FAILURE;

        printInfoReport(sender, team, data, detailed);
        return CommandHelpers.SUCCESS;
    }

    private static void printInfoReport(ICommandSender sender, Team team, TeamProspectionData data, boolean detailed) {
        IntSet knownDimensions = data.knownDimensions();

        long totalVeins = 0, totalFluids = 0, totalDepleted = 0;
        for (int dim : knownDimensions) {
            totalVeins += data.getDiscoveredVeinKeys(dim).size();
            totalFluids += data.getDiscoveredFluidKeys(dim).size();
            totalDepleted += data.getDepletedVeinKeys(dim).size();
        }

        int memberCount = team.getMembers().size();
        int onlineCount = countOnline(team);

        IChatComponent header = new ChatComponentTranslation(
                "visualprospecting.command.vp.team.info.header",
                team.getTeamName(),
                memberCount,
                onlineCount);
        header.getChatStyle().setBold(true);
        sender.addChatMessage(header);

        if (knownDimensions.isEmpty()) {
            IChatComponent empty = new ChatComponentTranslation("visualprospecting.command.vp.team.info.empty");
            empty.getChatStyle().setColor(EnumChatFormatting.GRAY);
            sender.addChatMessage(empty);
            return;
        }

        sender.addChatMessage(
                new ChatComponentTranslation(
                        "visualprospecting.command.vp.team.info.summary",
                        knownDimensions.size(),
                        totalVeins,
                        totalDepleted,
                        totalFluids));

        if (!detailed) return;

        int[] sortedDims = knownDimensions.toIntArray();
        Arrays.sort(sortedDims);
        for (int dim : sortedDims) {
            int v = data.getDiscoveredVeinKeys(dim).size();
            int f = data.getDiscoveredFluidKeys(dim).size();
            int d = data.getDepletedVeinKeys(dim).size();
            if (v == 0 && f == 0) continue;
            sender.addChatMessage(
                    new ChatComponentTranslation(
                            "visualprospecting.command.vp.team.info.dim_line",
                            dimLabel(dim),
                            v,
                            d,
                            f));
        }
    }

    private static int countOnline(Team team) {
        int[] count = { 0 };
        TeamManager.forEachOnlineTeamMember(team, p -> count[0]++);
        return count[0];
    }

    private static String dimLabel(int dim) {
        try {
            World world = DimensionManager.getWorld(dim);
            if (world != null && world.provider != null) {
                String name = world.provider.getDimensionName();
                if (name != null && !name.isEmpty()) {
                    return dim + " (" + name + ")";
                }
            }
        } catch (Exception ignored) {}
        return Integer.toString(dim);
    }
}
