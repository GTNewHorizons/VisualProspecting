package com.sinthoras.visualprospecting.database;

import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackX;
import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackY;
import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackZ;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import com.sinthoras.visualprospecting.network.ProspectingNotification;
import com.sinthoras.visualprospecting.network.ProspectingRequest;
import com.sinthoras.visualprospecting.teams.TeamProspectionDispatcher;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.events.OreDrillScanEvent;
import gregtech.api.events.OreInteractEvent;
import gregtech.api.events.VeinGenerateEvent;
import gregtech.api.interfaces.IOreMaterial;
import gregtech.common.UndergroundOil;
import gregtech.common.WorldgenGTOreLayer;
import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class ServerCache extends WorldCache {

    public static final ServerCache instance = new ServerCache();

    private ServerCache() {}

    protected File getStorageDirectory() {
        return Utils.getSubDirectory(Tags.SERVER_DIR);
    }

    public synchronized void notifyOreVeinGeneration(int dimensionId, int chunkX, int chunkZ, final VeinType veinType,
            VeinSource source) {
        if (veinType != VeinType.NO_VEIN) {
            super.putOreVein(new OreVeinPosition(dimensionId, chunkX, chunkZ, veinType, false, source));
        }
    }

    @SubscribeEvent
    public void onVeinGenerated(VeinGenerateEvent event) {
        if (event.result == WorldgenGTOreLayer.ORE_PLACED && !event.layer.mWorldGenName.equals("NoOresInVein")) {
            notifyOreVeinGeneration(
                    event.world.provider.dimensionId,
                    event.oreSeedX,
                    event.oreSeedZ,
                    VeinTypeCaching.getVeinType(event.layer.mWorldGenName),
                    VeinSource.GENERATED);
        }
    }

    @SubscribeEvent
    public void onOreClicked(OreInteractEvent event) {
        World world = event.world;
        int x = event.x;
        int y = event.y;
        int z = event.z;
        EntityPlayer player = event.player;

        if (!world.isRemote && Config.enableProspecting) {
            IOreMaterial material;

            try (OreInfo<?> info = OreManager.getOreInfo(world, x, y, z)) {
                material = info != null && info.isNatural ? info.material : null;
            }

            ProspectingNotification response = ProspectingRequest
                    .prospect(new ProspectingRequest(world.provider.dimensionId, x, y, z, material), event.world);

            if (response != null) {
                TeamProspectionDispatcher.deliverProspectingResults((EntityPlayerMP) player, response);
            }
        }
    }

    @SubscribeEvent
    public void onOreDrillScan(OreDrillScanEvent event) {
        if (event.world.isRemote || !Config.enableProspecting) return;

        final int dimensionId = event.world.provider.dimensionId;
        final Set<OreVeinPosition> foundVeins = new LinkedHashSet<>();
        final LongSet resolvedChunks = new LongOpenHashSet();
        final Long2ObjectMap<List<OreVeinPosition>> candidateVeins = new Long2ObjectOpenHashMap<>();
        for (long position : event.orePositions) {
            final int x = unpackX(position);
            final int z = unpackZ(position);
            final int chunkX = x >> 4;
            final int chunkZ = z >> 4;
            final long chunkKey = Utils.chunkCoordsToKey(chunkX, chunkZ);
            if (resolvedChunks.contains(chunkKey)) continue;

            try (OreInfo<?> info = OreManager.getOreInfo(event.world, x, unpackY(position), z)) {
                if (info != null && info.isNatural && !info.isSmall && info.material != null) {
                    final List<OreVeinPosition> candidates = candidateVeins
                            .computeIfAbsent(chunkKey, ignored -> getCandidateVeins(dimensionId, chunkX, chunkZ));
                    if (candidates.isEmpty()) {
                        resolvedChunks.add(chunkKey);
                        continue;
                    }

                    final OreVeinPosition vein = resolveVeinForOre(candidates, info.material);
                    if (vein.veinType != VeinType.NO_VEIN) {
                        foundVeins.add(vein);
                        resolvedChunks.add(chunkKey);
                    }
                }
            }
        }
        if (!foundVeins.isEmpty()) {
            TeamProspectionDispatcher
                    .deliverProspectingResults(event.owner, new ArrayList<>(foundVeins), Collections.emptyList());
        }
    }

    public OreVeinPosition resolveVeinForOre(int dimensionId, int chunkX, int chunkZ, IOreMaterial ore) {
        return resolveVeinForOre(getCandidateVeins(dimensionId, chunkX, chunkZ), ore);
    }

    private OreVeinPosition resolveVeinForOre(List<OreVeinPosition> candidateVeins, IOreMaterial ore) {
        if (ore == null) return OreVeinPosition.EMPTY_VEIN;
        for (OreVeinPosition vein : candidateVeins) {
            if (vein.veinType.containsOre(ore)) return vein;
        }
        return OreVeinPosition.EMPTY_VEIN;
    }

    private List<OreVeinPosition> getCandidateVeins(int dimensionId, int chunkX, int chunkZ) {
        final List<OreVeinPosition> candidateVeins = new ArrayList<>(9);
        final OreVeinPosition centerVein = getOreVein(dimensionId, chunkX, chunkZ);
        if (centerVein.veinType != VeinType.NO_VEIN) candidateVeins.add(centerVein);

        final int centerChunkX = Utils.mapToCenterOreChunkCoord(chunkX);
        final int centerChunkZ = Utils.mapToCenterOreChunkCoord(chunkZ);
        for (int offsetChunkX = -3; offsetChunkX <= 3; offsetChunkX += 3) {
            for (int offsetChunkZ = -3; offsetChunkZ <= 3; offsetChunkZ += 3) {
                if (offsetChunkX == 0 && offsetChunkZ == 0) continue;

                final int neighborChunkX = centerChunkX + offsetChunkX;
                final int neighborChunkZ = centerChunkZ + offsetChunkZ;
                final OreVeinPosition neighborVein = getOreVein(dimensionId, neighborChunkX, neighborChunkZ);
                final int distanceChunks = Math
                        .max(Math.abs(neighborChunkX - chunkX), Math.abs(neighborChunkZ - chunkZ));
                // Equals ceil(blockSize / 16.0) + 1.
                final int maxDistance = ((neighborVein.veinType.blockSize + 16) >> 4) + 1;
                if (neighborVein.veinType != VeinType.NO_VEIN && distanceChunks <= maxDistance) {
                    candidateVeins.add(neighborVein);
                }
            }
        }
        return candidateVeins;
    }

    public List<OreVeinPosition> prospectOreChunks(int dimensionId, int minChunkX, int minChunkZ, int maxChunkX,
            int maxChunkZ) {
        minChunkX = Utils.mapToCenterOreChunkCoord(minChunkX);
        minChunkZ = Utils.mapToCenterOreChunkCoord(minChunkZ);
        maxChunkX = Utils.mapToCenterOreChunkCoord(maxChunkX);
        maxChunkZ = Utils.mapToCenterOreChunkCoord(maxChunkZ);

        List<OreVeinPosition> oreVeinPositions = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX = Utils.mapToCenterOreChunkCoord(chunkX + 3)) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ = Utils.mapToCenterOreChunkCoord(chunkZ + 3)) {
                final OreVeinPosition oreVeinPosition = getOreVein(dimensionId, chunkX, chunkZ);
                if (oreVeinPosition.veinType != VeinType.NO_VEIN) {
                    oreVeinPositions.add(oreVeinPosition);
                }
            }
        }
        return oreVeinPositions;
    }

    public List<OreVeinPosition> prospectOreBlocks(int dimensionId, int minBlockX, int minBlockZ, int maxBlockX,
            int maxBlockZ) {
        return prospectOreChunks(
                dimensionId,
                Utils.coordBlockToChunk(minBlockX),
                Utils.coordBlockToChunk(minBlockZ),
                Utils.coordBlockToChunk(maxBlockX),
                Utils.coordBlockToChunk(maxBlockZ));
    }

    public List<OreVeinPosition> prospectOreBlockRadius(int dimensionId, int blockX, int blockZ, int blockRadius) {
        return prospectOreBlocks(
                dimensionId,
                blockX - blockRadius,
                blockZ - blockRadius,
                blockX + blockRadius,
                blockZ + blockRadius);
    }

    public List<UndergroundFluidPosition> prospectUndergroundFluidBlockRadius(World world, int blockX, int blockZ,
            int undergroundFluidBlockRadius) {
        final int minChunkX = Utils
                .mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(blockX - undergroundFluidBlockRadius));
        final int minChunkZ = Utils
                .mapToCornerUndergroundFluidChunkCoord(Utils.coordBlockToChunk(blockZ - undergroundFluidBlockRadius));

        // Equals to ceil(undergroundFluidBlockRadius / (VP.undergroundFluidFieldSizeChunkX * VP.chunkWidth))
        final int undergroundFluidRadius = (undergroundFluidBlockRadius + VP.undergroundFluidSizeChunkX * VP.chunkWidth
                - 1) / (VP.undergroundFluidSizeChunkX * VP.chunkWidth);

        List<UndergroundFluidPosition> foundUndergroundFluids = new ArrayList<>(
                (2 * undergroundFluidRadius + 1) * (2 * undergroundFluidRadius + 1));

        for (int undergroundFluidX = 0; undergroundFluidX < 2 * undergroundFluidRadius + 1; undergroundFluidX++) {
            for (int undergroundFluidZ = 0; undergroundFluidZ < 2 * undergroundFluidRadius + 1; undergroundFluidZ++) {
                final int chunkX = minChunkX + undergroundFluidX * VP.undergroundFluidSizeChunkX;
                final int chunkZ = minChunkZ + undergroundFluidZ * VP.undergroundFluidSizeChunkZ;
                final int[][] chunks = new int[VP.undergroundFluidSizeChunkX][VP.undergroundFluidSizeChunkZ];
                Fluid fluid = null;
                for (int offsetChunkX = 0; offsetChunkX < VP.undergroundFluidSizeChunkX; offsetChunkX++) {
                    for (int offsetChunkZ = 0; offsetChunkZ < VP.undergroundFluidSizeChunkZ; offsetChunkZ++) {
                        final FluidStack prospectedFluid = UndergroundOil
                                .undergroundOil(world, chunkX + offsetChunkX, chunkZ + offsetChunkZ, -1);
                        if (prospectedFluid != null) {
                            fluid = prospectedFluid.getFluid();
                            chunks[offsetChunkX][offsetChunkZ] = prospectedFluid.amount;
                        }
                    }
                }
                if (fluid != null) {
                    foundUndergroundFluids.add(
                            new UndergroundFluidPosition(world.provider.dimensionId, chunkX, chunkZ, fluid, chunks));
                }
            }
        }
        return foundUndergroundFluids;
    }
}
