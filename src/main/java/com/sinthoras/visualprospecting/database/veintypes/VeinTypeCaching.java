package com.sinthoras.visualprospecting.database.veintypes;

import static com.sinthoras.visualprospecting.Utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import net.minecraft.util.EnumChatFormatting;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.sinthoras.visualprospecting.Tags;

import bartworks.system.material.Werkstoff;
import bartworks.system.oregen.BWOreLayer;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.SearchField;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OreMixes;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class VeinTypeCaching {

    private static final Object2ObjectMap<String, VeinType> veinTypes = new Object2ObjectOpenHashMap<>();

    public static void init() {
        veinTypes.put(Tags.ORE_MIX_NONE_NAME, VeinType.NO_VEIN);

        for (OreMixes mix : OreMixes.values()) {
            veinTypes.put(mix.oreMixBuilder.oreMixName, new VeinType(mix.oreMixBuilder));
        }

        for (BWOreLayer vein : BWOreLayer.sList) {
            final IOreMaterialProvider oreMaterialProvider;
            if (containsBartworksOres(vein.bwOres)) {
                oreMaterialProvider = getRepresentativeProvider(
                        vein.mWorldGenName,
                        new int[] { vein.mPrimaryMeta, vein.mSecondaryMeta, vein.mBetweenMeta, vein.mSporadicMeta });
            } else {
                oreMaterialProvider = new GregTechOreMaterialProvider(
                        GregTechAPI.sGeneratedMaterials[(short) vein.mPrimaryMeta]);
            }
            veinTypes.put(
                    vein.mWorldGenName,
                    new VeinType(
                            vein.mWorldGenName,
                            oreMaterialProvider,
                            vein.mSize,
                            (short) vein.mPrimaryMeta,
                            (short) vein.mSecondaryMeta,
                            (short) vein.mBetweenMeta,
                            (short) vein.mSporadicMeta,
                            vein.mMinY,
                            vein.mMaxY,
                            vein.getDimName()));
        }
    }

    public static @NotNull VeinType getVeinType(String veinTypeName) {
        return veinTypes.getOrDefault(veinTypeName, VeinType.NO_VEIN);
    }

    public static Collection<VeinType> getVeinTypes() {
        return veinTypes.values();
    }

    public static void recalculateNEISearch() {
        if (isNEIInstalled()) {
            final boolean isSearchActive = SearchField.searchInventories();
            final String searchString = NEIClientConfig.getSearchExpression().toLowerCase();
            final Pattern filterPattern = SearchField.getPattern(searchString);

            for (VeinType veinType : veinTypes.values()) {
                if (veinType == VeinType.NO_VEIN) continue;
                if (isSearchActive && !searchString.isEmpty()) {
                    List<String> searchableStrings = new ArrayList<>(veinType.getOreMaterialNames());
                    searchableStrings.add(veinType.getVeinName());
                    final boolean match = searchableStrings.stream()
                            .map(EnumChatFormatting::getTextWithoutFormattingCodes).map(String::toLowerCase)
                            .anyMatch(searchableString -> filterPattern.matcher(searchableString).find());

                    veinType.setNEISearchHighlight(match);
                } else {
                    veinType.setNEISearchHighlight(true);
                }
            }
        }
    }

    private static boolean containsBartworksOres(byte bwOres) {
        return (bwOres & 0b1111) != 0;
    }

    private static IOreMaterialProvider getRepresentativeProvider(String veinName, int[] ores) {
        for (int meta : ores) {
            Werkstoff werkstoff = Werkstoff.werkstoffHashMap.get((short) meta);
            if (werkstoff == null) continue;
            String defaultName = werkstoff.getDefaultName();
            // also try lobbing a few characters off the end of the vein name because bart
            if (StringUtils.endsWithIgnoreCase(veinName, defaultName)
                    || StringUtils.containsIgnoreCase(veinName, defaultName.substring(0, defaultName.length() - 2))) {
                return new BartworksOreMaterialProvider(werkstoff);
            }
        }

        Werkstoff primaryMaterial = Arrays.stream(ores).mapToObj(meta -> Werkstoff.werkstoffHashMap.get((short) meta))
                .filter(Objects::nonNull).findFirst().orElse(null);

        return primaryMaterial == null ? new GregTechOreMaterialProvider(Materials._NULL)
                : new BartworksOreMaterialProvider(primaryMaterial);
    }
}
