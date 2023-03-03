package com.sinthoras.visualprospecting.mixins.late.journeymap;

import journeymap.client.ui.minimap.DisplayVars;
import journeymap.client.ui.minimap.Shape;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DisplayVars.class)
public interface DisplayVarsAccessor {

    @Accessor(remap = false)
    float getDrawScale();

    @Accessor(remap = false)
    double getFontScale();

    @Accessor(remap = false)
    Shape getShape();

    @Accessor(remap = false)
    int getMinimapWidth();

}
