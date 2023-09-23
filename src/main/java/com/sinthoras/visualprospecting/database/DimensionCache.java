package com.sinthoras.visualprospecting.database;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

public class DimensionCache {

    public enum UpdateResult {
        AlreadyKnown,
        Updated,
        New
    }

    private final Map<ChunkCoordIntPair, OreVeinPosition> oreChunks = new HashMap<>();
    private final Map<ChunkCoordIntPair, UndergroundFluidPosition> undergroundFluids = new HashMap<>();
    private final Set<ChunkCoordIntPair> changedOrNewOreChunks = new HashSet<>();
    private final Set<ChunkCoordIntPair> changedOrNewUndergroundFluids = new HashSet<>();
    public final int dimensionId;

    public DimensionCache(int dimensionId) {
        this.dimensionId = dimensionId;
    }

    public ByteBuffer saveOreChunks() {
        if (changedOrNewOreChunks.isEmpty() == false) {
            final ByteBuffer byteBuffer = ByteBuffer
                    .allocate(changedOrNewOreChunks.size() * (2 * Integer.BYTES + Short.BYTES));
            changedOrNewOreChunks.stream().map(oreChunks::get).forEach(oreVeinPosition -> {
                byteBuffer.putInt(oreVeinPosition.chunkX);
                byteBuffer.putInt(oreVeinPosition.chunkZ);
                short veinTypeId = VeinTypeCaching.getVeinTypeId(oreVeinPosition.veinType);
                if (oreVeinPosition.isDepleted()) {
                    veinTypeId |= 0x8000;
                }
                byteBuffer.putShort(veinTypeId);
            });
            changedOrNewOreChunks.clear();
            byteBuffer.flip();
            return byteBuffer;
        }
        return null;
    }

    public ByteBuffer saveUndergroundFluids() {
        if (!changedOrNewUndergroundFluids.isEmpty()) {
            final int initialCapacity = changedOrNewUndergroundFluids.size() * (Long.BYTES
                    + Integer.BYTES * (8 + VP.undergroundFluidSizeChunkX * VP.undergroundFluidSizeChunkZ));
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(initialCapacity);
                    final DataOutputStream dos = new DataOutputStream(baos)) {
                for (ChunkCoordIntPair changedOrNewUndergroundFluid : changedOrNewUndergroundFluids) {
                    UndergroundFluidPosition undergroundFluidPosition = undergroundFluids
                            .get(changedOrNewUndergroundFluid);
                    dos.writeInt(undergroundFluidPosition.chunkX);
                    dos.writeInt(undergroundFluidPosition.chunkZ);
                    byte[] fluidNameBytes = undergroundFluidPosition.fluid.getName().getBytes(StandardCharsets.UTF_8);
                    // Negative to keep backwards compat with int fluidID written here before.
                    dos.writeInt(-fluidNameBytes.length);
                    dos.write(fluidNameBytes);
                    for (int offsetChunkX = 0; offsetChunkX < VP.undergroundFluidSizeChunkX; offsetChunkX++) {
                        for (int offsetChunkZ = 0; offsetChunkZ < VP.undergroundFluidSizeChunkZ; offsetChunkZ++) {
                            dos.writeInt(undergroundFluidPosition.chunks[offsetChunkX][offsetChunkZ]);
                        }
                    }
                }
                changedOrNewUndergroundFluids.clear();
                return ByteBuffer.wrap(baos.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public void loadCache(ByteBuffer oreChunksBuffer, ByteBuffer undergroundFluidsBuffer) {
        if (oreChunksBuffer != null) {
            while (oreChunksBuffer.remaining() >= Integer.BYTES * 2 + Short.BYTES) {
                final int chunkX = oreChunksBuffer.getInt();
                final int chunkZ = oreChunksBuffer.getInt();
                final short veinTypeId = oreChunksBuffer.getShort();
                final boolean depleted = (veinTypeId & 0x8000) > 0;
                final VeinType veinType = VeinTypeCaching.getVeinType((short) (veinTypeId & 0x7FFF));
                oreChunks.put(
                        getOreVeinKey(chunkX, chunkZ),
                        new OreVeinPosition(dimensionId, chunkX, chunkZ, veinType, depleted));
            }
        }
        if (undergroundFluidsBuffer != null) {
            while (undergroundFluidsBuffer.remaining()
                    >= Integer.BYTES * (3 + VP.undergroundFluidSizeChunkX * VP.undergroundFluidSizeChunkZ)) {
                final int chunkX = undergroundFluidsBuffer.getInt();
                final int chunkZ = undergroundFluidsBuffer.getInt();
                final int fluidIDorNameLength = undergroundFluidsBuffer.getInt();
                final Fluid fluid;
                if (fluidIDorNameLength < 0) { // name length
                    byte[] fluidNameBytes = new byte[-fluidIDorNameLength];
                    undergroundFluidsBuffer.get(fluidNameBytes);
                    String fluidName = new String(fluidNameBytes, StandardCharsets.UTF_8);
                    fluid = FluidRegistry.getFluid(fluidName);
                } else { // ID (legacy save format)
                    fluid = FluidRegistry.getFluid(fluidIDorNameLength);
                }
                final int[][] chunks = new int[VP.undergroundFluidSizeChunkX][VP.undergroundFluidSizeChunkZ];
                for (int offsetChunkX = 0; offsetChunkX < VP.undergroundFluidSizeChunkX; offsetChunkX++) {
                    for (int offsetChunkZ = 0; offsetChunkZ < VP.undergroundFluidSizeChunkZ; offsetChunkZ++) {
                        chunks[offsetChunkX][offsetChunkZ] = undergroundFluidsBuffer.getInt();
                    }
                }
                if (fluid != null) {
                    undergroundFluids.put(
                            getUndergroundFluidKey(chunkX, chunkZ),
                            new UndergroundFluidPosition(dimensionId, chunkX, chunkZ, fluid, chunks));
                }
            }
        }
    }

    private ChunkCoordIntPair getOreVeinKey(int chunkX, int chunkZ) {
        return new ChunkCoordIntPair(Utils.mapToCenterOreChunkCoord(chunkX), Utils.mapToCenterOreChunkCoord(chunkZ));
    }

    public UpdateResult putOreVein(final OreVeinPosition oreVeinPosition) {
        final ChunkCoordIntPair key = getOreVeinKey(oreVeinPosition.chunkX, oreVeinPosition.chunkZ);
        if (oreChunks.containsKey(key) == false) {
            oreChunks.put(key, oreVeinPosition);
            changedOrNewOreChunks.add(key);
            return UpdateResult.New;
        }
        final OreVeinPosition storedOreVeinPosition = oreChunks.get(key);
        if (storedOreVeinPosition.veinType != oreVeinPosition.veinType) {
            oreChunks.put(key, oreVeinPosition.joinDepletedState(storedOreVeinPosition));
            changedOrNewOreChunks.add(key);
            return UpdateResult.New;
        }
        return UpdateResult.AlreadyKnown;
    }

    public void toggleOreVein(int chunkX, int chunkZ) {
        final ChunkCoordIntPair key = getOreVeinKey(chunkX, chunkZ);
        if (oreChunks.containsKey(key)) {
            oreChunks.get(key).toggleDepleted();
            changedOrNewOreChunks.add(key);
        }
    }

    public OreVeinPosition getOreVein(int chunkX, int chunkZ) {
        final ChunkCoordIntPair key = getOreVeinKey(chunkX, chunkZ);
        return oreChunks.getOrDefault(key, new OreVeinPosition(dimensionId, chunkX, chunkZ, VeinType.NO_VEIN, true));
    }

    private ChunkCoordIntPair getUndergroundFluidKey(int chunkX, int chunkZ) {
        return new ChunkCoordIntPair(
                Utils.mapToCornerUndergroundFluidChunkCoord(chunkX),
                Utils.mapToCornerUndergroundFluidChunkCoord(chunkZ));
    }

    public UpdateResult putUndergroundFluid(final UndergroundFluidPosition undergroundFluid) {
        final ChunkCoordIntPair key = getUndergroundFluidKey(undergroundFluid.chunkX, undergroundFluid.chunkZ);
        if (undergroundFluids.containsKey(key) == false) {
            changedOrNewUndergroundFluids.add(key);
            undergroundFluids.put(key, undergroundFluid);
            return UpdateResult.New;
        } else if (undergroundFluids.get(key).equals(undergroundFluid) == false) {
            changedOrNewUndergroundFluids.add(key);
            undergroundFluids.put(key, undergroundFluid);
            return UpdateResult.Updated;
        }
        return UpdateResult.AlreadyKnown;
    }

    public UndergroundFluidPosition getUndergroundFluid(int chunkX, int chunkZ) {
        final ChunkCoordIntPair key = getUndergroundFluidKey(chunkX, chunkZ);
        return undergroundFluids
                .getOrDefault(key, UndergroundFluidPosition.getNotProspected(dimensionId, chunkX, chunkZ));
    }

    public Collection<OreVeinPosition> getAllOreVeins() {
        return oreChunks.values();
    }

    public Collection<UndergroundFluidPosition> getAllUndergroundFluids() {
        return undergroundFluids.values();
    }

    /**
     * Reset selected veins; these veins need not be present. Input coords are in chunk coordinates, NOT block coords.
     * Will not error on bad input, but it also probably won't do anything useful. startChunks should be less than their
     * respective endChunks.
     */
    public void clearOreVeins(int startChunkX, int startChunkZ, int endChunkX, int endChunkZ) {

        // Remove entries if they fall within the corners
        // This method iterates for each chunk mapped. In many cases, it is probably faster to iterate over chunks in
        // the area to be cleared instead. i.e. if (chunksInClearArea < totalChunksMapped) {useAltIterator()}. If
        // someone calls this enough to make it a problem, they can add that.
        oreChunks.entrySet().removeIf(entry -> {

            OreVeinPosition val = entry.getValue();
            final boolean withinX = val.chunkX >= startChunkX && val.chunkX <= endChunkX;
            final boolean withinZ = val.chunkZ >= startChunkZ && val.chunkZ <= endChunkZ;
            return withinX && withinZ;
        });
    }
}
