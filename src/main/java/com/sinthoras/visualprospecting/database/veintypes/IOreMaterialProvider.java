package com.sinthoras.visualprospecting.database.veintypes;

import net.minecraft.util.IIcon;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.shorts.ShortCollection;

public interface IOreMaterialProvider {

    @SideOnly(Side.CLIENT)
    IIcon getIcon();

    int getColor();

    String getLocalizedName();

    ImmutableList<String> getContainedOres(ShortCollection ores);
}
