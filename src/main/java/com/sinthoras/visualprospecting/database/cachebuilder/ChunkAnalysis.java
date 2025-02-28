package com.sinthoras.visualprospecting.database.cachebuilder;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.block.Block;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import gregtech.api.interfaces.IOreMaterial;
import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;
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

    @SuppressWarnings("unchecked")
    public void processMinecraftChunk(final PartiallyLoadedChunk chunk) {
        for (int y = 0; y < PartiallyLoadedChunk.CHUNK_HEIGHT; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    Block block = chunk.getBlock(x, y, z);
                    int meta = chunk.getBlockMeta(x, y, z);

                    try (OreInfo<IOreMaterial> info = OreManager.getOreInfo(block, meta)) {
                        if (info == null || info.isSmall) continue;

                        oreCounts.addTo(info.material, 1);

                        if (minVeinBlockY > y) {
                            minVeinBlockY = y;
                        }
                    }
                }
            }
        }

        // spotless:off
        var byCount = oreCounts.reference2IntEntrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .collect(Collectors.toList());
        // spotless:on

        if (byCount.size() >= 1) mostCommonOre = byCount.get(0).getKey();
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
