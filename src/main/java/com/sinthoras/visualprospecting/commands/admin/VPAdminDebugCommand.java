package com.sinthoras.visualprospecting.commands.admin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.mojang.brigadier.context.CommandContext;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.commands.CommandHelpers;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.cachebuilder.ChunkAnalysis;
import com.sinthoras.visualprospecting.database.cachebuilder.DetailedChunkAnalysis;
import com.sinthoras.visualprospecting.database.cachebuilder.DimensionAnalysis;
import com.sinthoras.visualprospecting.database.cachebuilder.PartiallyLoadedChunk;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import gregtech.api.interfaces.IOreMaterial;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

public class VPAdminDebugCommand {

    private static final String VEIN = "visualprospecting.command.vpadmin.debug.vein.";
    private static final String ANALYZE = "visualprospecting.command.vpadmin.debug.analyze.";

    private VPAdminDebugCommand() {}

    /**
     * {@code /vp_admin debug vein}: inspect the central ore chunk the sender is standing on. Prints the server cache
     * record and a live output of the cache-builder analysis for that chunk.
     * <p>
     * Limitation: Doesn't do the whole cleanUpWithNeighbors logic
     */
    public static int cmdDebugOreVein(CommandContext<ICommandSender> ctx) {
        ICommandSender sender = ctx.getSource();

        if (!(sender instanceof EntityPlayerMP player)) {
            return CommandHelpers.error(sender, "visualprospecting.command.console_forbidden");
        }

        final int dimensionId = player.dimension;
        final int chunkX = Utils
                .mapToCenterOreChunkCoord(Utils.coordBlockToChunk(MathHelper.floor_double(player.posX)));
        final int chunkZ = Utils
                .mapToCenterOreChunkCoord(Utils.coordBlockToChunk(MathHelper.floor_double(player.posZ)));
        final String dimName = new DimensionAnalysis(dimensionId).dimensionName;

        IChatComponent header = new ChatComponentTranslation(VEIN + "header", chunkX, chunkZ, dimensionId, dimName);
        header.getChatStyle().setBold(true);
        sender.addChatMessage(header);

        printCacheRecord(sender, dimensionId, chunkX, chunkZ);
        printDryRunAnalysis(sender, player.worldObj, dimName, dimensionId, chunkX, chunkZ);

        return CommandHelpers.SUCCESS;
    }

    // Print data stored in server cache
    private static void printCacheRecord(ICommandSender sender, int dimensionId, int chunkX, int chunkZ) {
        final OreVeinPosition vein = ServerCache.instance.getOreVein(dimensionId, chunkX, chunkZ);
        if (vein.veinType == VeinType.NO_VEIN) {
            CommandHelpers.plain(sender, VEIN + "cache_none");
            return;
        }
        CommandHelpers.plain(sender, VEIN + "type", vein.veinType.name, vein.veinType.getVeinName());
        CommandHelpers.plain(sender, VEIN + "source", vein.getSource());
    }

    // Run the cache-builder analysis on the live chunk (output only, nothing is written to cache)
    private static void printDryRunAnalysis(ICommandSender sender, World world, String dimName, int dimensionId,
            int chunkX, int chunkZ) {
        CommandHelpers.plain(sender, ANALYZE + "title");

        final PartiallyLoadedChunk chunk;
        try {
            chunk = loadLiveChunk(world, chunkX, chunkZ);
        } catch (Exception e) {
            VP.LOG.error(
                    "/vp_admin debug vein: failed to read live chunk at {}, {} in dim {}",
                    chunkX,
                    chunkZ,
                    dimensionId,
                    e);
            CommandHelpers.error(sender, ANALYZE + "error", String.valueOf(e));
            return;
        }

        // Raw ore tally
        final Reference2IntOpenHashMap<IOreMaterial> oreCounts = new Reference2IntOpenHashMap<>();
        final int[] minY = { Integer.MAX_VALUE };
        final int[] maxY = { Integer.MIN_VALUE };
        chunk.forEachOre((x, y, z, info) -> {
            oreCounts.addTo(info.material, 1);
            if (y < minY[0]) minY[0] = y;
            if (y > maxY[0]) maxY[0] = y;
        });

        if (oreCounts.isEmpty()) {
            CommandHelpers.plain(sender, ANALYZE + "no_ores");
            return;
        }

        int total = 0;
        for (var entry : oreCounts.reference2IntEntrySet()) {
            total += entry.getIntValue();
        }
        CommandHelpers.plain(sender, ANALYZE + "stats", total, oreCounts.size(), minY[0], maxY[0]);

        final List<IOreMaterial> materials = new ArrayList<>(oreCounts.keySet());
        materials.sort(Comparator.comparingInt(oreCounts::getInt).reversed());
        for (IOreMaterial ore : materials) {
            CommandHelpers.plain(sender, ANALYZE + "ore_line", ore.getLocalizedName(), oreCounts.getInt(ore));
        }

        final IOreMaterial dominant = materials.get(0);
        CommandHelpers.plain(sender, ANALYZE + "dominant", dominant.getLocalizedName(), oreCounts.getInt(dominant));

        if (dimName != null && !dimName.isEmpty() && !VeinTypeCaching.hasVeinsInDimension(dimName)) {
            CommandHelpers.plain(sender, ANALYZE + "no_dim_veins", dimName);
        }

        printCandidateReasoning(sender, oreCounts, dimName, dominant);
        printResolvedVerdicts(sender, chunk, dimName, dimensionId, chunkX, chunkZ);
    }

    // Mirror the inputs of DetailedChunkAnalysis.getMatchedVein()
    private static void printCandidateReasoning(ICommandSender sender, Reference2IntOpenHashMap<IOreMaterial> oreCounts,
            String dimName, IOreMaterial dominant) {
        final List<VeinType> candidates = new ArrayList<>();
        for (VeinType vein : VeinTypeCaching.getVeinTypesForOre(dominant)) {
            if (dimName.isEmpty() || vein.getAllowedDimensions().contains(dimName)) {
                candidates.add(vein);
            }
        }
        if (candidates.isEmpty()) {
            CommandHelpers.plain(sender, ANALYZE + "candidates_none");
            return;
        }

        CommandHelpers.plain(sender, ANALYZE + "candidates_header", dominant.getLocalizedName(), candidates.size());
        int fullMatches = 0;
        int sporadicIgnoredMatches = 0;
        for (VeinType candidate : candidates) {
            final boolean full = candidate.matchesWithSpecificPrimaryOrSecondary(oreCounts.keySet(), dimName, dominant);
            final boolean ignoreSporadic = candidate.matchesIgnoringSporadic(oreCounts.keySet(), dimName, dominant);
            if (full) fullMatches++;
            if (ignoreSporadic) sporadicIgnoredMatches++;
            CommandHelpers.plain(sender, ANALYZE + "candidate_line", candidate.name, full, ignoreSporadic);
        }
        CommandHelpers.plain(sender, ANALYZE + "match_summary", fullMatches, sporadicIgnoredMatches);
    }

    // Print Verdict
    private static void printResolvedVerdicts(ICommandSender sender, PartiallyLoadedChunk chunk, String dimName,
            int dimensionId, int chunkX, int chunkZ) {
        final DetailedChunkAnalysis detailed = new DetailedChunkAnalysis(dimensionId, dimName, chunkX, chunkZ);
        detailed.processMinecraftChunk(chunk);
        detailed.resolve(new Long2IntOpenHashMap());
        final VeinType resolved = detailed.getResolvedVeinType();
        if (resolved == VeinType.NO_VEIN) {
            CommandHelpers.plain(sender, ANALYZE + "resolved_none");
        } else {
            CommandHelpers.plain(sender, ANALYZE + "resolved", resolved.name, resolved.getVeinName());
        }

        final ChunkAnalysis fast = new ChunkAnalysis(dimName);
        fast.processMinecraftChunk(chunk);
        if (fast.matchesSingleVein()) {
            CommandHelpers.plain(sender, ANALYZE + "fastpath", fast.getMatchedVein().name);
        } else {
            CommandHelpers.plain(sender, ANALYZE + "fastpath_ambiguous");
        }
    }

    private static PartiallyLoadedChunk loadLiveChunk(World world, int chunkX, int chunkZ) {
        final Chunk live = world.getChunkFromChunkCoords(chunkX, chunkZ);
        final PartiallyLoadedChunk chunk = new PartiallyLoadedChunk();
        chunk.loadFromLiveChunk(live);
        return chunk;
    }
}
