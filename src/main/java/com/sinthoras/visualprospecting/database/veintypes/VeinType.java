package com.sinthoras.visualprospecting.database.veintypes;

import java.util.ArrayList;
import java.util.List;

import com.sinthoras.visualprospecting.Tags;

import it.unimi.dsi.fastutil.shorts.ShortCollection;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;

public class VeinType {

    public static final int veinHeight = 9;

    public final String name;
    public short veinId;
    public final IOreMaterialProvider oreMaterialProvider;
    public final int blockSize;
    public final short primaryOreMeta;
    public final short secondaryOreMeta;
    public final short inBetweenOreMeta;
    public final short sporadicOreMeta;
    public final int minBlockY;
    public final int maxBlockY;
    public final ShortSet oresAsSet = new ShortOpenHashSet();
    private final List<String> containedOres = new ArrayList<>();
    private boolean isHighlighted = true;
    private String primaryOreName = "";

    // Available after VisualProspecting post GT initialization
    public static final VeinType NO_VEIN = new VeinType(
            Tags.ORE_MIX_NONE_NAME,
            null,
            0,
            (short) -1,
            (short) -1,
            (short) -1,
            (short) -1,
            0,
            0);

    public VeinType(String name, IOreMaterialProvider oreMaterialProvider, int blockSize, short primaryOreMeta,
            short secondaryOreMeta, short inBetweenOreMeta, short sporadicOreMeta, int minBlockY, int maxBlockY) {
        this.name = name;
        this.oreMaterialProvider = oreMaterialProvider;
        this.blockSize = blockSize;
        this.minBlockY = minBlockY;
        this.maxBlockY = maxBlockY;
        oresAsSet.add(this.primaryOreMeta = primaryOreMeta);
        oresAsSet.add(this.secondaryOreMeta = secondaryOreMeta);
        oresAsSet.add(this.inBetweenOreMeta = inBetweenOreMeta);
        oresAsSet.add(this.sporadicOreMeta = sporadicOreMeta);
        if (oreMaterialProvider != null) {
            containedOres.addAll(oreMaterialProvider.getContainedOres(oresAsSet));
            primaryOreName = oreMaterialProvider.getLocalizedName();
        }
    }

    public boolean matches(ShortCollection foundOres) {
        return foundOres.containsAll(oresAsSet);
    }

    public boolean matchesWithSpecificPrimaryOrSecondary(ShortCollection foundOres, short specificMeta) {
        return (primaryOreMeta == specificMeta || secondaryOreMeta == specificMeta) && foundOres.containsAll(oresAsSet);
    }

    public boolean canOverlapIntoNeighborOreChunk() {
        return blockSize > 24;
    }

    // Ore chunks on coordinates -1 and 1 are one chunk less apart
    public boolean canOverlapIntoNeighborOreChunkAtCoordinateAxis() {
        return blockSize > 16;
    }

    public boolean containsOre(short oreMetaData) {
        return primaryOreMeta == oreMetaData || secondaryOreMeta == oreMetaData
                || inBetweenOreMeta == oreMetaData
                || sporadicOreMeta == oreMetaData;
    }

    public List<String> getOreMaterialNames() {
        return containedOres;
    }

    public String getPrimaryOreName() {
        return primaryOreName;
    }

    public ShortSet getOresAtLayer(int layerBlockY) {
        final ShortSet result = new ShortOpenHashSet();
        switch (layerBlockY) {
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
