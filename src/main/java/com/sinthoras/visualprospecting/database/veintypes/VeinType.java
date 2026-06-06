package com.sinthoras.visualprospecting.database.veintypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.sinthoras.visualprospecting.Tags;

import galacticgreg.api.enums.DimensionDef.DimNames;
import gregtech.api.interfaces.IOreMaterial;
import gregtech.common.OreMixBuilder;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

@ParametersAreNonnullByDefault
public class VeinType {

    // A vein spans 9 block layers in GT WorldgenGTOreLayer.
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
    private final ReferenceOpenHashSet<IOreMaterial> oresAsSet = new ReferenceOpenHashSet<>();
    private final ReferenceOpenHashSet<IOreMaterial> nonSporadicOres = new ReferenceOpenHashSet<>();
    private final List<List<IOreMaterial>> oresByLayer;
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
        this.oresByLayer = buildOresByLayer();
    }

    public VeinType(OreMixBuilder oreMix) {
        name = oreMix.oreMixName;
        localizedName = oreMix.getLocalizedName();
        blockSize = oreMix.size;
        representativeOre = oreMix.representative;
        oresAsSet.add(primaryOre = oreMix.primary);
        oresAsSet.add(secondaryOre = oreMix.secondary);
        oresAsSet.add(inBetweenOre = oreMix.between);
        oresAsSet.add(sporadicOre = oreMix.sporadic);
        nonSporadicOres.add(primaryOre);
        nonSporadicOres.add(secondaryOre);
        nonSporadicOres.add(inBetweenOre);
        oresByLayer = buildOresByLayer();
        minBlockY = Math.max(0, oreMix.minY - 6);
        maxBlockY = Math.min(255, oreMix.maxY - 6);
        allowedDims.addAll(oreMix.dimsEnabled);

        // Fuse entries for "The End" and "EndAsteroid" since they are both about same dimension.
        // World gen swap which one is used around 16 chunks from central point.
        final boolean inEnd = allowedDims.contains(DimNames.THE_END);
        final boolean inEndAsteroid = allowedDims.contains(DimNames.ENDASTEROID);
        if (inEnd != inEndAsteroid) {
            allowedDims.add(inEnd ? DimNames.ENDASTEROID : DimNames.THE_END);
        }
    }

    public boolean containsAllFoundOres(Collection<IOreMaterial> foundOres, String dimName, IOreMaterial specific,
            int minY) {
        return minY >= minBlockY && (primaryOre == specific || secondaryOre == specific)
                && (dimName.isEmpty() || allowedDims.contains(dimName))
                && foundOres.containsAll(oresAsSet);
    }

    public boolean matchesWithSpecificPrimaryOrSecondary(Collection<IOreMaterial> foundOres, String dimName,
            IOreMaterial specific) {
        return (primaryOre == specific || secondaryOre == specific)
                && (dimName.isEmpty() || allowedDims.contains(dimName))
                && foundOres.containsAll(oresAsSet);
    }

    public boolean matchesIgnoringSporadic(Collection<IOreMaterial> foundOres, String dimName, IOreMaterial specific) {
        return (primaryOre == specific || secondaryOre == specific)
                && (dimName.isEmpty() || allowedDims.contains(dimName))
                && foundOres.containsAll(nonSporadicOres);
    }

    public List<String> getAllowedDimensions() {
        return allowedDims;
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
        if (layerBlockY < 0 || layerBlockY >= veinHeight) {
            return Collections.emptyList();
        }
        return oresByLayer.get(layerBlockY);
    }

    // Layer -> ores mirrors GT WorldgenGTOreLayer placement
    private List<List<IOreMaterial>> buildOresByLayer() {
        final List<List<IOreMaterial>> layers = new ArrayList<>(veinHeight);
        for (int layer = 0; layer < veinHeight; layer++) {
            final List<IOreMaterial> ores = new ArrayList<>(3);
            switch (layer) {
                case 0, 1, 2 -> {
                    ores.add(secondaryOre);
                    ores.add(sporadicOre);
                }
                case 3 -> {
                    ores.add(secondaryOre);
                    ores.add(inBetweenOre);
                    ores.add(sporadicOre);
                }
                case 4 -> {
                    ores.add(inBetweenOre);
                    ores.add(sporadicOre);
                }
                case 5, 6 -> {
                    ores.add(primaryOre);
                    ores.add(inBetweenOre);
                    ores.add(sporadicOre);
                }
                case 7, 8 -> {
                    ores.add(primaryOre);
                    ores.add(sporadicOre);
                }
            }
            layers.add(ores);
        }
        return layers;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setNEISearchHighlight(boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
    }
}
