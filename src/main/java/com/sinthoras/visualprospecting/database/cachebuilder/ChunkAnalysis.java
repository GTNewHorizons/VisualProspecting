package com.sinthoras.visualprospecting.database.cachebuilder;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import gregtech.api.interfaces.IOreMaterial;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

// A slim, but faster version to identify >90% of veins
public class ChunkAnalysis {

    private final ObjectSet<VeinType> matchedVeins = new ObjectOpenHashSet<>();
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

        // spotless:off
        var byCount = oreCounts.reference2IntEntrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .collect(Collectors.toList());
        // spotless:on

        if (!byCount.isEmpty()) mostCommonOre = byCount.get(0).getKey();
    }

    public boolean matchesSingleVein() {
        if (oreCounts.isEmpty()) return true;
        if (oreCounts.size() > 4) return false;
        // spotless:off
        VeinTypeCaching.getVeinTypes().stream()
                .filter(vein -> vein.containsAllFoundOres(oreCounts.keySet(), dimName, mostCommonOre, minVeinBlockY))
                .forEach(matchedVeins::add);
        // spotless:on
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
