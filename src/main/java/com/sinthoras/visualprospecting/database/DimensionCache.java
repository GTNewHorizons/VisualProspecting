package com.sinthoras.visualprospecting.database;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMaps;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class DimensionCache {

    public enum UpdateResult {
        AlreadyKnown,
        Updated,
        New
    }

    private static final File oldIdFile = new File(Tags.VISUALPROSPECTING_DIR, "veintypesLUT");
    private static Short2ObjectMap<String> idConversionMap;
    private final Long2ObjectMap<OreVeinPosition> oreChunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<UndergroundFluidPosition> undergroundFluids = new Long2ObjectOpenHashMap<>();
    public final int dimensionId;
    private boolean isDirty = false;
    private boolean preventSaving = false;

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
        for (OreVeinPosition vein : oreChunks.values()) {
            NBTTagCompound veinCompound = new NBTTagCompound();
            veinCompound.setInteger("chunkX", vein.chunkX);
            veinCompound.setInteger("chunkZ", vein.chunkZ);
            veinCompound.setString("veinTypeName", vein.veinType.name);
            veinCompound.setBoolean("depleted", vein.isDepleted());
            compound.setTag(String.valueOf(getOreVeinKey(vein.chunkX, vein.chunkZ)), veinCompound);
        }
        return compound;
    }

    private NBTTagCompound saveFluids() {
        NBTTagCompound compound = new NBTTagCompound();
        for (UndergroundFluidPosition fluid : undergroundFluids.values()) {
            NBTTagCompound fluidCompound = new NBTTagCompound();
            fluidCompound.setInteger("chunkX", fluid.chunkX);
            fluidCompound.setInteger("chunkZ", fluid.chunkZ);
            fluidCompound.setString("fluidName", fluid.fluid.getName());
            NBTTagList chunkList = new NBTTagList();
            for (int i = 0; i < VP.undergroundFluidSizeChunkX; i++) {
                chunkList.appendTag(new NBTTagIntArray(fluid.chunks[i]));
            }
            fluidCompound.setTag("chunks", chunkList);
            compound.setTag(String.valueOf(getUndergroundFluid(fluid.chunkX, fluid.chunkZ)), fluidCompound);
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
            boolean depleted = veinCompound.getBoolean("depleted");
            VeinType veinType;
            if (veinCompound.hasKey("veinTypeId")) {
                veinType = getVeinFromId(veinCompound.getShort("veinTypeId"));
                if (veinType == null) return;
                markDirty();
            } else {
                veinType = VeinTypeCaching.getVeinType(veinCompound.getString("veinTypeName"));
            }

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
                final VeinType veinType = getVeinFromId((short) (veinTypeId & 0x7FFF));
                if (veinType == null) return;

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
            markDirty();
        }
    }

    public UpdateResult putOreVein(final OreVeinPosition oreVeinPosition) {
        final long key = getOreVeinKey(oreVeinPosition.chunkX, oreVeinPosition.chunkZ);
        final OreVeinPosition storedOreVeinPosition = oreChunks.get(key);
        if (storedOreVeinPosition == null) {
            oreChunks.put(key, oreVeinPosition);
            markDirty();
            return UpdateResult.New;
        }
        if (storedOreVeinPosition.veinType != oreVeinPosition.veinType) {
            oreChunks.put(key, oreVeinPosition.joinDepletedState(storedOreVeinPosition));
            markDirty();
            return UpdateResult.New;
        }
        return UpdateResult.AlreadyKnown;
    }

    public UpdateResult putUndergroundFluid(final UndergroundFluidPosition undergroundFluid) {
        final long key = getUndergroundFluidKey(undergroundFluid.chunkX, undergroundFluid.chunkZ);
        final UndergroundFluidPosition storedUndergroundFluid = undergroundFluids.get(key);
        if (storedUndergroundFluid == null) {
            undergroundFluids.put(key, undergroundFluid);
            markDirty();
            return UpdateResult.New;
        } else if (!storedUndergroundFluid.equals(undergroundFluid)) {
            undergroundFluids.put(key, undergroundFluid);
            markDirty();
            return UpdateResult.Updated;
        }
        return UpdateResult.AlreadyKnown;
    }

    public OreVeinPosition getOreVein(int chunkX, int chunkZ) {
        final long key = getOreVeinKey(chunkX, chunkZ);
        return oreChunks.getOrDefault(key, OreVeinPosition.EMPTY_VEIN);
    }

    public UndergroundFluidPosition getUndergroundFluid(int chunkX, int chunkZ) {
        final long key = getUndergroundFluidKey(chunkX, chunkZ);
        return undergroundFluids.getOrDefault(key, UndergroundFluidPosition.NOT_PROSPECTED);
    }

    public Collection<OreVeinPosition> getAllOreVeins() {
        return oreChunks.values();
    }

    public Collection<UndergroundFluidPosition> getAllUndergroundFluids() {
        return undergroundFluids.values();
    }

    public boolean isDirty() {
        return isDirty && !preventSaving;
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

    private @Nullable VeinType getVeinFromId(short veinTypeId) {
        final String veinTypeName = getIdConversionMap().get(veinTypeId);
        if (veinTypeName == null) {
            preventSaving = true;
            VP.LOG.warn(
                    "Not loading ores in dimension {}. Couldn't find vein type for id {}, file {} may be missing.",
                    dimensionId,
                    veinTypeId,
                    oldIdFile.getAbsolutePath());
            return null;
        }
        return VeinTypeCaching.getVeinType(veinTypeName);
    }

    private static Short2ObjectMap<String> getIdConversionMap() {
        if (idConversionMap != null) {
            return idConversionMap;
        }

        if (!oldIdFile.exists()) {
            return Short2ObjectMaps.emptyMap();
        }
        try {
            final Gson gson = new Gson();
            final Reader reader = Files.newBufferedReader(oldIdFile.toPath());
            final Map<String, Short> map = gson.fromJson(reader, new TypeToken<Map<String, Short>>() {}.getType());
            reader.close();
            Short2ObjectMap<String> result = new Short2ObjectOpenHashMap<>();
            result.put((short) 0, Tags.ORE_MIX_NONE_NAME);
            for (Map.Entry<String, Short> entry : map.entrySet()) {
                result.put(entry.getValue().shortValue(), entry.getKey());
            }
            return idConversionMap = result;
        } catch (IOException e) {
            e.printStackTrace();
            return Short2ObjectMaps.emptyMap();
        }
    }
}
