package com.sinthoras.visualprospecting.database.veintypes;

import static com.sinthoras.visualprospecting.Utils.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Pattern;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;

import bartworks.system.material.Werkstoff;
import bartworks.system.oregen.BWOreLayer;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.SearchField;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.common.WorldgenGTOreLayer;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class VeinTypeCaching implements Runnable {

    private static final BiMap<Short, VeinType> veinTypeLookupTableForIds = HashBiMap.create();
    private static final Map<String, VeinType> veinTypeLookupTableForNames = new HashMap<>();
    private static final Object2ShortMap<String> veinTypeStorageInfo = new Object2ShortOpenHashMap<>();
    private static final Short2ObjectMap<ObjectSet<VeinType>> primaryMetaToVeinType = new Short2ObjectOpenHashMap<>();
    public static ObjectSet<VeinType> veinTypes;
    private static int longesOreName = 0;

    // BartWorks initializes veins in FML preInit
    // GalacticGreg initializes veins in FML postInit, but only copies all base game veins to make them available on all
    // planets
    // GregTech initializes veins in a thread in FML postInit
    // Therefore, this method must be called after GregTech postInit
    public void run() {
        veinTypes = new ObjectOpenHashSet<>();
        veinTypes.add(VeinType.NO_VEIN);

        for (WorldgenGTOreLayer vein : WorldgenGTOreLayer.sList) {
            if (vein.mWorldGenName.equals(Tags.ORE_MIX_NONE_NAME)) {
                break;
            }
            final Materials material = getGregTechMaterial(vein.mPrimaryMeta);
            veinTypes.add(
                    new VeinType(
                            vein.mWorldGenName,
                            new GregTechOreMaterialProvider(material),
                            vein.mSize,
                            vein.mPrimaryMeta,
                            vein.mSecondaryMeta,
                            vein.mBetweenMeta,
                            vein.mSporadicMeta,
                            Math.max(0, vein.mMinY - 6), // GregTech ore veins start at layer -1 and the blockY RNG adds
                                                         // another -5
                            // offset
                            Math.min(255, vein.mMaxY - 6)));
        }

        for (BWOreLayer vein : BWOreLayer.sList) {
            final IOreMaterialProvider oreMaterialProvider = (vein.bwOres & 0b1000) == 0
                    ? new GregTechOreMaterialProvider(getGregTechMaterial((short) vein.mPrimaryMeta))
                    : new BartworksOreMaterialProvider(Werkstoff.werkstoffHashMap.get((short) vein.mPrimaryMeta));
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
                            Math.min(255, vein.mMaxY)));
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
            primaryMetaToVeinType.computeIfAbsent(veinType.primaryOreMeta, k -> new ObjectOpenHashSet<>())
                    .add(veinType);
        }
        saveVeinTypeStorageInfo();

        for (VeinType veinType : veinTypes) {
            longesOreName = Math.max(longesOreName, veinType.name.length());
        }
    }

    private Materials getGregTechMaterial(short metaId) {
        final Materials material = GregTechAPI.sGeneratedMaterials[metaId];
        if (material == null) {
            // Some materials are not registered in dev when their usage mod is not available.
            return Materials.getAll().stream().filter(m -> m.mMetaItemSubID == metaId).findAny().get();
        }
        return material;
    }

    public static int getLongesOreNameLength() {
        return longesOreName;
    }

    public static short getVeinTypeId(VeinType veinType) {
        return veinTypeLookupTableForIds.inverse().get(veinType);
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

    public static ObjectSet<VeinType> getVeinTypesForPrimaryMeta(short primaryMeta) {

        return primaryMetaToVeinType.getOrDefault(primaryMeta, ObjectSets.emptySet());
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
                    searchableStrings.add(I18n.format(veinType.name));
                    final boolean match = searchableStrings.stream()
                            .map(EnumChatFormatting::getTextWithoutFormattingCodes).map(String::toLowerCase)
                            .anyMatch(searchableString -> filterPattern.matcher(searchableString).find());

                    veinType.setNEISearchHeighlight(match);
                } else {
                    veinType.setNEISearchHeighlight(true);
                }
            }
        }
    }
}
