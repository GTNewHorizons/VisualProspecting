package com.sinthoras.visualprospecting.database;

import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackX;
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
import gregtech.api.events.OreDrillScanEvent.ScanType;
import gregtech.api.events.OreInteractEvent;
import gregtech.api.events.VeinGenerateEvent;
import gregtech.api.interfaces.IOreMaterial;
import gregtech.common.UndergroundOil;
import gregtech.common.WorldgenGTOreLayer;
import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class ServerCache extends WorldCache {

    private static final int ALL_VEIN_CHUNKS_EMPTY = (1 << 9) - 1;

    public static final ServerCache instance = new ServerCache();

    private final Int2ObjectMap<Long2IntMap> emptyOreChunkMasks = new Int2ObjectOpenHashMap<>();

    private ServerCache() {}

    protected File getStorageDirectory() {
        return Utils.getSubDirectory(Tags.SERVER_DIR);
    }

    @Override
    public void reset() {
        super.reset();
        emptyOreChunkMasks.clear();
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
        if (!event.orePositions.isEmpty()) {
            final Set<OreVeinPosition> foundVeins = new LinkedHashSet<>();
            final LongSet foundChunks = new LongOpenHashSet();
            for (long position : event.orePositions) {
                final int chunkX = unpackX(position) >> 4;
                final int chunkZ = unpackZ(position) >> 4;
                if (!foundChunks.add(Utils.chunkCoordsToKey(chunkX, chunkZ))) continue;

                final OreVeinPosition vein = getOreVein(dimensionId, chunkX, chunkZ);
                if (vein.veinType != VeinType.NO_VEIN) foundVeins.add(vein);
            }
            if (!foundVeins.isEmpty()) {
                TeamProspectionDispatcher
                        .deliverProspectingResults(event.owner, new ArrayList<>(foundVeins), Collections.emptyList());
            }
        }

        if (event.scanType != ScanType.CHUNK_COLUMN) return;

        final int chunkX = event.minX >> 4;
        final int chunkZ = event.minZ >> 4;
        final OreVeinPosition vein = getOreVein(dimensionId, chunkX, chunkZ);
        if (vein.veinType == VeinType.NO_VEIN) return;

        final int offsetX = chunkX - vein.chunkX + 1;
        final int offsetZ = chunkZ - vein.chunkZ + 1;
        if (offsetX < 0 || offsetX > 2 || offsetZ < 0 || offsetZ > 2) return;

        final Long2IntMap dimensionMasks = emptyOreChunkMasks
                .computeIfAbsent(dimensionId, ignored -> new Long2IntOpenHashMap());
        final long veinKey = Utils.chunkCoordsToKey(vein.chunkX, vein.chunkZ);
        final int chunkBit = 1 << (offsetX * 3 + offsetZ);
        final int oldMask = dimensionMasks.getOrDefault(veinKey, 0);
        final int newMask = event.orePositions.isEmpty() ? oldMask | chunkBit : oldMask & ~chunkBit;
        if (newMask == 0) {
            dimensionMasks.remove(veinKey);
        } else {
            dimensionMasks.put(veinKey, newMask);
        }

        if (newMask == ALL_VEIN_CHUNKS_EMPTY) {
            TeamProspectionDispatcher.handleDepletionToggle(event.owner, dimensionId, vein.chunkX, vein.chunkZ, true);
        }
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
