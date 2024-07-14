package com.sinthoras.visualprospecting.integration.model.buttons;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.buttons.ButtonManager;
import com.sinthoras.visualprospecting.Tags;

public class ThaumcraftNodeButtonManager extends ButtonManager {

    public static final ThaumcraftNodeButtonManager instance = new ThaumcraftNodeButtonManager();

    @Override
    public ResourceLocation getIcon(SupportedMods mod, String theme) {
        return new ResourceLocation(Tags.MODID, "textures/icons/nodes.png");
    }

    @Override
    public String getButtonText() {
        return StatCollector.translateToLocal("visualprospecting.button.nodes");
    }
}
