package com.sinthoras.visualprospecting.teams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import com.gtnewhorizon.gtnhlib.teams.ITeamData;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamDataCopyReason;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.VP;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;

/**
 * Per-team prospection record. Tracks which ore veins / underground fluids a team has discovered, which veins are
 * depleted, per dimension.
 * <p>
 * Only coordinate keys are stored. Full {@code OreVeinPosition} / {@code UndergroundFluidPosition} infos (vein types,
 * fluid amounts) are recovered from {@code ServerCache}.
 * <p>
 * The vein/fluid/depleted state is stored into a blob as a single gzipped base64 {@code NBTTagString}.
 * <p>
 * On server restart the data sent by GTNHLib is not expanded until first access.
 */
public class TeamProspectionData implements ITeamData {

    public static final String DATA_KEY = "visualprospecting";

    private static final String TAG_VERSION = "version";
    private static final String TAG_BLOB = "blob";

    private static final int FORMAT_VERSION = 1;

    // Team Data - Raw
    private byte[] pendingBlob;
    // Team Data - Expanded
    private Int2ObjectMap<LongSet> discoveredVeins;
    private Int2ObjectMap<LongSet> discoveredFluids;
    private Int2ObjectMap<LongSet> depletedVeins;
    // Team Data - Status
    private boolean expanded;
    private byte[] unknownVersionBlob;
    private int unknownVersion;

    // Lazy team data expansion
    private void ensureExpanded() {
        if (expanded) return;

        discoveredVeins = new Int2ObjectOpenHashMap<>();
        discoveredFluids = new Int2ObjectOpenHashMap<>();
        depletedVeins = new Int2ObjectOpenHashMap<>();
        if (pendingBlob != null) {
            try {
                decodeBlobInto(pendingBlob, discoveredVeins, discoveredFluids, depletedVeins);
            } catch (IOException e) {
                VP.LOG.error("Failed to decode team prospection data.", e);
            }
            pendingBlob = null;
        }
        expanded = true;
    }

    // Pack an Ore vein position
    static long packVein(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    static int veinChunkX(long key) {
        return (int) (key >>> 32);
    }

    static int veinChunkZ(long key) {
        return (int) key;
    }

    // Pack an Underground Fluids position. They are on a 8x8 pattern so we can divide by 8
    static long packFluid(int chunkX, int chunkZ) {
        return ((long) (chunkX >> 3) << 32) | ((chunkZ >> 3) & 0xFFFFFFFFL);
    }

    static int fluidChunkX(long key) {
        return ((int) (key >>> 32)) << 3;
    }

    static int fluidChunkZ(long key) {
        return ((int) key) << 3;
    }

    // Add ore vein to team record
    public boolean addVein(int dim, int chunkX, int chunkZ) {
        ensureExpanded();
        return getOrCreate(discoveredVeins, dim).add(packVein(chunkX, chunkZ));
    }

    // Add underground fluid to team record
    public boolean addFluid(int dim, int chunkX, int chunkZ) {
        ensureExpanded();
        return getOrCreate(discoveredFluids, dim).add(packFluid(chunkX, chunkZ));
    }

    // Record an ore vein as depleted
    public boolean setVeinDepleted(int dim, int chunkX, int chunkZ, boolean depleted) {
        ensureExpanded();
        long key = packVein(chunkX, chunkZ);
        if (depleted) {
            return getOrCreate(depletedVeins, dim).add(key);
        }
        LongSet set = depletedVeins.get(dim);
        return set != null && set.remove(key);
    }

    // Delete all data for a team
    public void clear() {
        pendingBlob = null;
        unknownVersionBlob = null;
        unknownVersion = 0;
        discoveredVeins = new Int2ObjectOpenHashMap<>();
        discoveredFluids = new Int2ObjectOpenHashMap<>();
        depletedVeins = new Int2ObjectOpenHashMap<>();
        expanded = true;
    }

    public LongSet getDiscoveredVeinKeys(int dim) {
        ensureExpanded();
        return discoveredVeins.getOrDefault(dim, LongSets.EMPTY_SET);
    }

    public LongSet getDiscoveredFluidKeys(int dim) {
        ensureExpanded();
        return discoveredFluids.getOrDefault(dim, LongSets.EMPTY_SET);
    }

    public LongSet getDepletedVeinKeys(int dim) {
        ensureExpanded();
        return depletedVeins.getOrDefault(dim, LongSets.EMPTY_SET);
    }

    public boolean isVeinDiscovered(int dim, int chunkX, int chunkZ) {
        ensureExpanded();
        LongSet set = discoveredVeins.get(dim);
        return set != null && set.contains(packVein(chunkX, chunkZ));
    }

    public boolean isVeinDepleted(int dim, int chunkX, int chunkZ) {
        ensureExpanded();
        LongSet set = depletedVeins.get(dim);
        return set != null && set.contains(packVein(chunkX, chunkZ));
    }

    public IntSet knownDimensions() {
        ensureExpanded();
        IntSet dims = new IntOpenHashSet();
        dims.addAll(discoveredVeins.keySet());
        dims.addAll(discoveredFluids.keySet());
        dims.addAll(depletedVeins.keySet());
        return dims;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        // In case of unknown FORMAT_VERSION we write OG data untouched
        if (unknownVersionBlob != null) {
            tag.setInteger(TAG_VERSION, unknownVersion);
            tag.setString(TAG_BLOB, Base64.getEncoder().encodeToString(unknownVersionBlob));
            return;
        }

        byte[] blob;
        if (expanded) {
            blob = encodeBlob(discoveredVeins, discoveredFluids, depletedVeins);
        } else if (pendingBlob != null) {
            blob = pendingBlob;
        } else return;
        if (blob.length == 0) return;
        tag.setInteger(TAG_VERSION, FORMAT_VERSION);
        tag.setString(TAG_BLOB, Base64.getEncoder().encodeToString(blob));
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        expanded = false;
        pendingBlob = null;
        unknownVersionBlob = null;
        unknownVersion = 0;
        discoveredVeins = null;
        discoveredFluids = null;
        depletedVeins = null;

        if (!tag.hasKey(TAG_BLOB, Constants.NBT.TAG_STRING)) return;
        int version = tag.getInteger(TAG_VERSION);
        String b64 = tag.getString(TAG_BLOB);
        if (b64.isEmpty()) return;

        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(b64);
        } catch (IllegalArgumentException e) {
            VP.LOG.error("Team prospection data blob is not valid base64. Starting empty", e);
            return;
        }
        if (version != FORMAT_VERSION) {
            VP.LOG.error(
                    "Team prospection data is format version {}, this supports v{}. New changes won't be recorded.",
                    version,
                    FORMAT_VERSION);
            unknownVersion = version;
            unknownVersionBlob = raw;
            return;
        }
        pendingBlob = raw;
    }

    @Override
    public void mergeData(Team consumed, Team surviving, ITeamData oldTeamData) {
        if (oldTeamData instanceof TeamProspectionData other) {
            ensureExpanded();
            other.ensureExpanded();
            mergeFrom(other);
        }
    }

    @Override
    public void copyData(Team prevTeam, Team newTeam, UUID playerId, ITeamData prevTeamData,
            TeamDataCopyReason reason) {
        if (!(prevTeamData instanceof TeamProspectionData prev)) return;
        if (reason == TeamDataCopyReason.JoinedNewTeam && !Config.keepProspectionOnTeamLeave) return;
        ensureExpanded();
        prev.ensureExpanded();
        mergeFrom(prev);
    }

    private void mergeFrom(TeamProspectionData other) {
        mergeMap(other.discoveredVeins, discoveredVeins);
        mergeMap(other.discoveredFluids, discoveredFluids);
        mergeMap(other.depletedVeins, depletedVeins);
    }

    private static void mergeMap(Int2ObjectMap<LongSet> source, Int2ObjectMap<LongSet> target) {
        if (source == null) return;
        for (Int2ObjectMap.Entry<LongSet> entry : source.int2ObjectEntrySet()) {
            int dim = entry.getIntKey();
            getOrCreate(target, dim).addAll(entry.getValue());
        }
    }

    private static LongSet getOrCreate(Int2ObjectMap<LongSet> map, int dim) {
        LongSet set = map.get(dim);
        if (set == null) {
            set = new LongOpenHashSet();
            map.put(dim, set);
        }
        return set;
    }

    /**
     * Data structure used:<br>
     * &nbsp;int numDims<br>
     * &nbsp;for each dim: <br>
     * &nbsp;&nbsp; int dimId<br>
     * &nbsp;&nbsp; int veinCount<br>
     * &nbsp;&nbsp; long veinKeys[veinCount]<br>
     * &nbsp;&nbsp; int fluidCount;<br>
     * &nbsp;&nbsp; long fluidKeys[fluidCount]<br>
     * &nbsp;&nbsp; int depletedCount<br>
     * &nbsp;&nbsp; long depletedKeys[depletedCount]<br>
     * <br>
     * Data is sorted to improve gzip efficiency
     */
    private static byte[] encodeBlob(Int2ObjectMap<LongSet> veins, Int2ObjectMap<LongSet> fluids,
            Int2ObjectMap<LongSet> depleted) {
        IntSet allDims = new IntOpenHashSet();
        if (veins != null) allDims.addAll(veins.keySet());
        if (fluids != null) allDims.addAll(fluids.keySet());
        if (depleted != null) allDims.addAll(depleted.keySet());

        int[] dimsToWrite = allDims.intStream()
                .filter(d -> !isEmpty(veins, d) || !isEmpty(fluids, d) || !isEmpty(depleted, d)).sorted().toArray();
        if (dimsToWrite.length == 0) return EMPTY_BYTES;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos); DataOutputStream out = new DataOutputStream(gz)) {
            out.writeInt(dimsToWrite.length);
            for (int dim : dimsToWrite) {
                out.writeInt(dim);
                writeLongSet(out, veins == null ? null : veins.get(dim));
                writeLongSet(out, fluids == null ? null : fluids.get(dim));
                writeLongSet(out, depleted == null ? null : depleted.get(dim));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode team prospection blob", e);
        }
        return bos.toByteArray();
    }

    private static boolean isEmpty(Int2ObjectMap<LongSet> map, int dim) {
        if (map == null) return true;
        LongSet set = map.get(dim);
        return set == null || set.isEmpty();
    }

    private static void writeLongSet(DataOutputStream out, LongSet set) throws IOException {
        if (set == null || set.isEmpty()) {
            out.writeInt(0);
            return;
        }
        long[] sorted = set.toLongArray();
        Arrays.sort(sorted);
        out.writeInt(sorted.length);
        for (long k : sorted) {
            out.writeLong(k);
        }
    }

    private static void decodeBlobInto(byte[] blob, Int2ObjectMap<LongSet> veinsOut, Int2ObjectMap<LongSet> fluidsOut,
            Int2ObjectMap<LongSet> depletedOut) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(blob));
                DataInputStream in = new DataInputStream(gz)) {
            int numDims = in.readInt();
            for (int i = 0; i < numDims; i++) {
                int dim = in.readInt();
                readIntoSet(in, getOrCreate(veinsOut, dim));
                readIntoSet(in, getOrCreate(fluidsOut, dim));
                readIntoSet(in, getOrCreate(depletedOut, dim));
            }
        }
    }

    private static void readIntoSet(DataInputStream in, LongSet target) throws IOException {
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            target.add(in.readLong());
        }
    }

    private static final byte[] EMPTY_BYTES = new byte[0];
}
