package com.sinthoras.visualprospecting.commands;

import static com.gtnewhorizon.gtnhlib.util.CommandUtils.literal;
import static com.sinthoras.visualprospecting.commands.CommandHelpers.sendUsage;

import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.sinthoras.visualprospecting.commands.admin.VPAdminCacheCommand;
import com.sinthoras.visualprospecting.commands.admin.VPAdminDebugCommand;
import com.sinthoras.visualprospecting.commands.admin.VPAdminTeamCommand;

/**
 * {@code /vp_admin} command, available to ops and from console.
 */
public final class VPAdminCommand {

    private VPAdminCommand() {}

    // spotless:off
    public static void register() {
        BrigadierApi.getCommandDispatcher().register(literal("vp_admin")
                // Command root
                .requires(src -> src.canCommandSenderUseCommand(2, "vp_admin"))
                .executes(ctx -> sendUsage(ctx, "visualprospecting.command.vpadmin.root.usage"))

                // Tree for /vp_admin team <commands...>
                .then(literal("team")
                        .executes(ctx -> sendUsage(ctx, "visualprospecting.command.vpadmin.team.usage"))

                        .then(literal("info")
                                .executes(ctx -> sendUsage(ctx, "visualprospecting.command.vpadmin.team.info.usage"))
                                .then(CommandHelpers.playerNameArg(VPAdminTeamCommand.ARG_PLAYER)
                                        .executes(ctx -> VPAdminTeamCommand.cmdInfo(ctx, false))
                                        .then(literal("detailed").executes(ctx -> VPAdminTeamCommand.cmdInfo(ctx, true)))))

                        .then(literal("upload")
                                .executes(ctx -> sendUsage(ctx, "visualprospecting.command.vpadmin.team.upload.usage"))
                                .then(CommandHelpers.playerNameArg(VPAdminTeamCommand.ARG_PLAYER)
                                        .executes(VPAdminTeamCommand::cmdUpload)))

                        .then(literal("clear")
                                .executes(ctx -> sendUsage(ctx, "visualprospecting.command.vpadmin.team.clear.usage"))
                                .then(CommandHelpers.playerNameArg(VPAdminTeamCommand.ARG_PLAYER)
                                        .executes(VPAdminTeamCommand::cmdClear)))
                )

                // Tree for /vp_admin servercache <commands...>
                .then(literal("servercache")
                        .requires(src -> src.canCommandSenderUseCommand(4, "vp_admin"))
                        .executes(ctx -> sendUsage(ctx, "visualprospecting.command.vpadmin.servercache.usage"))

                        .then(literal("rebuild")
                                .executes(ctx -> sendUsage(ctx, "visualprospecting.command.vpadmin.servercache.rebuild.usage"))

                                .then(literal("all")
                                        .executes(VPAdminCacheCommand::cmdRebuildAll))
                                .then(literal("spawn")
                                        .executes(VPAdminCacheCommand::cmdRebuildSpawn)))
                )

                // Tree for /vp_admin debug <commands...>
                .then(literal("debug")
                        .requires(src -> src.canCommandSenderUseCommand(4, "vp_admin"))
                        .executes(ctx -> sendUsage(ctx, "visualprospecting.command.vpadmin.debug.usage"))

                        .then(literal("vein").executes(VPAdminDebugCommand::cmdDebugOreVein))
                )
        );
    }
    // spotless:on
}
