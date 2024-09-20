package com.sinthoras.visualprospecting.database.veintypes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.sinthoras.visualprospecting.Tags;

import gregtech.common.OreMixBuilder;
import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import it.unimi.dsi.fastutil.shorts.ShortCollection;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;

@ParametersAreNonnullByDefault
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
    private final ShortSet oresAsSet = new ShortArraySet();
    private final List<String> allowedDims = new ArrayList<>();
    private boolean isHighlighted = true;

    // Available after VisualProspecting post GT initialization
    public static final VeinType NO_VEIN = new VeinType(
            Tags.ORE_MIX_NONE_NAME,
            new GregTechOreMaterialProvider(),
            0,
            (short) -1,
            (short) -1,
            (short) -1,
            (short) -1,
            0,
            0,
            "");

    public VeinType(String name, IOreMaterialProvider oreMaterialProvider, int blockSize, short primaryOreMeta,
            short secondaryOreMeta, short inBetweenOreMeta, short sporadicOreMeta, int minBlockY, int maxBlockY,
            String dimName) {
        this.name = name;
        this.oreMaterialProvider = oreMaterialProvider;
        this.blockSize = blockSize;
        this.minBlockY = minBlockY;
        this.maxBlockY = maxBlockY;
        oresAsSet.add(this.primaryOreMeta = primaryOreMeta);
        oresAsSet.add(this.secondaryOreMeta = secondaryOreMeta);
        oresAsSet.add(this.inBetweenOreMeta = inBetweenOreMeta);
        oresAsSet.add(this.sporadicOreMeta = sporadicOreMeta);
        allowedDims.add(dimName);
    }

    public VeinType(OreMixBuilder oreMix) {
        name = oreMix.oreMixName;
        oreMaterialProvider = new GregTechOreMaterialProvider(oreMix.primary);
        blockSize = oreMix.size;
        oresAsSet.add(primaryOreMeta = (short) oreMix.primary.mMetaItemSubID);
        oresAsSet.add(secondaryOreMeta = (short) oreMix.secondary.mMetaItemSubID);
        oresAsSet.add(inBetweenOreMeta = (short) oreMix.between.mMetaItemSubID);
        oresAsSet.add(sporadicOreMeta = (short) oreMix.sporadic.mMetaItemSubID);
        minBlockY = Math.max(0, oreMix.minY - 6);
        maxBlockY = Math.min(255, oreMix.maxY - 6);
        allowedDims.addAll(oreMix.dimsEnabled.keySet());
    }

    public boolean containsAllFoundOres(ShortCollection foundOres, String dimName, short specificMeta, int minY) {
        return minY >= minBlockY && (primaryOreMeta == specificMeta || secondaryOreMeta == specificMeta)
                && (dimName.isEmpty() || allowedDims.contains(dimName))
                && oresAsSet.containsAll(foundOres);
    }

    public boolean matchesWithSpecificPrimaryOrSecondary(ShortCollection foundOres, String dimName,
            short specificMeta) {
        return (primaryOreMeta == specificMeta || secondaryOreMeta == specificMeta)
                && (dimName.isEmpty() || allowedDims.contains(dimName))
                && foundOres.containsAll(oresAsSet);
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

    public ImmutableList<String> getOreMaterialNames() {
        return oreMaterialProvider.getContainedOres(oresAsSet);
    }

    public String getPrimaryOreName() {
        return oreMaterialProvider.getLocalizedName();
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

    public void setNEISearchHighlight(boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
    }
}
