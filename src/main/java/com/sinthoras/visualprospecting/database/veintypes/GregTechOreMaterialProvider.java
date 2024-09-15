package com.sinthoras.visualprospecting.database.veintypes;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;

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

    public GregTechOreMaterialProvider(Materials material) {
        this.material = material;
        this.primaryOreColor = (material.mRGBa[0] << 16) | (material.mRGBa[1]) << 8 | material.mRGBa[2];
        this.primaryOreName = material.mLocalizedName;
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
    public List<String> getContainedOres(ShortCollection ores) {
        return ores.intStream().mapToObj(metaData -> GregTechAPI.sGeneratedMaterials[metaData]).filter(Objects::nonNull)
                .map(material -> EnumChatFormatting.GRAY + material.mLocalizedName).collect(Collectors.toList());
    }
}
