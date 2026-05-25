package com.sinthoras.visualprospecting.teams;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * {@code /vp_team_info [player] [detailed]} : inspect a player team's prospection record.
 * <p>
 * Require op to inspect other players.
 */
public class TeamInfoCommand extends CommandBase {

    private static final String DETAILED_FLAG = "detailed";

    @Override
    public String getCommandName() {
        return "vp_team_info";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return StatCollector.translateToLocal("visualprospecting.teaminfo.command");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            String[] names = MinecraftServer.getServer().getAllUsernames();
            String[] options = Arrays.copyOf(names, names.length + 1);
            options[names.length] = DETAILED_FLAG;
            return getListOfStringsMatchingLastWord(args, options);
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, DETAILED_FLAG);
        }
        return Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        boolean detailed = false;
        String targetPlayerName = null;
        for (String arg : args) {
            if (arg.equalsIgnoreCase(DETAILED_FLAG)) {
                detailed = true;
            } else if (targetPlayerName == null) {
                targetPlayerName = arg;
            } else {
                sendMessage(sender, "visualprospecting.command.usage_error", getCommandName());
                return;
            }
        }

        EntityPlayerMP targetPlayer = resolveTarget(sender, targetPlayerName);
        if (targetPlayer == null) return;

        Team team = TeamManager.getTeamByPlayer(targetPlayer.getUniqueID());
        if (team == null) {
            sendMessage(sender, "visualprospecting.teamcommand.no_team", targetPlayer.getCommandSenderName());
            return;
        }

        TeamProspectionData data = (TeamProspectionData) team.getData(TeamProspectionData.DATA_KEY);
        if (data == null) {
            sendMessage(sender, "visualprospecting.teaminfo.no_data");
            return;
        }

        printReport(sender, team, data, detailed);
    }

    private EntityPlayerMP resolveTarget(ICommandSender sender, String targetName) {
        if (targetName == null) {
            if (sender instanceof EntityPlayerMP self) return self;
            sendMessage(sender, "visualprospecting.teamcommand.console_needs_player");
            return null;
        }

        // Permission check
        if (!sender.canCommandSenderUseCommand(2, getCommandName())) {
            sendMessage(sender, "visualprospecting.teaminfo.no_permission_other");
            return null;
        }

        EntityPlayerMP target = MinecraftServer.getServer().getConfigurationManager().func_152612_a(targetName);
        if (target == null) {
            sendMessage(sender, "visualprospecting.teamcommand.player_offline", targetName);
            return null;
        }
        return target;
    }

    private static void printReport(ICommandSender sender, Team team, TeamProspectionData data, boolean detailed) {
        IntSet dims = data.knownDimensions();

        long totalVeins = 0, totalFluids = 0, totalDepleted = 0;
        for (int dim : dims) {
            totalVeins += data.getDiscoveredVeinKeys(dim).size();
            totalFluids += data.getDiscoveredFluidKeys(dim).size();
            totalDepleted += data.getDepletedVeinKeys(dim).size();
        }

        int memberCount = team.getMembers().size();
        int onlineCount = countOnline(team);

        IChatComponent header = new ChatComponentTranslation(
                "visualprospecting.teaminfo.header",
                team.getTeamName(),
                memberCount,
                onlineCount);
        header.getChatStyle().setBold(true);
        sender.addChatMessage(header);

        if (dims.isEmpty()) {
            IChatComponent empty = new ChatComponentTranslation("visualprospecting.teaminfo.empty");
            empty.getChatStyle().setColor(EnumChatFormatting.GRAY);
            sender.addChatMessage(empty);
            return;
        }

        sender.addChatMessage(
                new ChatComponentTranslation(
                        "visualprospecting.teaminfo.summary",
                        dims.size(),
                        totalVeins,
                        totalDepleted,
                        totalFluids));

        if (!detailed) return;

        int[] sortedDims = dims.toIntArray();
        Arrays.sort(sortedDims);
        for (int dim : sortedDims) {
            int v = data.getDiscoveredVeinKeys(dim).size();
            int f = data.getDiscoveredFluidKeys(dim).size();
            int d = data.getDepletedVeinKeys(dim).size();
            if (v == 0 && f == 0) continue;
            sender.addChatMessage(
                    new ChatComponentTranslation("visualprospecting.teaminfo.dim_line", dimLabel(dim), v, d, f));
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

    private static void sendMessage(ICommandSender sender, String translationKey, Object... args) {
        IChatComponent msg = new ChatComponentTranslation(translationKey, args);
        msg.getChatStyle().setColor(EnumChatFormatting.RED);
        sender.addChatMessage(msg);
    }
}
