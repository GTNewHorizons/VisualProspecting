package com.sinthoras.visualprospecting.commands.admin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.github.bsideup.jabel.Desugar;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.commands.CommandHelpers;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.VeinSource;
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

    public static final String ARG_VEIN = "vein";

    private static final String VEIN = "visualprospecting.command.vpadmin.debug.vein.";
    private static final String ANALYZE = "visualprospecting.command.vpadmin.debug.analyze.";
    private static final String VERDICT = "visualprospecting.command.vpadmin.debug.verdict.";
    private static final String AREA = "visualprospecting.command.vpadmin.debug.area.";
    private static final String APPLY = "visualprospecting.command.vpadmin.debug.apply.";

    private static final int AREA_MAX_ORES_PER_LINE = 4;

    private VPAdminDebugCommand() {}

    @Desugar
    private record OreChunkTarget(int dimensionId, int chunkX, int chunkZ, String dimName, World world) {

        static OreChunkTarget of(EntityPlayerMP player) {
            final int dimensionId = player.dimension;
            return new OreChunkTarget(
                    dimensionId,
                    Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(MathHelper.floor_double(player.posX))),
                    Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(MathHelper.floor_double(player.posZ))),
                    new DimensionAnalysis(dimensionId).dimensionName,
                    player.worldObj);
        }
    }

    /**
     * {@code /vp_admin debug vein}: inspect the central ore chunk the sender is standing on. Prints the server cache
     * record, a live output of the cache-builder analysis for that chunk and whether both agree.
     * <p>
     * Limitation: Doesn't do the whole cleanUpWithNeighbors logic
     */
    public static int cmdDebugOreVein(CommandContext<ICommandSender> ctx) {
        ICommandSender sender = ctx.getSource();

        if (!(sender instanceof EntityPlayerMP player)) {
            return CommandHelpers.error(sender, "visualprospecting.command.console_forbidden");
        }

        final OreChunkTarget target = OreChunkTarget.of(player);
        printHeader(sender, target);
        final OreVeinPosition cached = printCacheRecord(sender, target);
        final VeinType resolved = printDryRunAnalysis(sender, target);
        if (resolved != null) {
            printVerdict(sender, cached, resolved);
        }

        return CommandHelpers.SUCCESS;
    }

    /**
     * {@code /vp_admin debug vein area}: per-chunk ore breakdown of the 3x3 chunks around the central ore chunk, to
     * spot veins that were partly generated with a different ore mix.
     */
    public static int cmdDebugOreVeinArea(CommandContext<ICommandSender> ctx) {
        ICommandSender sender = ctx.getSource();

        if (!(sender instanceof EntityPlayerMP player)) {
            return CommandHelpers.error(sender, "visualprospecting.command.console_forbidden");
        }

        final OreChunkTarget target = OreChunkTarget.of(player);
        printHeader(sender, target);
        printCacheRecord(sender, target);
        CommandHelpers.plain(sender, AREA + "title");
        for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                printAreaChunk(sender, target, target.chunkX + offsetX, target.chunkZ + offsetZ);
            }
        }

        return CommandHelpers.SUCCESS;
    }

    /**
     * {@code /vp_admin debug vein apply [vein]}: store the analysis result, or the given vein, for the central ore
     * chunk with {@link VeinSource#ADMIN}.
     */
    public static int cmdApplyOreVein(CommandContext<ICommandSender> ctx, String veinName) {
        ICommandSender sender = ctx.getSource();

        if (!(sender instanceof EntityPlayerMP player)) {
            return CommandHelpers.error(sender, "visualprospecting.command.console_forbidden");
        }

        final OreChunkTarget target = OreChunkTarget.of(player);
        final VeinType veinType;
        if (veinName == null) {
            try {
                veinType = resolveDetailed(loadLiveChunk(target.world, target.chunkX, target.chunkZ), target);
            } catch (Exception e) {
                VP.LOG.error(
                        "/vp_admin debug vein apply: failed to read live chunk at {}, {} in dim {}",
                        target.chunkX,
                        target.chunkZ,
                        target.dimensionId,
                        e);
                return CommandHelpers.error(sender, ANALYZE + "error", String.valueOf(e));
            }
            if (veinType == VeinType.NO_VEIN) {
                return CommandHelpers.error(sender, APPLY + "inconclusive");
            }
        } else {
            veinType = VeinTypeCaching.getVeinType(veinName);
            if (veinType == VeinType.NO_VEIN) {
                return CommandHelpers.error(sender, APPLY + "unknown_vein", veinName);
            }
            if (!target.dimName.isEmpty() && !veinType.getAllowedDimensions().contains(target.dimName)) {
                return CommandHelpers.error(sender, APPLY + "wrong_dimension", veinType.name, target.dimName);
            }
        }

        final OreVeinPosition previous = ServerCache.instance
                .getOreVein(target.dimensionId, target.chunkX, target.chunkZ);
        final String previousDescription = previous.veinType == VeinType.NO_VEIN ? "-"
                : previous.veinType.name + " (" + previous.getSource() + ")";

        ServerCache.instance
                .notifyOreVeinGeneration(target.dimensionId, target.chunkX, target.chunkZ, veinType, VeinSource.ADMIN);
        VP.LOG.info(
                "{} set ore vein at chunk {}, {} in dim {} to {}, was {}",
                sender.getCommandSenderName(),
                target.chunkX,
                target.chunkZ,
                target.dimensionId,
                veinType.name,
                previousDescription);

        CommandHelpers.success(sender, APPLY + "done", veinType.name, veinType.getVeinName(), previousDescription);
        return CommandHelpers.plain(sender, APPLY + "clients");
    }

    public static CompletableFuture<Suggestions> suggestVeinNames(CommandContext<ICommandSender> ctx,
            SuggestionsBuilder builder) {
        final String dimName = ctx.getSource() instanceof EntityPlayerMP player
                ? new DimensionAnalysis(player.dimension).dimensionName
                : "";
        final String remaining = builder.getRemaining();
        final List<String> names = new ArrayList<>();
        for (VeinType veinType : VeinTypeCaching.getVeinTypes()) {
            if (veinType == VeinType.NO_VEIN || !veinType.name.startsWith(remaining)) continue;
            if (!dimName.isEmpty() && !veinType.getAllowedDimensions().contains(dimName)) continue;
            names.add(veinType.name);
        }
        names.sort(null);
        names.forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static void printHeader(ICommandSender sender, OreChunkTarget target) {
        IChatComponent header = new ChatComponentTranslation(
                VEIN + "header",
                target.chunkX,
                target.chunkZ,
                target.dimensionId,
                target.dimName);
        header.getChatStyle().setBold(true);
        sender.addChatMessage(header);
    }

    // Print data stored in server cache
    private static OreVeinPosition printCacheRecord(ICommandSender sender, OreChunkTarget target) {
        final OreVeinPosition vein = ServerCache.instance.getOreVein(target.dimensionId, target.chunkX, target.chunkZ);
        if (vein.veinType == VeinType.NO_VEIN) {
            CommandHelpers.plain(sender, VEIN + "cache_none");
        } else {
            CommandHelpers.plain(sender, VEIN + "type", vein.veinType.name, vein.veinType.getVeinName());
            CommandHelpers.plain(sender, VEIN + "source", vein.getSource());
        }
        return vein;
    }

    // Run the cache-builder analysis on the live chunk (output only, nothing is written to cache). Returns the
    // detailed verdict, or null if the chunk could not be read.
    private static VeinType printDryRunAnalysis(ICommandSender sender, OreChunkTarget target) {
        CommandHelpers.plain(sender, ANALYZE + "title");

        final PartiallyLoadedChunk chunk;
        try {
            chunk = loadLiveChunk(target.world, target.chunkX, target.chunkZ);
        } catch (Exception e) {
            VP.LOG.error(
                    "/vp_admin debug vein: failed to read live chunk at {}, {} in dim {}",
                    target.chunkX,
                    target.chunkZ,
                    target.dimensionId,
                    e);
            CommandHelpers.error(sender, ANALYZE + "error", String.valueOf(e));
            return null;
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
            return VeinType.NO_VEIN;
        }

        CommandHelpers.plain(sender, ANALYZE + "stats", total(oreCounts), oreCounts.size(), minY[0], maxY[0]);

        final List<IOreMaterial> materials = sortedByCount(oreCounts);
        for (IOreMaterial ore : materials) {
            CommandHelpers.plain(sender, ANALYZE + "ore_line", ore.getLocalizedName(), oreCounts.getInt(ore));
        }

        final IOreMaterial dominant = materials.get(0);
        CommandHelpers.plain(sender, ANALYZE + "dominant", dominant.getLocalizedName(), oreCounts.getInt(dominant));

        final String dimName = target.dimName;
        if (dimName != null && !dimName.isEmpty() && !VeinTypeCaching.hasVeinsInDimension(dimName)) {
            CommandHelpers.plain(sender, ANALYZE + "no_dim_veins", dimName);
        }

        printCandidateReasoning(sender, oreCounts, dimName, dominant);
        return printResolvedVerdicts(sender, chunk, target);
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
        int oneMissingMatches = 0;
        for (VeinType candidate : candidates) {
            final boolean full = candidate.matchesWithSpecificPrimaryOrSecondary(oreCounts.keySet(), dimName, dominant);
            final boolean oneMissing = candidate.matchesWithOneMissingOre(oreCounts.keySet(), dimName, dominant);
            if (full) fullMatches++;
            if (oneMissing) oneMissingMatches++;
            CommandHelpers.plain(sender, ANALYZE + "candidate_line", candidate.name, full, oneMissing);
        }
        CommandHelpers.plain(sender, ANALYZE + "match_summary", fullMatches, oneMissingMatches);
    }

    // Print Verdict
    private static VeinType printResolvedVerdicts(ICommandSender sender, PartiallyLoadedChunk chunk,
            OreChunkTarget target) {
        final VeinType resolved = resolveDetailed(chunk, target);
        if (resolved == VeinType.NO_VEIN) {
            CommandHelpers.plain(sender, ANALYZE + "resolved_none");
        } else {
            CommandHelpers.plain(sender, ANALYZE + "resolved", resolved.name, resolved.getVeinName());
        }

        final ChunkAnalysis fast = new ChunkAnalysis(target.dimName);
        fast.processMinecraftChunk(chunk);
        if (fast.matchesSingleVein()) {
            CommandHelpers.plain(sender, ANALYZE + "fastpath", fast.getMatchedVein().name);
        } else {
            CommandHelpers.plain(sender, ANALYZE + "fastpath_ambiguous");
        }
        return resolved;
    }

    // Compare the cache record with the detailed analysis
    private static void printVerdict(ICommandSender sender, OreVeinPosition cached, VeinType resolved) {
        if (resolved == VeinType.NO_VEIN) {
            if (cached.veinType != VeinType.NO_VEIN) {
                CommandHelpers.plain(sender, VERDICT + "inconclusive");
            }
            return;
        }
        if (cached.veinType == resolved) {
            CommandHelpers.success(sender, VERDICT + "match");
            return;
        }

        if (cached.veinType == VeinType.NO_VEIN) {
            CommandHelpers.error(sender, VERDICT + "missing", resolved.name);
        } else {
            CommandHelpers.error(sender, VERDICT + "mismatch", cached.veinType.name, resolved.name);
            if (!VeinSource.RESCAN.canOverwrite(cached.getSource())) {
                CommandHelpers.plain(sender, VERDICT + "protected", cached.getSource());
            }
        }
        CommandHelpers.plain(sender, VERDICT + "hint_apply");
    }

    private static void printAreaChunk(ICommandSender sender, OreChunkTarget target, int chunkX, int chunkZ) {
        final boolean isCentre = chunkX == target.chunkX && chunkZ == target.chunkZ;

        final PartiallyLoadedChunk chunk;
        try {
            chunk = loadLiveChunk(target.world, chunkX, chunkZ);
        } catch (Exception e) {
            VP.LOG.error(
                    "/vp_admin debug vein area: failed to read live chunk at {}, {} in dim {}",
                    chunkX,
                    chunkZ,
                    target.dimensionId,
                    e);
            CommandHelpers.error(sender, AREA + "error", chunkX, chunkZ, String.valueOf(e));
            return;
        }

        final Reference2IntOpenHashMap<IOreMaterial> oreCounts = new Reference2IntOpenHashMap<>();
        chunk.forEachOre((x, y, z, info) -> oreCounts.addTo(info.material, 1));
        if (oreCounts.isEmpty()) {
            CommandHelpers.plain(sender, AREA + (isCentre ? "line_empty_centre" : "line_empty"), chunkX, chunkZ);
            return;
        }

        final VeinType resolved = resolveDetailed(chunk, target);
        CommandHelpers.plain(
                sender,
                AREA + (isCentre ? "line_centre" : "line"),
                chunkX,
                chunkZ,
                total(oreCounts),
                resolved == VeinType.NO_VEIN ? "?" : resolved.name);

        final List<IOreMaterial> materials = sortedByCount(oreCounts);
        final StringBuilder ores = new StringBuilder();
        for (int i = 0; i < Math.min(materials.size(), AREA_MAX_ORES_PER_LINE); i++) {
            if (i > 0) ores.append(", ");
            final IOreMaterial ore = materials.get(i);
            ores.append(ore.getLocalizedName()).append(" x").append(oreCounts.getInt(ore));
        }
        if (materials.size() > AREA_MAX_ORES_PER_LINE) {
            ores.append(", +").append(materials.size() - AREA_MAX_ORES_PER_LINE);
        }
        CommandHelpers.plain(sender, AREA + "ores", ores.toString());
    }

    private static VeinType resolveDetailed(PartiallyLoadedChunk chunk, OreChunkTarget target) {
        final DetailedChunkAnalysis detailed = new DetailedChunkAnalysis(
                target.dimensionId,
                target.dimName,
                target.chunkX,
                target.chunkZ);
        detailed.processMinecraftChunk(chunk);
        detailed.resolve(new Long2IntOpenHashMap());
        return detailed.getResolvedVeinType();
    }

    private static List<IOreMaterial> sortedByCount(Reference2IntOpenHashMap<IOreMaterial> oreCounts) {
        final List<IOreMaterial> materials = new ArrayList<>(oreCounts.keySet());
        materials.sort(Comparator.comparingInt(oreCounts::getInt).reversed());
        return materials;
    }

    private static int total(Reference2IntOpenHashMap<IOreMaterial> oreCounts) {
        int total = 0;
        for (var entry : oreCounts.reference2IntEntrySet()) {
            total += entry.getIntValue();
        }
        return total;
    }

    private static PartiallyLoadedChunk loadLiveChunk(World world, int chunkX, int chunkZ) {
        final Chunk live = world.getChunkFromChunkCoords(chunkX, chunkZ);
        final PartiallyLoadedChunk chunk = new PartiallyLoadedChunk();
        chunk.loadFromLiveChunk(live);
        return chunk;
    }
}
