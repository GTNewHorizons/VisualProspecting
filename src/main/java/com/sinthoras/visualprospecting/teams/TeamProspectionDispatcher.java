package com.sinthoras.visualprospecting.teams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.network.ProspectingNotification;
import com.sinthoras.visualprospecting.network.VeinDepletionMessage;

/**
 * Entry point for delivering prospection results from the server to the player.
 * <p>
 * If team sharing is enabled, store results in the team's {@link TeamProspectionData} then broadcast to online
 * teammates.
 */
public final class TeamProspectionDispatcher {

    private TeamProspectionDispatcher() {}

    /**
     * Send prospection results to {@code player}, then route through team sharing.
     *
     * @param player   the player whose action produced these results
     * @param oreVeins ore veins to deliver. May be empty; must not be {@code null}.
     * @param fluids   underground fluid positions to deliver. May be empty; must not be {@code null}.
     */
    public static void deliverProspectingResults(EntityPlayerMP player, List<OreVeinPosition> oreVeins,
            List<UndergroundFluidPosition> fluids) {
        deliverProspectingResults(player, new ProspectingNotification(oreVeins, fluids), true);
    }

    public static void deliverProspectingResults(EntityPlayerMP player, ProspectingNotification notification) {
        deliverProspectingResults(player, notification, true);
    }

    /**
     * Send prospection results to {@code player}'s team through UUID.
     *
     * @param player   the player's team whose action produced these results
     * @param oreVeins ore veins to deliver. May be empty; must not be {@code null}.
     * @param fluids   underground fluid positions to deliver. May be empty; must not be {@code null}.
     */
    public static void deliverProspectingResults(UUID player, List<OreVeinPosition> oreVeins,
            List<UndergroundFluidPosition> fluids) {
        deliverProspectingResults(player, new ProspectingNotification(oreVeins, fluids));
    }

    public static void deliverProspectingResults(UUID player, ProspectingNotification notification) {
        if (player == null) return;
        if (!Config.enableTeamSharing) {
            EntityPlayerMP onlinePlayer = getOnlinePlayer(player);
            if (onlinePlayer != null) VP.network.sendTo(notification, onlinePlayer);
            return;
        }
        shareProspectingResults(player, notification, null);
    }

    public static void deliverProspectingResults(EntityPlayerMP player, ProspectingNotification notification,
            boolean notifySelf) {
        if (player == null || player.playerNetServerHandler == null) return;

        if (notifySelf) {
            VP.network.sendTo(notification, player);
        }

        shareProspectingResults(player.getUniqueID(), notification, player.getUniqueID());
    }

    /**
     * Manage vein depletion toggle sent by {@code player} from a map GUI.
     * <p>
     * Toggles are only accepted for veins the team has actually discovered.
     */
    public static void handleDepletionToggle(EntityPlayerMP player, int dim, int chunkX, int chunkZ, boolean depleted) {
        updateTeamDepletion(player.getUniqueID(), dim, chunkX, chunkZ, depleted, player.getUniqueID());
    }

    public static void handleDepletionToggle(UUID player, int dim, int chunkX, int chunkZ, boolean depleted) {
        if (!Config.enableTeamSharing) {
            EntityPlayerMP onlinePlayer = getOnlinePlayer(player);
            if (onlinePlayer != null)
                VP.network.sendTo(new VeinDepletionMessage(dim, chunkX, chunkZ, depleted), onlinePlayer);
            return;
        }
        updateTeamDepletion(player, dim, chunkX, chunkZ, depleted, null);
    }

    private static void shareProspectingResults(UUID player, ProspectingNotification notification,
            UUID excludedPlayer) {
        if (!Config.enableTeamSharing) return;

        Team team = TeamManager.getTeamByPlayer(player);
        if (team == null) return;
        TeamProspectionData data = (TeamProspectionData) team.getData(TeamProspectionData.DATA_KEY);
        if (data == null) return;

        List<OreVeinPosition> newVeins = filterNewVeins(data, notification.getOreVeins());
        List<UndergroundFluidPosition> newFluids = filterNewFluids(data, notification.getUndergroundFluids());
        if (newVeins.isEmpty() && newFluids.isEmpty()) return;

        team.markDirty();

        ProspectingNotification broadcast = new ProspectingNotification(newVeins, newFluids);
        TeamManager.forEachOnlineTeamMember(team, member -> {
            if (excludedPlayer == null || !member.getUniqueID().equals(excludedPlayer)) {
                VP.network.sendTo(broadcast, member);
            }
        });
    }

    private static void updateTeamDepletion(UUID player, int dim, int chunkX, int chunkZ, boolean depleted,
            UUID excludedPlayer) {
        if (!Config.enableTeamSharing) return;

        Team team = TeamManager.getTeamByPlayer(player);
        if (team == null) return;
        TeamProspectionData data = (TeamProspectionData) team.getData(TeamProspectionData.DATA_KEY);
        if (data == null || !data.isVeinDiscovered(dim, chunkX, chunkZ)) return;

        if (!data.setVeinDepleted(dim, chunkX, chunkZ, depleted)) return;

        team.markDirty();

        VeinDepletionMessage broadcast = new VeinDepletionMessage(dim, chunkX, chunkZ, depleted);
        TeamManager.forEachOnlineTeamMember(team, member -> {
            if (excludedPlayer == null || !member.getUniqueID().equals(excludedPlayer)) {
                VP.network.sendTo(broadcast, member);
            }
        });
    }

    private static EntityPlayerMP getOnlinePlayer(UUID player) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return null;
        for (EntityPlayerMP onlinePlayer : server.getConfigurationManager().playerEntityList) {
            if (onlinePlayer.getUniqueID().equals(player) && onlinePlayer.playerNetServerHandler != null) {
                return onlinePlayer;
            }
        }
        return null;
    }

    private static List<OreVeinPosition> filterNewVeins(TeamProspectionData data, List<OreVeinPosition> veins) {
        if (veins == null || veins.isEmpty()) return Collections.emptyList();
        List<OreVeinPosition> newVeins = new ArrayList<>(veins.size());
        for (OreVeinPosition v : veins) {
            if (v == null || v.veinType == VeinType.NO_VEIN) continue;
            if (data.addVein(v.dimensionId, v.chunkX, v.chunkZ)) {
                newVeins.add(v);
            }
        }
        return newVeins;
    }

    private static List<UndergroundFluidPosition> filterNewFluids(TeamProspectionData data,
            List<UndergroundFluidPosition> fluids) {
        if (fluids == null || fluids.isEmpty()) return Collections.emptyList();
        List<UndergroundFluidPosition> newFluids = new ArrayList<>(fluids.size());
        for (UndergroundFluidPosition f : fluids) {
            if (f == null || !f.isProspected()) continue;
            if (data.addFluid(f.dimensionId, f.chunkX, f.chunkZ)) {
                newFluids.add(f);
            }
        }
        return newFluids;
    }
}
