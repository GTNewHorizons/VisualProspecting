package com.sinthoras.visualprospecting.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Cache of ore veins and underground fluids. Stored as NBT file per dimension (e.g. {@code DIM0.dat}).
 * <p>
 * 
 * <pre>
 * Root: {
 *   "version": 3,  // int   Absence means a legacy format {@link LegacyDimensionCacheLoader}
 *   "dim":     &lt;dimensionId&gt;,      // int
 *   "ores":    &lt;ores compound&gt;,    // optional
 *   "fluids":  &lt;fluids compound&gt;   // optional
 * }
 * </pre>
 *
 * {@code ores} and {@code fluids} use a palette.
 * 
 * <pre>
 * ores: {
 *   "palette":       [String, ...],  // unique vein type
 *   "chunkX":        int[N],         // chunk X
 *   "chunkZ":        int[N],         // chunk Z
 *   "veinTypeIndex": int[N],         // index into palette
 *   "depleted":      byte[N],        // bool
 *   "source":        byte[N]         // {@link VeinSource}
 * }
 *
 * fluids: {
 *   "palette":        [String, ...], // unique fluid names
 *   "chunkX":         int[N],
 *   "chunkZ":         int[N],
 *   "fluidTypeIndex": int[N],
 *   "chunkData":      int[N * chunkDataSize], // fluid amounts for 8x8 area
 *   "chunkDataSize":  int
 * }
 * </pre>
 */
public class DimensionCache {

    public enum UpdateResult {
        AlreadyKnown,
        Updated,
        New
    }

    private static final int CURRENT_FORMAT_VERSION = 3;

    private final Long2ObjectOpenHashMap<OreVeinPosition> oreChunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<UndergroundFluidPosition> undergroundFluids = new Long2ObjectOpenHashMap<>();
    public final int dimensionId;
    private boolean isDirty = false;
    private boolean preventSaving = false;

    public DimensionCache(int dimensionId) {
        this.dimensionId = dimensionId;
    }

    public void loadFromNbt(NBTTagCompound compound) {
        int version = compound.getInteger("version");

        if (version > CURRENT_FORMAT_VERSION) {
            preventSaving = true;
            VP.LOG.warn(
                    "Dimension {}: unsupported format version {}, data will not be modified.",
                    dimensionId,
                    version);
            return;
        }

        if (version == CURRENT_FORMAT_VERSION) {
            loadOres(compound.getCompoundTag("ores"));
            loadFluids(compound.getCompoundTag("fluids"));
        } else {
            // version key absent => v2 or older
            LegacyDimensionCacheLoader.loadV2Ores(this, compound.getCompoundTag("ores"));
            LegacyDimensionCacheLoader.loadV2Fluids(this, compound.getCompoundTag("fluids"));
        }
    }

    public NBTTagCompound saveToNbt() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("version", CURRENT_FORMAT_VERSION);
        compound.setInteger("dim", dimensionId);
        compound.setTag("ores", saveOres());
        compound.setTag("fluids", saveFluids());
        isDirty = false;
        return compound;
    }

    private void loadOres(NBTTagCompound ores) {
        if (!ores.hasKey("palette")) return;

        NBTTagList paletteTag = ores.getTagList("palette", 8);
        String[] palette = new String[paletteTag.tagCount()];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = paletteTag.getStringTagAt(i);
        }

        int[] chunkXArray = ores.getIntArray("chunkX");
        int[] chunkZArray = ores.getIntArray("chunkZ");
        int[] veinTypeIndexArray = ores.getIntArray("veinTypeIndex");
        byte[] depletedArray = ores.getByteArray("depleted");
        byte[] sourceArray = ores.getByteArray("source");

        int size = chunkXArray.length;
        oreChunks.ensureCapacity(oreChunks.size() + size);

        int unknownVeinTypes = 0;
        for (int i = 0; i < size; i++) {
            int index = veinTypeIndexArray[i];
            if (index < 0 || index >= palette.length) {
                unknownVeinTypes++;
                continue;
            }
            VeinType veinType = VeinTypeCaching.getVeinType(palette[index]);
            if (veinType == VeinType.NO_VEIN) {
                unknownVeinTypes++;
                continue;
            }
            VeinSource source = VeinSource.fromByte(sourceArray[i]);
            oreChunks.put(
                    getOreVeinKey(chunkXArray[i], chunkZArray[i]),
                    new OreVeinPosition(
                            dimensionId,
                            chunkXArray[i],
                            chunkZArray[i],
                            veinType,
                            depletedArray[i] == 1,
                            source));
        }
        if (unknownVeinTypes > 0) {
            VP.LOG.warn(
                    "Dimension {}: skipped {} entries with unknown vein type names while loading cache.",
                    dimensionId,
                    unknownVeinTypes);
        }
    }

    private void loadFluids(NBTTagCompound fluids) {
        if (!fluids.hasKey("palette")) return;

        NBTTagList paletteTag = fluids.getTagList("palette", 8);
        String[] palette = new String[paletteTag.tagCount()];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = paletteTag.getStringTagAt(i);
        }

        int[] chunkXArray = fluids.getIntArray("chunkX");
        int[] chunkZArray = fluids.getIntArray("chunkZ");
        int[] fluidTypeIndexArray = fluids.getIntArray("fluidTypeIndex");
        int[] allChunkData = fluids.getIntArray("chunkData");
        int chunkDataSize = fluids.getInteger("chunkDataSize");

        int fluidCount = chunkXArray.length;
        undergroundFluids.ensureCapacity(undergroundFluids.size() + fluidCount);

        int unknownFluids = 0;
        for (int i = 0; i < fluidCount; i++) {
            int index = fluidTypeIndexArray[i];
            if (index < 0 || index >= palette.length) {
                unknownFluids++;
                continue;
            }
            Fluid fluid = FluidRegistry.getFluid(palette[index]);
            if (fluid == null) {
                unknownFluids++;
                continue;
            }
            int[][] chunks = new int[VP.undergroundFluidSizeChunkX][VP.undergroundFluidSizeChunkZ];
            int baseOffset = i * chunkDataSize;
            for (int x = 0; x < VP.undergroundFluidSizeChunkX; x++) {
                System.arraycopy(
                        allChunkData,
                        baseOffset + x * VP.undergroundFluidSizeChunkZ,
                        chunks[x],
                        0,
                        VP.undergroundFluidSizeChunkZ);
            }
            undergroundFluids.put(
                    getUndergroundFluidKey(chunkXArray[i], chunkZArray[i]),
                    new UndergroundFluidPosition(dimensionId, chunkXArray[i], chunkZArray[i], fluid, chunks));
        }
        if (unknownFluids > 0) {
            VP.LOG.warn(
                    "Dimension {}: skipped {} entries with unknown fluid names while loading cache.",
                    dimensionId,
                    unknownFluids);
        }
    }

    private NBTTagCompound saveOres() {
        NBTTagCompound compound = new NBTTagCompound();
        int size = oreChunks.size();
        if (size == 0) return compound;

        long[] sortedKeys = oreChunks.keySet().toLongArray();
        Arrays.sort(sortedKeys);

        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteIndex = new HashMap<>();
        for (long key : sortedKeys) {
            String name = oreChunks.get(key).veinType.name;
            if (!paletteIndex.containsKey(name)) {
                paletteIndex.put(name, palette.size());
                palette.add(name);
            }
        }

        int[] chunkXArray = new int[size];
        int[] chunkZArray = new int[size];
        int[] veinTypeIndexArray = new int[size];
        byte[] depletedArray = new byte[size];
        byte[] sourceArray = new byte[size];

        for (int i = 0; i < size; i++) {
            OreVeinPosition vein = oreChunks.get(sortedKeys[i]);
            chunkXArray[i] = vein.chunkX;
            chunkZArray[i] = vein.chunkZ;
            veinTypeIndexArray[i] = paletteIndex.get(vein.veinType.name);
            depletedArray[i] = vein.isDepleted() ? (byte) 1 : (byte) 0;
            sourceArray[i] = vein.getSourceByte();
        }

        NBTTagList paletteTag = new NBTTagList();
        for (String name : palette) {
            paletteTag.appendTag(new NBTTagString(name));
        }

        compound.setTag("palette", paletteTag);
        compound.setIntArray("chunkX", chunkXArray);
        compound.setIntArray("chunkZ", chunkZArray);
        compound.setIntArray("veinTypeIndex", veinTypeIndexArray);
        compound.setByteArray("depleted", depletedArray);
        compound.setByteArray("source", sourceArray);

        return compound;
    }

    private NBTTagCompound saveFluids() {
        NBTTagCompound compound = new NBTTagCompound();
        int size = undergroundFluids.size();
        if (size == 0) return compound;

        int chunkDataSize = VP.undergroundFluidSizeChunkX * VP.undergroundFluidSizeChunkZ;

        long[] sortedKeys = undergroundFluids.keySet().toLongArray();
        Arrays.sort(sortedKeys);

        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteIndex = new HashMap<>();
        for (long key : sortedKeys) {
            String name = undergroundFluids.get(key).fluid.getName();
            if (!paletteIndex.containsKey(name)) {
                paletteIndex.put(name, palette.size());
                palette.add(name);
            }
        }

        int[] chunkXArray = new int[size];
        int[] chunkZArray = new int[size];
        int[] fluidTypeIndexArray = new int[size];
        int[] allChunkData = new int[size * chunkDataSize];

        for (int i = 0; i < size; i++) {
            UndergroundFluidPosition fluid = undergroundFluids.get(sortedKeys[i]);
            chunkXArray[i] = fluid.chunkX;
            chunkZArray[i] = fluid.chunkZ;
            fluidTypeIndexArray[i] = paletteIndex.get(fluid.fluid.getName());
            int baseOffset = i * chunkDataSize;
            for (int x = 0; x < VP.undergroundFluidSizeChunkX; x++) {
                System.arraycopy(
                        fluid.chunks[x],
                        0,
                        allChunkData,
                        baseOffset + x * VP.undergroundFluidSizeChunkZ,
                        VP.undergroundFluidSizeChunkZ);
            }
        }

        NBTTagList paletteTag = new NBTTagList();
        for (String name : palette) {
            paletteTag.appendTag(new NBTTagString(name));
        }

        compound.setTag("palette", paletteTag);
        compound.setIntArray("chunkX", chunkXArray);
        compound.setIntArray("chunkZ", chunkZArray);
        compound.setIntArray("fluidTypeIndex", fluidTypeIndexArray);
        compound.setIntArray("chunkData", allChunkData);
        compound.setInteger("chunkDataSize", chunkDataSize);

        return compound;
    }

    void putOreFromLegacyLoad(OreVeinPosition pos) {
        oreChunks.put(getOreVeinKey(pos.chunkX, pos.chunkZ), pos);
    }

    void putFluidFromLegacyLoad(UndergroundFluidPosition pos) {
        undergroundFluids.put(getUndergroundFluidKey(pos.chunkX, pos.chunkZ), pos);
    }

    void ensureOreCapacityForLegacyLoad(int expectedAdditions) {
        oreChunks.ensureCapacity(oreChunks.size() + expectedAdditions);
    }

    void ensureFluidCapacityForLegacyLoad(int expectedAdditions) {
        undergroundFluids.ensureCapacity(undergroundFluids.size() + expectedAdditions);
    }

    boolean hasOres() {
        return !oreChunks.isEmpty();
    }

    boolean hasFluids() {
        return !undergroundFluids.isEmpty();
    }

    void markPreventSaving() {
        preventSaving = true;
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
        if (oreVeinPosition.veinType == VeinType.NO_VEIN) {
            return UpdateResult.AlreadyKnown;
        }

        final long key = getOreVeinKey(oreVeinPosition.chunkX, oreVeinPosition.chunkZ);
        final OreVeinPosition storedOreVeinPosition = oreChunks.get(key);
        if (storedOreVeinPosition == null) {
            oreChunks.put(key, oreVeinPosition);
            markDirty();
            return UpdateResult.New;
        }

        // Stop update if lower trusted VeinSource
        if (!oreVeinPosition.getSource().canOverwrite(storedOreVeinPosition.getSource())) {
            return UpdateResult.AlreadyKnown;
        }

        if (storedOreVeinPosition.veinType != oreVeinPosition.veinType) {
            oreChunks.put(key, oreVeinPosition.joinDepletedState(storedOreVeinPosition));
            markDirty();
            return UpdateResult.New;
        }

        // If identical entry but "better" source we update it
        if (storedOreVeinPosition.getSourceByte() != oreVeinPosition.getSourceByte()) {
            oreChunks.put(key, oreVeinPosition.joinDepletedState(storedOreVeinPosition));
            markDirty();
            return UpdateResult.Updated;
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
        clearOreVeins(startChunkX, startChunkZ, endChunkX, endChunkZ, EnumSet.noneOf(VeinSource.class));
    }

    /**
     * Reset selected veins within the area whose source is not in {@code protectedSources}.
     */
    public void clearOreVeins(int startChunkX, int startChunkZ, int endChunkX, int endChunkZ,
            EnumSet<VeinSource> protectedSources) {
        // This method iterates for each chunk mapped. In many cases, it is probably faster to iterate over chunks in
        // the area to be cleared instead. i.e. if (chunksInClearArea < totalChunksMapped) {useAltIterator()}. If
        // someone calls this enough to make it a problem, they can add that.
        boolean removedAny = oreChunks.long2ObjectEntrySet().removeIf(entry -> {
            OreVeinPosition val = entry.getValue();
            final boolean withinX = val.chunkX >= startChunkX && val.chunkX <= endChunkX;
            final boolean withinZ = val.chunkZ >= startChunkZ && val.chunkZ <= endChunkZ;
            if (!withinX || !withinZ) return false;
            return !protectedSources.contains(val.getSource());
        });
        if (removedAny) {
            markDirty();
        }
    }

    /**
     * Remove all ore veins from this dimension whose source is not in {@code protectedSources}.
     */
    public void clearOreVeinsExcept(EnumSet<VeinSource> protectedSources) {
        boolean removedAny = oreChunks.long2ObjectEntrySet()
                .removeIf(entry -> !protectedSources.contains(entry.getValue().getSource()));
        if (removedAny) {
            markDirty();
        }
    }

}
