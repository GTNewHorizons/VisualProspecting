package com.sinthoras.visualprospecting.database.veintypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sinthoras.visualprospecting.Tags;

import gregtech.api.enums.OreMixes;
import gregtech.api.interfaces.IOreMaterial;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

public class VeinTypeCaching {

    private static final Object2ObjectMap<String, VeinType> veinTypes = new Object2ObjectOpenHashMap<>();

    // Dimensions without registered OreMixes are not expected to match anything
    private static final ObjectSet<String> dimensionsWithVeins = new ObjectOpenHashSet<>();

    // VeinTypes indexed by primary/secondary Ore in OreMix. For faster matching
    private static final Reference2ObjectMap<IOreMaterial, List<VeinType>> veinTypesByPrimaryOrSecondary = new Reference2ObjectOpenHashMap<>();

    public static void init() {
        veinTypes.clear();
        veinTypes.put(Tags.ORE_MIX_NONE_NAME, VeinType.NO_VEIN);

        for (OreMixes mix : OreMixes.values()) {
            veinTypes.put(mix.oreMixBuilder.oreMixName, new VeinType(mix.oreMixBuilder));
        }

        dimensionsWithVeins.clear();
        veinTypesByPrimaryOrSecondary.clear();
        for (VeinType veinType : veinTypes.values()) {
            if (veinType == VeinType.NO_VEIN) continue;
            dimensionsWithVeins.addAll(veinType.getAllowedDimensions());
            indexByOre(veinType.primaryOre, veinType);
            if (veinType.secondaryOre != veinType.primaryOre) {
                indexByOre(veinType.secondaryOre, veinType);
            }
        }
    }

    private static void indexByOre(IOreMaterial ore, VeinType veinType) {
        if (ore == null) return;
        List<VeinType> matchingVeinTypes = veinTypesByPrimaryOrSecondary.get(ore);
        if (matchingVeinTypes == null) {
            matchingVeinTypes = new ArrayList<>();
            veinTypesByPrimaryOrSecondary.put(ore, matchingVeinTypes);
        }
        matchingVeinTypes.add(veinType);
    }

    public static boolean hasVeinsInDimension(String dimensionName) {
        return dimensionsWithVeins.contains(dimensionName);
    }

    public static @NotNull VeinType getVeinType(String veinTypeName) {
        return veinTypes.getOrDefault(veinTypeName, VeinType.NO_VEIN);
    }

    public static Collection<VeinType> getVeinTypes() {
        return veinTypes.values();
    }

    public static List<VeinType> getVeinTypesForOre(IOreMaterial dominantOre) {
        final List<VeinType> matchingVeinTypes = veinTypesByPrimaryOrSecondary.get(dominantOre);
        return matchingVeinTypes == null ? Collections.emptyList() : matchingVeinTypes;
    }

    public static void recalculateSearch(@Nullable Pattern filterPattern) {
        for (VeinType veinType : veinTypes.values()) {
            if (veinType == VeinType.NO_VEIN) continue;
            if (filterPattern != null) {
                List<String> searchableStrings = new ArrayList<>(veinType.getOreMaterialNames());
                searchableStrings.add(veinType.getVeinName());
                final boolean match = searchableStrings.stream().map(EnumChatFormatting::getTextWithoutFormattingCodes)
                        .map(String::toLowerCase)
                        .anyMatch(searchableString -> filterPattern.matcher(searchableString).find());

                veinType.setNEISearchHighlight(match);
            } else {
                veinType.setNEISearchHighlight(true);
            }
        }
    }
}
