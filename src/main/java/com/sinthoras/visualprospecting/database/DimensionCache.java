package com.sinthoras.visualprospecting.database;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class DimensionCache {

    public enum UpdateResult {
        AlreadyKnown,
        Updated,
        New
    }

    private final Long2ObjectMap<OreVeinPosition> oreChunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<UndergroundFluidPosition> undergroundFluids = new Long2ObjectOpenHashMap<>();
    private final LongSet changedOrNewOreChunks = new LongOpenHashSet();
    private final LongSet changedOrNewUndergroundFluids = new LongOpenHashSet();
    public final int dimensionId;
    private boolean isDirty = false;

    public DimensionCache(int dimensionId) {
        this.dimensionId = dimensionId;
    }

    public NBTTagCompound saveToNbt() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setTag("ores", saveOres());
        compound.setTag("fluids", saveFluids());
        compound.setInteger("dim", dimensionId);
        isDirty = false;
        return compound;
    }

    private NBTTagCompound saveOres() {
        NBTTagCompound compound = new NBTTagCompound();
        for (long key : changedOrNewOreChunks) {
            OreVeinPosition oreVeinPosition = oreChunks.get(key);
            NBTTagCompound veinCompound = new NBTTagCompound();
            veinCompound.setInteger("chunkX", oreVeinPosition.chunkX);
            veinCompound.setInteger("chunkZ", oreVeinPosition.chunkZ);
            veinCompound.setShort("veinTypeId", VeinTypeCaching.getVeinTypeId(oreVeinPosition.veinType));
            veinCompound.setBoolean("depleted", oreVeinPosition.isDepleted());
            compound.setTag(String.valueOf(key), veinCompound);
        }
        return compound;
    }

    private NBTTagCompound saveFluids() {
        NBTTagCompound compound = new NBTTagCompound();
        for (long key : changedOrNewUndergroundFluids) {
            UndergroundFluidPosition undergroundFluidPosition = undergroundFluids.get(key);
            NBTTagCompound fluidCompound = new NBTTagCompound();
            fluidCompound.setInteger("chunkX", undergroundFluidPosition.chunkX);
            fluidCompound.setInteger("chunkZ", undergroundFluidPosition.chunkZ);
            fluidCompound.setString("fluidName", undergroundFluidPosition.fluid.getName());
            NBTTagList chunkList = new NBTTagList();
            for (int i = 0; i < VP.undergroundFluidSizeChunkX; i++) {
                chunkList.appendTag(new NBTTagIntArray(undergroundFluidPosition.chunks[i]));
            }
            fluidCompound.setTag("chunks", chunkList);
            compound.setTag(String.valueOf(key), fluidCompound);
        }
        return compound;
    }

    @SuppressWarnings("unchecked")
    public void loadFromNbt(NBTTagCompound compound) {
        NBTTagCompound ores = compound.getCompoundTag("ores");
        for (NBTBase base : (Collection<NBTBase>) ores.tagMap.values()) {
            NBTTagCompound veinCompound = (NBTTagCompound) base;
            int chunkX = veinCompound.getInteger("chunkX");
            int chunkZ = veinCompound.getInteger("chunkZ");
            short veinTypeId = veinCompound.getShort("veinTypeId");
            boolean depleted = veinCompound.getBoolean("depleted");
            VeinType veinType = VeinTypeCaching.getVeinType(veinTypeId);
            oreChunks.put(
                    getOreVeinKey(chunkX, chunkZ),
                    new OreVeinPosition(dimensionId, chunkX, chunkZ, veinType, depleted));
        }

        NBTTagCompound fluids = compound.getCompoundTag("fluids");
        for (NBTBase base : (Collection<NBTBase>) fluids.tagMap.values()) {
            NBTTagCompound fluidCompound = (NBTTagCompound) base;
            int chunkX = fluidCompound.getInteger("chunkX");
            int chunkZ = fluidCompound.getInteger("chunkZ");
            String fluidName = fluidCompound.getString("fluidName");
            Fluid fluid = FluidRegistry.getFluid(fluidName);
            int[][] chunks = new int[VP.undergroundFluidSizeChunkX][VP.undergroundFluidSizeChunkZ];
            NBTTagList chunkList = fluidCompound.getTagList("chunks", 11);
            for (int i = 0; i < VP.undergroundFluidSizeChunkX; i++) {
                chunks[i] = chunkList.func_150306_c(i);
            }
            undergroundFluids.put(
                    getUndergroundFluidKey(chunkX, chunkZ),
                    new UndergroundFluidPosition(dimensionId, chunkX, chunkZ, fluid, chunks));
        }
    }

    void loadLegacy(ByteBuffer oreChunksBuffer, ByteBuffer undergroundFluidsBuffer) {
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
                changedOrNewOreChunks.add(getOreVeinKey(chunkX, chunkZ));
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
                    changedOrNewUndergroundFluids.add(getUndergroundFluidKey(chunkX, chunkZ));
                }
            }
        }
    }

    private long getOreVeinKey(int chunkX, int chunkZ) {
        return Utils.chunkCoordsToKey(Utils.mapToCenterOreChunkCoord(chunkX), Utils.mapToCenterOreChunkCoord(chunkZ));
    }

    private long getUndergroundFluidKey(int chunkX, int chunkZ) {
        return Utils.chunkCoordsToKey(
                Utils.mapToCornerUndergroundFluidChunkCoord(chunkX),
                Utils.mapToCornerUndergroundFluidChunkCoord(chunkZ));
    }

    public void toggleOreVein(int chunkX, int chunkZ) {
        final long key = getOreVeinKey(chunkX, chunkZ);
        final OreVeinPosition oreVeinPosition = oreChunks.get(key);
        if (oreVeinPosition != null) {
            oreVeinPosition.toggleDepleted();
            changedOrNewOreChunks.add(key);
            markDirty();
        }
    }

    public UpdateResult putOreVein(final OreVeinPosition oreVeinPosition) {
        final long key = getOreVeinKey(oreVeinPosition.chunkX, oreVeinPosition.chunkZ);
        final OreVeinPosition storedOreVeinPosition = oreChunks.get(key);
        if (storedOreVeinPosition == null) {
            oreChunks.put(key, oreVeinPosition);
            changedOrNewOreChunks.add(key);
            markDirty();
            return UpdateResult.New;
        }
        if (storedOreVeinPosition.veinType != oreVeinPosition.veinType) {
            oreChunks.put(key, oreVeinPosition.joinDepletedState(storedOreVeinPosition));
            changedOrNewOreChunks.add(key);
            markDirty();
            return UpdateResult.New;
        }
        return UpdateResult.AlreadyKnown;
    }

    public UpdateResult putUndergroundFluid(final UndergroundFluidPosition undergroundFluid) {
        final long key = getUndergroundFluidKey(undergroundFluid.chunkX, undergroundFluid.chunkZ);
        final UndergroundFluidPosition storedUndergroundFluid = undergroundFluids.get(key);
        if (storedUndergroundFluid == null) {
            changedOrNewUndergroundFluids.add(key);
            undergroundFluids.put(key, undergroundFluid);
            markDirty();
            return UpdateResult.New;
        } else if (!storedUndergroundFluid.equals(undergroundFluid)) {
            changedOrNewUndergroundFluids.add(key);
            undergroundFluids.put(key, undergroundFluid);
            markDirty();
            return UpdateResult.Updated;
        }
        return UpdateResult.AlreadyKnown;
    }

    public OreVeinPosition getOreVein(int chunkX, int chunkZ) {
        final long key = getOreVeinKey(chunkX, chunkZ);
        return oreChunks.getOrDefault(key, new OreVeinPosition(dimensionId, chunkX, chunkZ, VeinType.NO_VEIN, true));
    }

    public UndergroundFluidPosition getUndergroundFluid(int chunkX, int chunkZ) {
        final long key = getUndergroundFluidKey(chunkX, chunkZ);
        return undergroundFluids
                .getOrDefault(key, UndergroundFluidPosition.getNotProspected(dimensionId, chunkX, chunkZ));
    }

    public Collection<OreVeinPosition> getAllOreVeins() {
        return oreChunks.values();
    }

    public Collection<UndergroundFluidPosition> getAllUndergroundFluids() {
        return undergroundFluids.values();
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void markDirty() {
        isDirty = true;
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
        oreChunks.long2ObjectEntrySet().removeIf(entry -> {
            OreVeinPosition val = entry.getValue();
            final boolean withinX = val.chunkX >= startChunkX && val.chunkX <= endChunkX;
            final boolean withinZ = val.chunkZ >= startChunkZ && val.chunkZ <= endChunkZ;
            return withinX && withinZ;
        });
    }
}
