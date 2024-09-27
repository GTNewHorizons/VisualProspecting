package com.sinthoras.visualprospecting.database.veintypes;

import static com.sinthoras.visualprospecting.Utils.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.regex.Pattern;

import net.minecraft.util.EnumChatFormatting;

import org.apache.commons.lang3.StringUtils;

import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;

import bartworks.system.material.Werkstoff;
import bartworks.system.oregen.BWOreLayer;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.SearchField;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OreMixes;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class VeinTypeCaching implements Runnable {

    private static final Short2ObjectMap<VeinType> veinTypeLookupTableForIds = new Short2ObjectOpenHashMap<>();
    private static final Map<String, VeinType> veinTypeLookupTableForNames = new HashMap<>();
    private static final Object2ShortMap<String> veinTypeStorageInfo = new Object2ShortOpenHashMap<>();
    public static ObjectSet<VeinType> veinTypes;

    // BartWorks initializes veins in FML preInit
    // GalacticGreg initializes veins in FML postInit, but only copies all base game veins to make them available on all
    // planets
    // GregTech initializes veins in a thread in FML postInit
    // Therefore, this method must be called after GregTech postInit
    public void run() {
        veinTypes = new ObjectOpenHashSet<>();
        veinTypes.add(VeinType.NO_VEIN);

        for (OreMixes mix : OreMixes.values()) {
            veinTypes.add(new VeinType(mix.oreMixBuilder));
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
            veinTypes.add(
                    new VeinType(
                            vein.mWorldGenName,
                            oreMaterialProvider,
                            vein.mSize,
                            (short) vein.mPrimaryMeta,
                            (short) vein.mSecondaryMeta,
                            (short) vein.mBetweenMeta,
                            (short) vein.mSporadicMeta,
                            Math.max(0, vein.mMinY),
                            Math.min(255, vein.mMaxY),
                            vein.getDimName()));
        }

        // Assign veinTypeIds for efficient storage
        loadVeinTypeStorageInfo();

        final OptionalInt maxVeinTypeIdOptional = veinTypeStorageInfo.values().intStream().max();
        short maxVeinTypeId = (short) (maxVeinTypeIdOptional.orElse(0));

        for (VeinType veinType : veinTypes) {
            veinType.veinId = veinTypeStorageInfo.getOrDefault(veinType.name, ++maxVeinTypeId);
            veinTypeStorageInfo.putIfAbsent(veinType.name, veinType.veinId);
            veinTypeLookupTableForIds.put(veinType.veinId, veinType);
            veinTypeLookupTableForNames.put(veinType.name, veinType);
        }
        saveVeinTypeStorageInfo();
    }

    public static VeinType getVeinType(short veinTypeId) {
        return veinTypeLookupTableForIds.getOrDefault(veinTypeId, VeinType.NO_VEIN);
    }

    public static VeinType getVeinType(String veinTypeName) {
        return veinTypeLookupTableForNames.getOrDefault(veinTypeName, VeinType.NO_VEIN);
    }

    private static File getVeinTypeStorageInfoFile() {
        final File directory = Utils.getSubDirectory(Tags.VISUALPROSPECTING_DIR);
        // noinspection ResultOfMethodCallIgnored
        directory.mkdirs();
        return new File(directory, "veintypesLUT");
    }

    private static void loadVeinTypeStorageInfo() {
        veinTypeStorageInfo.putAll(Utils.readFileToMap(getVeinTypeStorageInfoFile()));
    }

    private static void saveVeinTypeStorageInfo() {
        Utils.writeMapToFile(getVeinTypeStorageInfoFile(), veinTypeStorageInfo);
    }

    public static void recalculateNEISearch() {
        if (isNEIInstalled()) {
            final boolean isSearchActive = SearchField.searchInventories();
            final String searchString = NEIClientConfig.getSearchExpression().toLowerCase();
            final Pattern filterPattern = SearchField.getPattern(searchString);

            for (VeinType veinType : veinTypes) {
                if (veinType == VeinType.NO_VEIN) continue;
                if (isSearchActive && !searchString.isEmpty()) {
                    List<String> searchableStrings = veinType.getOreMaterialNames();
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
