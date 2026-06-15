package com.sinthoras.visualprospecting.integration.model.render;

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

import com.sinthoras.visualprospecting.database.DimensionNameResolver;

import gregtech.api.enums.StoneType;
import gtneioreplugin.util.DimensionHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Picks the stone-type background icon for an ore-vein marker so it matches the vein's host rock (netherrack in the
 * Nether, end stone in the End, ...).
 * <p>
 * Unmapped dimensions fall back to vanilla stone.
 */
public final class DimensionStoneBackground {

    private static final Int2ObjectOpenHashMap<StoneType> STONE_BY_DIM_ID = new Int2ObjectOpenHashMap<>();

    private DimensionStoneBackground() {}

    /** Returns the stone background icon for the vein's dimension, or vanilla stone when unknown. */
    public static IIcon getBackgroundIcon(int dimensionId) {
        StoneType stoneType = STONE_BY_DIM_ID.get(dimensionId);
        if (stoneType == null) {
            final String dimName = DimensionNameResolver.resolve(dimensionId);
            stoneType = resolveStoneType(dimName);
            if (!dimName.isEmpty()) {
                STONE_BY_DIM_ID.put(dimensionId, stoneType);
            }
        }

        final IIcon icon = stoneType.getIcon(1);
        return icon != null ? icon : Blocks.stone.getIcon(0, 0);
    }

    private static StoneType resolveStoneType(String dimName) {
        if (!DimensionHelper.INTERNAL_TO_FULL.containsKey(dimName)) {
            return StoneType.Stone;
        }
        final List<StoneType> stoneTypes = DimensionHelper.getStoneTypes(dimName);
        return stoneTypes.isEmpty() ? StoneType.Stone : stoneTypes.get(0);
    }
}
