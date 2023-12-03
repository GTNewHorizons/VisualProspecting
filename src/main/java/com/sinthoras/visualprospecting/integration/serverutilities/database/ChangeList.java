package com.sinthoras.visualprospecting.integration.serverutilities.database;

import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class ChangeList {

    private final NavigableMap<Long, List<ChunkPosId>> changeListOreVeins = new TreeMap<>(Comparator.naturalOrder());
    private final NavigableMap<Long, List<ChunkPosId>> changeListUndergroundFluids = new TreeMap<>(Comparator.naturalOrder());
    private final Map<ChunkPosId, Long> oreVeinTimestampLUT = new HashMap<>();
    private final Map<ChunkPosId, Long> undergroundFluidTimestampLUT = new HashMap<>();

    public void addOreVein(OreVeinPosition oreVein, long currentTimestamp) {
        ChunkPosId posId = new ChunkPosId(oreVein);
        add(posId, oreVeinTimestampLUT, changeListOreVeins, currentTimestamp);
    }

    public void addUndergroundFluid(UndergroundFluidPosition undergroundFluid, long currentTimestamp) {
        ChunkPosId posId = new ChunkPosId(undergroundFluid);
        add(posId, undergroundFluidTimestampLUT, changeListUndergroundFluids, currentTimestamp);
    }

    private void add(ChunkPosId posId, Map<ChunkPosId, Long> lut, NavigableMap<Long, List<ChunkPosId>> changeMap, long currentTimestamp) {
        Long existingTimestamp = lut.get(posId);

        if (existingTimestamp != null) {
            List<ChunkPosId> changeList = changeMap.get(existingTimestamp);
            if (changeList != null)
                changeList.remove(posId);
        }

        changeMap.compute(currentTimestamp, (key, value) -> {
            if (value == null) {
                List<ChunkPosId> posIdList = new ArrayList<>();
                posIdList.add(posId);

                return posIdList;
            } else {
                if (!value.contains(posId))
                    value.add(posId);

                return value;
            }
        });

        lut.put(posId, currentTimestamp);
    }

    public ChangesPair getAllSince(long timestamp) {
        List<ChunkPosId> oreVeinList = getListOfChangesSince(timestamp, changeListOreVeins);
        List<ChunkPosId> undergroundFluidList = getListOfChangesSince(timestamp, changeListUndergroundFluids);

        return new ChangesPair(oreVeinList, undergroundFluidList);
    }

    private List<ChunkPosId> getListOfChangesSince(long timestamp, NavigableMap<Long, List<ChunkPosId>> map) {
        if (map.isEmpty())
            return new ArrayList<>();

        final long latest = map.lastKey();
        if (timestamp > latest)
            return new ArrayList<>();

        return map.subMap(timestamp, true, latest, true)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public void clear() {
        changeListOreVeins.clear();
        oreVeinTimestampLUT.clear();
        changeListUndergroundFluids.clear();
        undergroundFluidTimestampLUT.clear();
    }

    private int getByteSize() {
        final int oreByteSize = Integer.BYTES + (changeListOreVeins.size() * (Long.BYTES + Integer.BYTES)) + (oreVeinTimestampLUT.size() * ChunkPosId.BYTES);
        final int fluidByteSize = Integer.BYTES + (changeListUndergroundFluids.size() * (Long.BYTES + Integer.BYTES)) + (undergroundFluidTimestampLUT.size() * ChunkPosId.BYTES);

        return oreByteSize + fluidByteSize;
    }

    public ByteBuffer toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(getByteSize());

        writeChangeMapBytes(buf, changeListOreVeins);
        writeChangeMapBytes(buf, changeListUndergroundFluids);

        buf.flip();
        return buf;
    }

    private void writeChangeMapBytes(ByteBuffer buf, NavigableMap<Long, List<ChunkPosId>> changeMap) {
        buf.putInt(changeMap.size());
        changeMap.forEach((timestamp, changeList) -> {
            buf.putLong(timestamp);

            buf.putInt(changeList.size());
            for (ChunkPosId posId : changeList) {
                buf.putInt(posId.chunkX);
                buf.putInt(posId.chunkZ);
                buf.putInt(posId.dimId);
            }
        });
    }

    public void fromBytes(ByteBuffer buf) {
        try {
            MapLutPair oreVeinsPair = readChangeMapBytes(buf);
            MapLutPair undergroundFluidsPair = readChangeMapBytes(buf);

            changeListOreVeins.putAll(oreVeinsPair.map);
            oreVeinTimestampLUT.putAll(oreVeinsPair.lut);
            changeListUndergroundFluids.putAll(undergroundFluidsPair.map);
            undergroundFluidTimestampLUT.putAll(undergroundFluidsPair.lut);
        } catch (BufferUnderflowException exception) {
            exception.printStackTrace();
        }
    }

    private MapLutPair readChangeMapBytes(ByteBuffer buf) {
        NavigableMap<Long, List<ChunkPosId>> changeMap = new TreeMap<>();
        Map<ChunkPosId, Long> lut = new HashMap<>();

        final int changeMapSize = buf.getInt();
        for (int i = 0; i < changeMapSize; i++) {
            List<ChunkPosId> changeList = new ArrayList<>();

            final long timestamp = buf.getLong();

            final int changeListSize = buf.getInt();
            for (int j = 0; j < changeListSize; j++) {
                final int chunkX = buf.getInt();
                final int chunkZ = buf.getInt();
                final int dimId = buf.getInt();

                ChunkPosId posId = new ChunkPosId(chunkX, chunkZ, dimId);

                changeList.add(posId);
                lut.put(posId, timestamp);
            }

            changeMap.put(timestamp, changeList);
        }

        return new MapLutPair(changeMap, lut);
    }

    public static class ChunkPosId {

        private static final int BYTES = Integer.BYTES * 3;

        public final int chunkX;
        public final int chunkZ;
        public final int dimId;

        private ChunkPosId(int chunkX, int chunkZ, int dimId) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.dimId = dimId;
        }

        public ChunkPosId(OreVeinPosition oreVein) {
            this.chunkX = oreVein.chunkX;
            this.chunkZ = oreVein.chunkZ;
            this.dimId = oreVein.dimensionId;
        }

        public ChunkPosId(UndergroundFluidPosition undergroundFluid) {
            this.chunkX = undergroundFluid.chunkX;
            this.chunkZ = undergroundFluid.chunkZ;
            this.dimId = undergroundFluid.dimensionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkPosId that = (ChunkPosId) o;
            return chunkX == that.chunkX && chunkZ == that.chunkZ && dimId == that.dimId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkX, chunkZ, dimId);
        }
    }

    private static final class MapLutPair {
        private final NavigableMap<Long, List<ChunkPosId>> map;
        private final Map<ChunkPosId, Long> lut;

        private MapLutPair(NavigableMap<Long, List<ChunkPosId>> map, Map<ChunkPosId, Long> lut) {
            this.map = map;
            this.lut = lut;
        }
    }

    public static final class ChangesPair {
        private final List<ChunkPosId> oreVeinList;
        private final List<ChunkPosId> undergroundFluidList;

        public ChangesPair(List<ChunkPosId> oreVeinList, List<ChunkPosId> undergroundFluidList) {
            this.oreVeinList = oreVeinList;
            this.undergroundFluidList = undergroundFluidList;
        }

        public boolean isEmpty() {
                return oreVeinList.isEmpty() && undergroundFluidList.isEmpty();
            }

        public List<ChunkPosId> oreVeinList() {
            return oreVeinList;
        }

        public List<ChunkPosId> undergroundFluidList() {
            return undergroundFluidList;
        }
    }
}
