package com.sinthoras.visualprospecting.database.cachebuilder;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import gregtech.api.interfaces.IOreMaterial;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

// A slim, but faster version to identify >90% of veins
public class ChunkAnalysis {

    private VeinType matchedVein = VeinType.NO_VEIN;
    private final Reference2IntOpenHashMap<IOreMaterial> oreCounts = new Reference2IntOpenHashMap<>();
    private int minVeinBlockY = VP.minecraftWorldHeight;
    private IOreMaterial mostCommonOre;
    private final String dimName;

    public ChunkAnalysis(String dimName) {
        this.dimName = dimName;
    }

    public void processMinecraftChunk(final PartiallyLoadedChunk chunk) {
        chunk.forEachOre((x, y, z, info) -> {
            oreCounts.addTo(info.material, 1);

            if (minVeinBlockY > y) {
                minVeinBlockY = y;
            }
        });

        int highestOreCount = -1;
        for (var entry : oreCounts.reference2IntEntrySet()) {
            if (entry.getIntValue() > highestOreCount) {
                highestOreCount = entry.getIntValue();
                mostCommonOre = entry.getKey();
            }
        }
    }

    public boolean matchesSingleVein() {
        if (oreCounts.isEmpty()) return true;
        if (oreCounts.size() > 4) return false;
        for (VeinType vein : VeinTypeCaching.getVeinTypesForOre(mostCommonOre)) {
            if (vein.containsAllFoundOres(oreCounts.keySet(), dimName, mostCommonOre, minVeinBlockY)) {
                if (matchedVein != VeinType.NO_VEIN) {
                    return false;
                }
                matchedVein = vein;
            }
        }
        return matchedVein != VeinType.NO_VEIN;
    }

    // Result only valid if matchesSingleVein() returned true
    public VeinType getMatchedVein() {
        return matchedVein;
    }

    public int getVeinBlockY() {
        return minVeinBlockY;
    }
}
