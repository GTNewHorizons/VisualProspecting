package com.sinthoras.visualprospecting.teams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamEvents.TeamMergeEvent;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.network.TeamCatchupNotification;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * On player login, push the player's team's complete prospection record to their client by reconstructing all payloads
 * from {@link ServerCache} and sending them via {@link TeamCatchupNotification} packets.
 * <p>
 * Allow for player to catch up to what happened in the team when they were offline.
 * <p>
 * Data is split into separate batched notifications.
 */
@EventBusSubscriber
public final class TeamCatchupHandler {

    // Conservative caps for one ProspectingNotification:
    // ~20 B per vein -> 1000 veins ~= 20 KB per packet
    // ~272 B per fluid -> 100 fluids ~= 27 KB per packet
    private static final int VEINS_PER_PACKET = 1000;
    private static final int FLUIDS_PER_PACKET = 100;

    private static final List<OreVeinPosition> EMPTY_VEINS = Collections.emptyList();
    private static final List<UndergroundFluidPosition> EMPTY_FLUIDS = Collections.emptyList();

    private TeamCatchupHandler() {}

    // Need to run after GTNHLib or we may have no team to load
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (!Config.enableTeamSharing) return;
        if (!(event.player instanceof EntityPlayerMP playerMP)) return;
        if (playerMP.worldObj.isRemote) return;
        resyncPlayer(playerMP);
    }

    public static void resyncPlayer(EntityPlayerMP player) {
        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return;
        TeamProspectionData data = (TeamProspectionData) team.getData(TeamProspectionData.DATA_KEY);
        if (data == null) return;
        sendCatchup(player, data);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTeamMerge(TeamMergeEvent event) {
        if (!Config.enableTeamSharing) return;

        Team surviving = event.surviving;
        if (surviving == null) return;

        TeamProspectionData data = (TeamProspectionData) surviving.getData(TeamProspectionData.DATA_KEY);
        if (data == null) return;

        TeamManager.forEachOnlineTeamMember(surviving, member -> sendCatchup(member, data));
    }

    private static void sendCatchup(EntityPlayerMP player, TeamProspectionData data) {
        IntSet dims = data.knownDimensions();
        if (dims.isEmpty()) return;

        for (int dim : dims) {
            sendDimVeins(player, data, dim);
            sendDimFluids(player, data, dim);
        }
    }

    private static void sendDimVeins(EntityPlayerMP player, TeamProspectionData data, int dim) {
        LongSet veinKeys = data.getDiscoveredVeinKeys(dim);
        if (veinKeys.isEmpty()) return;

        LongSet depletedKeys = data.getDepletedVeinKeys(dim);
        List<OreVeinPosition> batch = new ArrayList<>(Math.min(VEINS_PER_PACKET, veinKeys.size()));

        for (LongIterator it = veinKeys.iterator(); it.hasNext();) {
            long key = it.nextLong();
            int chunkX = TeamProspectionData.veinChunkX(key);
            int chunkZ = TeamProspectionData.veinChunkZ(key);

            OreVeinPosition vein = ServerCache.instance.getOreVein(dim, chunkX, chunkZ);
            if (vein == null || vein.veinType == VeinType.NO_VEIN) {
                continue;
            }

            boolean teamDepleted = depletedKeys.contains(key);
            if (vein.isDepleted() != teamDepleted) {
                vein = new OreVeinPosition(dim, chunkX, chunkZ, vein.veinType, teamDepleted);
            }
            batch.add(vein);

            if (batch.size() >= VEINS_PER_PACKET) {
                flushVeins(player, batch);
                batch = new ArrayList<>(VEINS_PER_PACKET);
            }
        }
        flushVeins(player, batch);
    }

    private static void flushVeins(EntityPlayerMP player, List<OreVeinPosition> batch) {
        if (batch.isEmpty()) return;
        VP.network.sendTo(new TeamCatchupNotification(batch, EMPTY_FLUIDS), player);
    }

    private static void sendDimFluids(EntityPlayerMP player, TeamProspectionData data, int dim) {
        LongSet fluidKeys = data.getDiscoveredFluidKeys(dim);
        if (fluidKeys.isEmpty()) return;

        World world = DimensionManager.getWorld(dim);
        // Can't retrieve fluid if dim not loaded.
        if (world == null) return;

        List<UndergroundFluidPosition> batch = new ArrayList<>(Math.min(FLUIDS_PER_PACKET, fluidKeys.size()));

        for (LongIterator it = fluidKeys.iterator(); it.hasNext();) {
            long key = it.nextLong();
            int chunkX = TeamProspectionData.fluidChunkX(key);
            int chunkZ = TeamProspectionData.fluidChunkZ(key);
            int blockX = Utils.coordChunkToBlock(chunkX);
            int blockZ = Utils.coordChunkToBlock(chunkZ);

            List<UndergroundFluidPosition> foundUndergroundFluids = ServerCache.instance
                    .prospectUndergroundFluidBlockRadius(world, blockX, blockZ, 0);
            for (UndergroundFluidPosition fp : foundUndergroundFluids) {
                if (!fp.isProspected()) continue;
                batch.add(fp);
                if (batch.size() >= FLUIDS_PER_PACKET) {
                    flushFluids(player, batch);
                    batch = new ArrayList<>(FLUIDS_PER_PACKET);
                }
            }
        }
        flushFluids(player, batch);
    }

    private static void flushFluids(EntityPlayerMP player, List<UndergroundFluidPosition> batch) {
        if (batch.isEmpty()) return;
        VP.network.sendTo(new TeamCatchupNotification(EMPTY_VEINS, batch), player);
    }
}
