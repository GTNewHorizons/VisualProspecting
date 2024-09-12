package com.sinthoras.visualprospecting.database.cachebuilder;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;

// A slim, but faster version to identify >90% of veins
public class ChunkAnalysis {

    private final ShortSet ores = new ShortOpenHashSet();
    private final ObjectSet<VeinType> matchedVeins = new ObjectOpenHashSet<>();
    private int minVeinBlockY = VP.minecraftWorldHeight;

    public void processMinecraftChunk(final NBTTagList tileEntities) {
        if (tileEntities == null || tileEntities.tagCount() == 0) return;
        for (int i = 0; i < tileEntities.tagCount(); i++) {
            final NBTTagCompound tile = tileEntities.getCompoundTagAt(i);
            if (tile == null || !tile.hasKey("m")) continue;
            final String tagId = tile.getString("id");

            if (!tagId.equals("GT_TileEntity_Ores")) {
                continue;
            }

            short meta = tile.getShort("m");
            if (Utils.isSmallOreId(meta) || meta == 0) continue;

            meta = Utils.oreIdToMaterialId(meta);
            final int blockY = tile.getInteger("y");

            ores.add(meta);
            if (minVeinBlockY > blockY) {
                minVeinBlockY = blockY;
            }
        }
    }

    public boolean matchesSingleVein() {
        VeinTypeCaching.veinTypes.stream().filter(veinType -> veinType.matches(ores)).forEach(matchedVeins::add);
        return matchedVeins.size() <= 1;
    }

    // Result only valid if matchesSingleVein() returned true
    public VeinType getMatchedVein() {
        if (matchedVeins.isEmpty()) {
            return VeinType.NO_VEIN;
        }
        return matchedVeins.stream().findAny().get();
    }

    public int getVeinBlockY() {
        return minVeinBlockY;
    }
}
