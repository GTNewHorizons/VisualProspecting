package com.sinthoras.visualprospecting.commands;

import static com.gtnewhorizon.gtnhlib.util.CommandUtils.literal;
import static com.sinthoras.visualprospecting.commands.CommandHelpers.sendUsage;

import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.sinthoras.visualprospecting.commands.common.VPTeamCommand;

/**
 * {@code /vp} command, available to all players.
 */
public final class VPCommand {

    private VPCommand() {}

    // spotless:off
    public static void register() {
        BrigadierApi.getCommandDispatcher().register(literal("vp")
                .executes(ctx -> sendUsage(ctx, "visualprospecting.command.vp.root.usage"))

                .then(literal("team").executes(ctx -> sendUsage(ctx, "visualprospecting.command.vp.team.usage"))

                        .then(literal("info").executes(ctx -> VPTeamCommand.cmdInfo(ctx, false))
                                .then(literal("detailed").executes(ctx -> VPTeamCommand.cmdInfo(ctx, true)))
        )));
    }
    // spotless:on
}
