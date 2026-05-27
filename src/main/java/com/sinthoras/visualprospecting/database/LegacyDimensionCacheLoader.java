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
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMaps;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

/**
 * One-time loaders for legacy on-disk formats that {@link DimensionCache}.
 * <ul>
 * <li><b>v0</b> — pre-NBT raw binary (separate files under {@code oreChunks/} and {@code undergroundFluids/}). Vein
 * types referenced by numeric ID via a {@code veintypesLUT} JSON file.</li>
 * <li><b>v1</b> — NBT compound-per-vein nested under {@code ores}/{@code fluids}. May still use numeric IDs.</li>
 * <li><b>v2</b> — NBT parallel-array layout (no {@code version} key on the root compound).</li>
 * </ul>
 */
final class LegacyDimensionCacheLoader {

    private static final File OLD_ID_FILE = new File(Tags.VISUALPROSPECTING_DIR, "veintypesLUT");
    private static Short2ObjectMap<String> idConversionMap;

    private LegacyDimensionCacheLoader() {}

    @SuppressWarnings("unchecked")
    static void loadV2Ores(DimensionCache target, NBTTagCompound ores) {
        // v2 (parallel arrays) vs v1 (one nested compound per vein)
        if (ores.hasKey("chunkX") && ores.hasKey("chunkZ")) {
            int[] chunkXArray = ores.getIntArray("chunkX");
            int[] chunkZArray = ores.getIntArray("chunkZ");
            byte[] depletedFlags = ores.getByteArray("depleted");
            NBTTagList veinTypeNamesList = ores.getTagList("veinTypeNames", 8);

            int size = chunkXArray.length;
            int unknownVeinTypes = 0;
            target.ensureOreCapacityForLegacyLoad(size);

            for (int i = 0; i < size; i++) {
                int chunkX = chunkXArray[i];
                int chunkZ = chunkZArray[i];
                boolean depleted = depletedFlags[i] == 1;
                String veinTypeName = veinTypeNamesList.getStringTagAt(i);
                VeinType veinType = VeinTypeCaching.getVeinType(veinTypeName);
                if (veinType == VeinType.NO_VEIN) {
                    unknownVeinTypes++;
                    continue;
                }
                target.putOreFromLegacyLoad(
                        new OreVeinPosition(
                                target.dimensionId,
                                chunkX,
                                chunkZ,
                                veinType,
                                depleted,
                                VeinSource.UNKNOWN));
            }
            if (unknownVeinTypes > 0) {
                VP.LOG.warn(
                        "Dimension {}: skipped {} entries with unknown vein type names while loading cache.",
                        target.dimensionId,
                        unknownVeinTypes);
            }
        } else {
            for (NBTBase base : (Collection<NBTBase>) ores.tagMap.values()) {
                NBTTagCompound veinCompound = (NBTTagCompound) base;
                int chunkX = veinCompound.getInteger("chunkX");
                int chunkZ = veinCompound.getInteger("chunkZ");
                boolean depleted = veinCompound.getBoolean("depleted");
                VeinType veinType;
                if (veinCompound.hasKey("veinTypeId")) {
                    veinType = getVeinFromId(target, veinCompound.getShort("veinTypeId"));
                    if (veinType == null) continue;
                } else {
                    veinType = VeinTypeCaching.getVeinType(veinCompound.getString("veinTypeName"));
                    if (veinType == VeinType.NO_VEIN) continue;
                }
                target.putOreFromLegacyLoad(
                        new OreVeinPosition(
                                target.dimensionId,
                                chunkX,
                                chunkZ,
                                veinType,
                                depleted,
                                VeinSource.UNKNOWN));
            }
        }
        if (target.hasOres()) target.markDirty();
    }

    @SuppressWarnings("unchecked")
    static void loadV2Fluids(DimensionCache target, NBTTagCompound fluids) {
        // v2 (parallel arrays) vs v1 (one nested compound per fluid)
        if (fluids.hasKey("chunkX") && fluids.hasKey("chunkZ")) {
            int[] chunkXArray = fluids.getIntArray("chunkX");
            int[] chunkZArray = fluids.getIntArray("chunkZ");
            int[] allChunkData = fluids.getIntArray("chunkData");
            int chunkDataSize = fluids.getInteger("chunkDataSize");
            NBTTagList fluidNamesList = fluids.getTagList("fluidNames", 8);

            int fluidCount = chunkXArray.length;
            int unknownFluids = 0;
            target.ensureFluidCapacityForLegacyLoad(fluidCount);

            for (int fluidIndex = 0; fluidIndex < fluidCount; fluidIndex++) {
                int chunkX = chunkXArray[fluidIndex];
                int chunkZ = chunkZArray[fluidIndex];
                String fluidName = fluidNamesList.getStringTagAt(fluidIndex);
                Fluid fluid = FluidRegistry.getFluid(fluidName);
                if (fluid == null) {
                    unknownFluids++;
                    continue;
                }
                int[][] chunks = new int[VP.undergroundFluidSizeChunkX][VP.undergroundFluidSizeChunkZ];
                int baseOffset = fluidIndex * chunkDataSize;
                for (int x = 0; x < VP.undergroundFluidSizeChunkX; x++) {
                    System.arraycopy(
                            allChunkData,
                            baseOffset + x * VP.undergroundFluidSizeChunkZ,
                            chunks[x],
                            0,
                            VP.undergroundFluidSizeChunkZ);
                }
                target.putFluidFromLegacyLoad(
                        new UndergroundFluidPosition(target.dimensionId, chunkX, chunkZ, fluid, chunks));
            }
            if (unknownFluids > 0) {
                VP.LOG.warn(
                        "Dimension {}: skipped {} entries with unknown fluid names while loading cache.",
                        target.dimensionId,
                        unknownFluids);
            }
        } else {
            for (NBTBase base : (Collection<NBTBase>) fluids.tagMap.values()) {
                NBTTagCompound fluidCompound = (NBTTagCompound) base;
                int chunkX = fluidCompound.getInteger("chunkX");
                int chunkZ = fluidCompound.getInteger("chunkZ");
                String fluidName = fluidCompound.getString("fluidName");
                Fluid fluid = FluidRegistry.getFluid(fluidName);
                if (fluid == null) continue;
                int[][] chunks = new int[VP.undergroundFluidSizeChunkX][VP.undergroundFluidSizeChunkZ];
                NBTTagList chunkList = fluidCompound.getTagList("chunks", 11);
                for (int i = 0; i < VP.undergroundFluidSizeChunkX; i++) {
                    chunks[i] = chunkList.func_150306_c(i);
                }
                target.putFluidFromLegacyLoad(
                        new UndergroundFluidPosition(target.dimensionId, chunkX, chunkZ, fluid, chunks));
            }
        }
        if (target.hasFluids()) target.markDirty();
    }

    static void loadV0Binary(DimensionCache target, @Nullable ByteBuffer oreChunksBuffer,
            @Nullable ByteBuffer undergroundFluidsBuffer) {
        if (oreChunksBuffer != null) {
            while (oreChunksBuffer.remaining() >= Integer.BYTES * 2 + Short.BYTES) {
                final int chunkX = oreChunksBuffer.getInt();
                final int chunkZ = oreChunksBuffer.getInt();
                final short veinTypeId = oreChunksBuffer.getShort();
                final boolean depleted = (veinTypeId & 0x8000) > 0;
                final VeinType veinType = getVeinFromId(target, (short) (veinTypeId & 0x7FFF));
                if (veinType == null) continue;

                target.putOreFromLegacyLoad(
                        new OreVeinPosition(
                                target.dimensionId,
                                chunkX,
                                chunkZ,
                                veinType,
                                depleted,
                                VeinSource.UNKNOWN));
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
                    target.putFluidFromLegacyLoad(
                            new UndergroundFluidPosition(target.dimensionId, chunkX, chunkZ, fluid, chunks));
                }
            }
        }
    }

    private static @Nullable VeinType getVeinFromId(DimensionCache target, short veinTypeId) {
        final String veinTypeName = getIdConversionMap().get(veinTypeId);
        if (veinTypeName == null) {
            target.markPreventSaving();
            VP.LOG.warn(
                    "Not loading ores in dimension {}. Couldn't find vein type for id {}, file {} may be missing.",
                    target.dimensionId,
                    veinTypeId,
                    OLD_ID_FILE.getAbsolutePath());
            return null;
        }
        return VeinTypeCaching.getVeinType(veinTypeName);
    }

    private static Short2ObjectMap<String> getIdConversionMap() {
        if (idConversionMap != null) {
            return idConversionMap;
        }

        if (!OLD_ID_FILE.exists()) {
            return Short2ObjectMaps.emptyMap();
        }
        try {
            final Gson gson = new Gson();
            final Reader reader = Files.newBufferedReader(OLD_ID_FILE.toPath());
            final Map<String, Short> map = gson.fromJson(reader, new TypeToken<Map<String, Short>>() {}.getType());
            reader.close();
            Short2ObjectMap<String> result = new Short2ObjectOpenHashMap<>();
            result.put((short) 0, Tags.ORE_MIX_NONE_NAME);
            for (Map.Entry<String, Short> entry : map.entrySet()) {
                result.put(entry.getValue().shortValue(), entry.getKey());
            }
            return idConversionMap = result;
        } catch (IOException e) {
            VP.LOG.error("Failed to read legacy file at {}", OLD_ID_FILE.getAbsolutePath(), e);
            return Short2ObjectMaps.emptyMap();
        }
    }
}
