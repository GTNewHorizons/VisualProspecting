package com.sinthoras.visualprospecting.database.veintypes;

import com.sinthoras.visualprospecting.Tags;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VeinType {

    public static final int veinHeight = 9;

    public final String name;
    public short veinId;
    public final int blockSize;
    public final short primaryOreMeta;
    public final short secondaryOreMeta;
    public final short inBetweenOreMeta;
    public final short sporadicOreMeta;
    private final HashSet<Short> oresAsHashSet;
    private boolean isHighlighted = true;

    // Available after VisualProspecting post GT initialization
    public final static VeinType NO_VEIN = new VeinType(Tags.ORE_MIX_NONE_NAME, 0, (short)-1, (short)-1, (short)-1, (short)-1);

    public VeinType(String name, int blockSize, short primaryOreMeta, short secondaryOreMeta, short inBetweenOreMeta, short sporadicOreMeta)
    {
        this.name = name;
        this.blockSize = blockSize;
        this.primaryOreMeta = primaryOreMeta;
        this.secondaryOreMeta = secondaryOreMeta;
        this.inBetweenOreMeta = inBetweenOreMeta;
        this.sporadicOreMeta = sporadicOreMeta;
        oresAsHashSet = new HashSet<>();
        oresAsHashSet.add(primaryOreMeta);
        oresAsHashSet.add(secondaryOreMeta);
        oresAsHashSet.add(inBetweenOreMeta);
        oresAsHashSet.add(sporadicOreMeta);
    }

    public boolean matches(Set<Short> foundOres) {
        return foundOres.containsAll(oresAsHashSet);
    }

    public boolean matchesWithSpecificPrimaryOrSecondary(Set<Short> foundOres, short specificMeta) {
        return (primaryOreMeta == specificMeta || secondaryOreMeta == specificMeta) && foundOres.containsAll(oresAsHashSet);
    }

    public boolean canOverlapIntoNeighborOreChunk() {
        return blockSize > 24;
    }

    public boolean containsOre(short oreMetaData) {
        return primaryOreMeta == oreMetaData
                || secondaryOreMeta == oreMetaData
                || inBetweenOreMeta == oreMetaData
                || sporadicOreMeta == oreMetaData;
    }

    public List<Materials> getOreMaterials() {
        return oresAsHashSet.stream().map(metaData -> GregTech_API.sGeneratedMaterials[metaData]).collect(Collectors.toList());
    }

    public String getNameReadable() {
        final String[] fragments = name.split("\\.");
        final String veinName = fragments[fragments.length - 1];
        return veinName.substring(0, 1).toUpperCase() + veinName.substring(1);
    }

    public HashSet<Short> getOresAtLayer(int layerBlockY) {
        final HashSet<Short> result = new HashSet<>();
        switch(layerBlockY) {
            case 0:
            case 1:
            case 2:
                result.add(secondaryOreMeta);
                result.add(sporadicOreMeta);
                return result;
            case 3:
                result.add(secondaryOreMeta);
                result.add(inBetweenOreMeta);
                result.add(sporadicOreMeta);
                return result;
            case 4:
                result.add(inBetweenOreMeta);
                result.add(sporadicOreMeta);
                return result;
            case 5:
            case 6:
                result.add(primaryOreMeta);
                result.add(inBetweenOreMeta);
                result.add(sporadicOreMeta);
                return result;
            case 7:
            case 8:
                result.add(primaryOreMeta);
                result.add(sporadicOreMeta);
                return result;
            default:
                return result;
        }
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setNEISearchHeighlight(boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
    }
}