package com.sinthoras.visualprospecting.commands;

import static com.gtnewhorizon.gtnhlib.util.CommandUtils.argument;

import java.util.concurrent.CompletableFuture;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;
import com.gtnewhorizon.gtnhlib.util.CommandUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sinthoras.visualprospecting.teams.TeamProspectionData;

public final class CommandHelpers {

    public static final int SUCCESS = Command.SINGLE_SUCCESS;
    public static final int FAILURE = 0;

    private CommandHelpers() {}

    public static RequiredArgumentBuilder<ICommandSender, String> playerNameArg(String name) {
        return argument(name, StringArgumentType.word()).suggests(CommandHelpers::suggestOnlinePlayers);
    }

    public static EntityPlayerMP requireOnlinePlayer(ICommandSender sender, String name) {
        EntityPlayerMP target = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);
        if (target == null) {
            CommandUtils.error(sender, "visualprospecting.command.player_offline", name);
        }
        return target;
    }

    public static Team resolveTeam(ICommandSender sender, EntityPlayerMP player) {
        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) {
            CommandUtils.error(sender, "visualprospecting.command.vp.team.no_team", player.getCommandSenderName());
        }
        return team;
    }

    public static TeamProspectionData resolveData(ICommandSender sender, Team team) {
        TeamProspectionData data = (TeamProspectionData) team.getData(TeamProspectionData.DATA_KEY);
        if (data == null) {
            CommandUtils.error(sender, "visualprospecting.command.vp.team.no_data");
        }
        return data;
    }

    public static int sendUsage(CommandContext<ICommandSender> ctx, String translationKey) {
        return plain(ctx.getSource(), translationKey);
    }

    private static int plain(ICommandSender sender, String translationKey, Object... args) {
        sender.addChatMessage(new ChatComponentTranslation(translationKey, args));
        return SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<ICommandSender> ctx,
            SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (String name : MinecraftServer.getServer().getAllUsernames()) {
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }
}
