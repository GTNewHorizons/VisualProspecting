package com.sinthoras.visualprospecting.integration.model.buttons;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.buttons.ButtonManager;
import com.sinthoras.visualprospecting.Tags;

public class UndergroundFluidButtonManager extends ButtonManager {

    public static final UndergroundFluidButtonManager instance = new UndergroundFluidButtonManager();

    @Override
    public ResourceLocation getIcon(SupportedMods mod, String theme) {
        return new ResourceLocation(Tags.MODID, "textures/icons/undergroundfluid.png");
    }

    @Override
    public String getButtonText() {
        return StatCollector.translateToLocal("visualprospecting.button.undergroundfluid");
    }
}
