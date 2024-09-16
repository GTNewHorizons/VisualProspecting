package com.sinthoras.visualprospecting.database.veintypes;

import static bartworks.util.BWColorUtil.getColorFromRGBArray;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;

import bartworks.system.material.Werkstoff;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.OrePrefixes;
import it.unimi.dsi.fastutil.shorts.ShortCollection;

public class BartworksOreMaterialProvider implements IOreMaterialProvider {

    private final Werkstoff material;
    private final int primaryOreColor;
    private final String primaryOreName;
    private IIcon primaryOreIcon;

    public BartworksOreMaterialProvider(Werkstoff material) {
        this.material = material;
        this.primaryOreColor = getColorFromRGBArray(material.getRGBA());
        this.primaryOreName = material.getLocalizedName();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon() {
        if (primaryOreIcon == null) {
            primaryOreIcon = material.getTexSet().mTextures[OrePrefixes.ore.mTextureIndex].getIcon();
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
        List<String> oreNames = new ArrayList<>();
        for (short meta : ores) {
            Werkstoff werkstoff = Werkstoff.werkstoffHashMap.get(meta);
            if (werkstoff == null) {
                oreNames.add(EnumChatFormatting.GRAY + GregTechAPI.sGeneratedMaterials[meta].mLocalizedName);
            } else {
                oreNames.add(EnumChatFormatting.GRAY + werkstoff.getLocalizedName());

            }
        }
        return oreNames;
    }
}
