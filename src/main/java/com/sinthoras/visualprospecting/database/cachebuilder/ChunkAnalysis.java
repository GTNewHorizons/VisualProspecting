package com.sinthoras.visualprospecting.database.cachebuilder;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.shorts.Short2IntArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;

// A slim, but faster version to identify >90% of veins
public class ChunkAnalysis {

    private final ObjectSet<VeinType> matchedVeins = new ObjectOpenHashSet<>();
    private final Short2IntMap oreCounts = new Short2IntArrayMap();
    private int minVeinBlockY = VP.minecraftWorldHeight;
    private short primaryMeta;
    private final String dimName;

    public ChunkAnalysis(String dimName) {
        this.dimName = dimName;
    }

    public void processMinecraftChunk(final NBTTagList tileEntities) {
        if (tileEntities == null || tileEntities.tagCount() == 0) return;
        for (int i = 0; i < tileEntities.tagCount(); i++) {
            final NBTTagCompound tile = tileEntities.getCompoundTagAt(i);
            if (tile == null || !tile.hasKey("m")) continue;

            String id = tile.getString("id");
            if (!"GT_TileEntity_Ores".equals(id) && !"bw.blockoresTE".equals(id)) {
                continue;
            }

            short meta = tile.getShort("m");
            if (Utils.isSmallOreId(meta) || meta == 0) continue;

            meta = Utils.oreIdToMaterialId(meta);
            final int blockY = tile.getInteger("y");

            oreCounts.put(meta, oreCounts.get(meta) + 1);
            if (minVeinBlockY > blockY) {
                minVeinBlockY = blockY;
            }
        }

        if (oreCounts.size() == 1) {
            primaryMeta = oreCounts.keySet().iterator().nextShort();
        } else if (oreCounts.size() > 1) {
            ShortList metaCounts = new ShortArrayList(oreCounts.keySet());
            metaCounts.sort((a, b) -> Integer.compare(oreCounts.get(b), oreCounts.get(a)));
            primaryMeta = metaCounts.getShort(0);
        }
    }

    public boolean matchesSingleVein() {
        if (oreCounts.isEmpty()) return true;
        if (oreCounts.size() > 4) return false;
        VeinTypeCaching.veinTypes.stream()
                .filter(vein -> vein.containsAllFoundOres(oreCounts.keySet(), dimName, primaryMeta, minVeinBlockY))
                .forEach(matchedVeins::add);
        return matchedVeins.size() <= 1;
    }

    // Result only valid if matchesSingleVein() returned true
    public VeinType getMatchedVein() {
        if (matchedVeins.isEmpty()) {
            return VeinType.NO_VEIN;
        }
        return matchedVeins.iterator().next();
    }

    public int getVeinBlockY() {
        return minVeinBlockY;
    }
}
