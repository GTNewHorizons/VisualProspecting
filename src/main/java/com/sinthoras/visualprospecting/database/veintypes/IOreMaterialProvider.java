package com.sinthoras.visualprospecting.database.veintypes;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.IIconContainer;
import it.unimi.dsi.fastutil.shorts.ShortCollection;

public interface IOreMaterialProvider {

    @SideOnly(Side.CLIENT)
    IIconContainer getIconContainer();

    int getColor();

    String getLocalizedName();

    ImmutableList<String> getContainedOres(ShortCollection ores);
}
