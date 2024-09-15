package com.sinthoras.visualprospecting.database.veintypes;

import java.util.List;

import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.shorts.ShortCollection;

public interface IOreMaterialProvider {

    @SideOnly(Side.CLIENT)
    IIcon getIcon();

    int getColor();

    String getLocalizedName();

    List<String> getContainedOres(ShortCollection ores);
}
