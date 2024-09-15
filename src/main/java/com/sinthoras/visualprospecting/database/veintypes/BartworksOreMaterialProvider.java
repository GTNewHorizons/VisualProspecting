package com.sinthoras.visualprospecting.database.veintypes;

import static bartworks.util.BWColorUtil.getColorFromRGBArray;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;

import bartworks.system.material.Werkstoff;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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
        return ores.intStream().mapToObj(metaData -> Werkstoff.werkstoffHashMap.get((short) metaData))
                .filter(Objects::nonNull).map(material -> EnumChatFormatting.GRAY + material.getLocalizedName())
                .collect(Collectors.toList());
    }
}
