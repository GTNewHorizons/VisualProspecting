package com.sinthoras.visualprospecting.database.veintypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.sinthoras.visualprospecting.Tags;

import gregtech.api.interfaces.IOreMaterial;
import gregtech.common.OreMixBuilder;

@ParametersAreNonnullByDefault
public class VeinType {

    public static final int veinHeight = 9;

    public final String name;
    public final int blockSize;
    public final IOreMaterial representativeOre;
    public final IOreMaterial primaryOre;
    public final IOreMaterial secondaryOre;
    public final IOreMaterial inBetweenOre;
    public final IOreMaterial sporadicOre;
    public final int minBlockY;
    public final int maxBlockY;
    private final HashSet<IOreMaterial> oresAsSet = new HashSet<IOreMaterial>();
    private final List<String> allowedDims = new ArrayList<>();
    private boolean isHighlighted = true;
    private final String localizedName;

    // Available after VisualProspecting post GT initialization
    public static final VeinType NO_VEIN = new VeinType();

    private VeinType() {
        this.name = Tags.ORE_MIX_NONE_NAME;
        this.blockSize = 0;
        this.minBlockY = 0;
        this.maxBlockY = 255;
        this.representativeOre = null;
        this.primaryOre = null;
        this.secondaryOre = null;
        this.inBetweenOre = null;
        this.sporadicOre = null;
        this.localizedName = this.name;
    }

    public VeinType(OreMixBuilder oreMix) {
        name = oreMix.oreMixName;
        localizedName = oreMix.localizedName;
        blockSize = oreMix.size;
        representativeOre = oreMix.representative;
        oresAsSet.add(primaryOre = oreMix.primary);
        oresAsSet.add(secondaryOre = oreMix.secondary);
        oresAsSet.add(inBetweenOre = oreMix.between);
        oresAsSet.add(sporadicOre = oreMix.sporadic);
        minBlockY = Math.max(0, oreMix.minY - 6);
        maxBlockY = Math.min(255, oreMix.maxY - 6);
        allowedDims.addAll(oreMix.dimsEnabled);
    }

    public boolean containsAllFoundOres(Collection<IOreMaterial> foundOres, String dimName, IOreMaterial specific,
            int minY) {
        return minY >= minBlockY && (primaryOre == specific || secondaryOre == specific)
                && (dimName.isEmpty() || allowedDims.contains(dimName))
                && oresAsSet.containsAll(foundOres);
    }

    public boolean matchesWithSpecificPrimaryOrSecondary(Collection<IOreMaterial> foundOres, String dimName,
            IOreMaterial specific) {
        return (primaryOre == specific || secondaryOre == specific)
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

    public boolean containsOre(IOreMaterial ore) {
        return primaryOre == ore || secondaryOre == ore || inBetweenOre == ore || sporadicOre == ore;
    }

    public ImmutableList<String> getOreMaterialNames() {
        return ImmutableList
                .copyOf(oresAsSet.stream().map(IOreMaterial::getLocalizedName).collect(Collectors.toList()));
    }

    public String getVeinName() {
        return localizedName;
    }

    public List<IOreMaterial> getOresAtLayer(int layerBlockY) {
        final List<IOreMaterial> result = new ArrayList<>();

        switch (layerBlockY) {
            case 0, 1, 2 -> {
                result.add(secondaryOre);
                result.add(sporadicOre);
                return result;
            }
            case 3 -> {
                result.add(secondaryOre);
                result.add(inBetweenOre);
                result.add(sporadicOre);
                return result;
            }
            case 4 -> {
                result.add(inBetweenOre);
                result.add(sporadicOre);
                return result;
            }
            case 5, 6 -> {
                result.add(primaryOre);
                result.add(inBetweenOre);
                result.add(sporadicOre);
                return result;
            }
            case 7, 8 -> {
                result.add(primaryOre);
                result.add(sporadicOre);
                return result;
            }
            default -> {
                return result;
            }
        }
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setNEISearchHighlight(boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
    }
}
