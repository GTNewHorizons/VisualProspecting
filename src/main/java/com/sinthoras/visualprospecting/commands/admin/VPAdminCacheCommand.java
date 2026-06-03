package com.sinthoras.visualprospecting.commands.admin;

import static com.sinthoras.visualprospecting.commands.CommandHelpers.requireServer;

import java.io.IOException;
import java.util.zip.DataFormatException;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import com.mojang.brigadier.context.CommandContext;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.commands.CommandHelpers;
import com.sinthoras.visualprospecting.database.cachebuilder.WorldAnalysis;

public class VPAdminCacheCommand {

    private VPAdminCacheCommand() {}

    public static int cmdRebuildAll(CommandContext<ICommandSender> ctx) {
        return rebuild(ctx.getSource(), "rebuild", (world, server) -> world.cacheVeins());
    }

    public static int cmdRebuildSpawn(CommandContext<ICommandSender> ctx) {
        return rebuild(
                ctx.getSource(),
                "rebuildspawn",
                (world, server) -> world.cacheOverworldSpawnVeins(server.getEntityWorld().getSpawnPoint()));
    }

    private static int rebuild(ICommandSender sender, String kind, RebuildAction action) {
        MinecraftServer server = requireServer(sender);
        if (server == null) return CommandHelpers.FAILURE;

        String prefix = "visualprospecting.command.vpadmin.servercache." + kind;
        CommandHelpers.plain(sender, prefix + ".start");
        try {
            WorldAnalysis world = new WorldAnalysis(server.getEntityWorld().getSaveHandler().getWorldDirectory());
            action.run(world, server);
        } catch (IOException | DataFormatException e) {
            VP.LOG.error("Could not rebuild vein cache ({})", kind, e);
            return CommandHelpers.error(sender, prefix + ".failure");
        }
        return CommandHelpers.success(sender, prefix + ".confirmation");
    }

    @FunctionalInterface
    private interface RebuildAction {

        void run(WorldAnalysis world, MinecraftServer server) throws IOException, DataFormatException;
    }
}
