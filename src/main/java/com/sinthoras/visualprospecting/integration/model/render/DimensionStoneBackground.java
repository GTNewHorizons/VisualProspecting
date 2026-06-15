package com.sinthoras.visualprospecting.integration.model.render;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

import com.sinthoras.visualprospecting.database.DimensionNameResolver;

import galacticgreg.api.enums.DimensionDef.DimNames;
import gregtech.api.enums.StoneType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Picks the stone-type background icon for an ore-vein marker so it matches the vein's host rock (netherrack in the
 * Nether, end stone in the End, ...).
 * <p>
 * Unmapped dimensions fall back to vanilla stone.
 */
public final class DimensionStoneBackground {

    private static final Map<String, StoneType> STONE_BY_DIM_NAME = new HashMap<>();

    static {
        // Vanilla dimensions
        STONE_BY_DIM_NAME.put(DimNames.OW, StoneType.Stone);
        STONE_BY_DIM_NAME.put(DimNames.TWILIGHT_FOREST, StoneType.Stone);
        STONE_BY_DIM_NAME.put(DimNames.NETHER, StoneType.Netherrack);
        STONE_BY_DIM_NAME.put(DimNames.THE_END, StoneType.Endstone); // The End and EndAsteroid share same dim

        // GalactiCraft / GalaxySpace / AmunRa bodies
        STONE_BY_DIM_NAME.put(DimNames.MOON, StoneType.Moon);
        STONE_BY_DIM_NAME.put(DimNames.MARS, StoneType.Mars);
        STONE_BY_DIM_NAME.put(DimNames.ASTEROIDS, StoneType.Asteroid);
        STONE_BY_DIM_NAME.put(DimNames.PHOBOS, StoneType.Phobos);
        STONE_BY_DIM_NAME.put(DimNames.DEIMOS, StoneType.Deimos);
        STONE_BY_DIM_NAME.put(DimNames.CERES, StoneType.Ceres);
        STONE_BY_DIM_NAME.put(DimNames.IO, StoneType.Io);
        STONE_BY_DIM_NAME.put(DimNames.EUROPA, StoneType.Europa);
        STONE_BY_DIM_NAME.put(DimNames.GANYMEDE, StoneType.Ganymede);
        STONE_BY_DIM_NAME.put(DimNames.CALLISTO, StoneType.Callisto);
        STONE_BY_DIM_NAME.put(DimNames.ENCELADUS, StoneType.Enceladus);
        STONE_BY_DIM_NAME.put(DimNames.TITAN, StoneType.Titan);
        STONE_BY_DIM_NAME.put(DimNames.MIRANDA, StoneType.Miranda);
        STONE_BY_DIM_NAME.put(DimNames.OBERON, StoneType.Oberon);
        STONE_BY_DIM_NAME.put(DimNames.PROTEUS, StoneType.Proteus);
        STONE_BY_DIM_NAME.put(DimNames.TRITON, StoneType.Triton);
        STONE_BY_DIM_NAME.put(DimNames.PLUTO, StoneType.Pluto);
        STONE_BY_DIM_NAME.put(DimNames.HAUMEA, StoneType.Haumea);
        STONE_BY_DIM_NAME.put(DimNames.MAKEMAKE, StoneType.MakeMake);
        STONE_BY_DIM_NAME.put(DimNames.VENUS, StoneType.Venus);
        STONE_BY_DIM_NAME.put(DimNames.MERCURY, StoneType.Mercury);
        STONE_BY_DIM_NAME.put(DimNames.CENTAURIBB, StoneType.AlphaCentauri);
        STONE_BY_DIM_NAME.put(DimNames.VEGAB, StoneType.VegaB);
        STONE_BY_DIM_NAME.put(DimNames.BARNARDE, StoneType.BarnardaE);
        STONE_BY_DIM_NAME.put(DimNames.BARNARDF, StoneType.BarnardaF);
        STONE_BY_DIM_NAME.put(DimNames.TCETIE, StoneType.TCetiE);
        STONE_BY_DIM_NAME.put(DimNames.HORUS, StoneType.Horus);
        STONE_BY_DIM_NAME.put(DimNames.ANUBIS, StoneType.AnubisAndMaahes);
        STONE_BY_DIM_NAME.put(DimNames.MAAHES, StoneType.AnubisAndMaahes);
        STONE_BY_DIM_NAME.put(DimNames.SETH, StoneType.SethClay);
        STONE_BY_DIM_NAME.put(DimNames.MEHENBELT, StoneType.PackedIce);
        STONE_BY_DIM_NAME.put(DimNames.KUIPERBELT, StoneType.PackedIce);
    }

    private static final Int2ObjectOpenHashMap<StoneType> STONE_BY_DIM_ID = new Int2ObjectOpenHashMap<>();

    private DimensionStoneBackground() {}

    /** Returns the stone background icon for the vein's dimension, or vanilla stone when unknown. */
    public static IIcon getBackgroundIcon(int dimensionId) {
        StoneType stoneType = STONE_BY_DIM_ID.get(dimensionId);
        if (stoneType == null) {
            final String dimName = DimensionNameResolver.resolve(dimensionId);
            stoneType = STONE_BY_DIM_NAME.getOrDefault(dimName, StoneType.Stone);
            if (!dimName.isEmpty()) {
                STONE_BY_DIM_ID.put(dimensionId, stoneType);
            }
        }

        final IIcon icon = stoneType.getIcon(1);
        return icon != null ? icon : Blocks.stone.getIcon(0, 0);
    }
}
