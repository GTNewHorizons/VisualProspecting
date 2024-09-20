package com.sinthoras.visualprospecting.database.veintypes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.IIcon;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import it.unimi.dsi.fastutil.shorts.ShortCollection;

public class GregTechOreMaterialProvider implements IOreMaterialProvider {

    private final Materials material;
    private final int primaryOreColor;
    private IIcon primaryOreIcon;
    private final String primaryOreName;
    private ImmutableList<String> containedOres;

    public GregTechOreMaterialProvider(Materials material) {
        this.material = material;
        this.primaryOreColor = (material.mRGBa[0] << 16) | (material.mRGBa[1]) << 8 | material.mRGBa[2];
        this.primaryOreName = material.mLocalizedName;
    }

    GregTechOreMaterialProvider() {
        material = Materials._NULL;
        primaryOreColor = 0;
        primaryOreName = "";
        containedOres = ImmutableList.of();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon() {
        if (primaryOreIcon == null) {
            primaryOreIcon = material.mIconSet.mTextures[OrePrefixes.ore.mTextureIndex].getIcon();
        }
        return primaryOreIcon;
    }

    @Override
    public int getColor() {
        return primaryOreColor;
    }

    @Override
    public String getLocalizedName() {
        return primaryOreName;
    }

    @Override
    public ImmutableList<String> getContainedOres(ShortCollection ores) {
        if (containedOres == null) {
            List<String> temp = new ArrayList<>();
            for (short meta : ores) {
                Materials material = GregTechAPI.sGeneratedMaterials[meta];
                if (material == null) continue;
                temp.add(material.mLocalizedName);
            }
            containedOres = ImmutableList.copyOf(temp);
        }
        return containedOres;
    }
}
