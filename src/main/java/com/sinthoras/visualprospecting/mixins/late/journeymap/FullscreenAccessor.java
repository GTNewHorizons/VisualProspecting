package com.sinthoras.visualprospecting.mixins.late.journeymap;

import journeymap.client.render.map.GridRenderer;
import journeymap.client.ui.fullscreen.Fullscreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Fullscreen.class)
public interface FullscreenAccessor {

    @Accessor(remap = false)
    static GridRenderer getGridRenderer() {
        throw new IllegalStateException("Mixin accessor failed to apply");
    }

}
