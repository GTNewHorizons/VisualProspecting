package com.sinthoras.visualprospecting.teams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

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
        // Notify the originating player.
        VP.network.sendTo(new ProspectingNotification(oreVeins, fluids), player);

        if (!Config.enableTeamSharing) return;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return;
        TeamProspectionData data = (TeamProspectionData) team.getData(TeamProspectionData.DATA_KEY);
        if (data == null) return;

        // Update the team's record
        List<OreVeinPosition> newVeins = filterNewVeins(data, oreVeins);
        List<UndergroundFluidPosition> newFluids = filterNewFluids(data, fluids);
        if (newVeins.isEmpty() && newFluids.isEmpty()) return;

        team.markDirty();

        // No one to broadcast to if solo team
        if (team.getMembers().size() <= 1) return;

        // Broadcast to other online teammates.
        ProspectingNotification broadcast = new ProspectingNotification(newVeins, newFluids);
        TeamManager.forEachOnlineTeamMember(team, member -> {
            if (!member.getUniqueID().equals(player.getUniqueID())) {
                VP.network.sendTo(broadcast, member);
            }
        });
    }

    /**
     * Manage vein depletion toggle sent by {@code player} from a map GUI.
     * <p>
     * Toggles are only accepted for veins the team has actually discovered.
     */
    public static void handleDepletionToggle(EntityPlayerMP player, int dim, int chunkX, int chunkZ, boolean depleted) {
        if (!Config.enableTeamSharing) return;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return;
        TeamProspectionData data = (TeamProspectionData) team.getData(TeamProspectionData.DATA_KEY);
        if (data == null) return;

        // Require the vein to be in the team's discovered set.
        if (!data.isVeinDiscovered(dim, chunkX, chunkZ)) return;

        // Stop here if the team's depletion state already matches.
        if (!data.setVeinDepleted(dim, chunkX, chunkZ, depleted)) return;

        team.markDirty();

        // Broadcast to other online teammates.
        VeinDepletionMessage broadcast = new VeinDepletionMessage(dim, chunkX, chunkZ, depleted);
        TeamManager.forEachOnlineTeamMember(team, member -> {
            if (!member.getUniqueID().equals(player.getUniqueID())) {
                VP.network.sendTo(broadcast, member);
            }
        });
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
