package com.sinthoras.visualprospecting.teams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Lazily push the team's prospection record to a player, one dimension at a time, as they enter it. Only catchup once
 * per dimension per session as the rest is done live.
 * <p>
 * Allow for player to catch up to what happened in the team when they were offline.
 * <p>
 * Data is split into type and sent in batched notifications.
 */
@EventBusSubscriber
public final class TeamCatchupHandler {

    // Caps for one ProspectingNotification: (With ton of margin)
    // ~20 B per vein -> 5000 veins ~= 100 KB per packet
    // ~272 B per fluid -> 400 fluids ~= 109 KB per packet
    private static final int VEINS_PER_PACKET = 5000;
    private static final int FLUIDS_PER_PACKET = 400;

    private static final Map<UUID, IntSet> SENT_DIMS = new HashMap<>();

    private TeamCatchupHandler() {}

    // Need to run after GTNHLib or we may have no team to load
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (!Config.enableTeamSharing) return;
        if (!(event.player instanceof EntityPlayerMP playerMP)) return;
        SENT_DIMS.remove(playerMP.getUniqueID());

        sendDimIfNeeded(playerMP, playerMP.dimension);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        if (!Config.enableTeamSharing) return;
        if (!(event.player instanceof EntityPlayerMP playerMP)) return;

        sendDimIfNeeded(playerMP, event.toDim);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP playerMP)) return;
        SENT_DIMS.remove(playerMP.getUniqueID());
    }

    @SubscribeEvent
    public static void onTeamMerge(TeamMergeEvent event) {
        if (!Config.enableTeamSharing) return;

        Team surviving = event.surviving;
        if (surviving == null) return;

        TeamProspectionData data = (TeamProspectionData) surviving.getData(TeamProspectionData.DATA_KEY);
        if (data == null) return;

        // Update every player with new merged data, only for dimensions they visited since login
        TeamManager.forEachOnlineTeamMember(surviving, member -> {
            VP.LOG.debug("[onTeamMerge] Sending catchup to: {}", member);

            IntSet alreadySent = SENT_DIMS.get(member.getUniqueID());
            if (alreadySent == null || alreadySent.isEmpty()) return;

            alreadySent.forEach((int dim) -> {
                sendDimVeins(member, data, dim);
                sendDimFluids(member, data, dim);
            });
        });
    }

    private static void sendDimIfNeeded(EntityPlayerMP player, int dim) {
        UUID uuid = player.getUniqueID();
        IntSet sent = SENT_DIMS.computeIfAbsent(uuid, k -> new IntOpenHashSet());
        if (!sent.add(dim)) return;

        Team team = TeamManager.getTeamByPlayer(uuid);
        if (team == null) return;
        TeamProspectionData data = (TeamProspectionData) team.getData(TeamProspectionData.DATA_KEY);
        if (data == null) return;

        sendDimVeins(player, data, dim);
        sendDimFluids(player, data, dim);
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
                vein = new OreVeinPosition(dim, chunkX, chunkZ, vein.veinType, teamDepleted, vein.getSource());
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
        VP.network.sendTo(TeamCatchupNotification.veins(batch), player);
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
        VP.network.sendTo(TeamCatchupNotification.fluids(batch), player);
    }
}
