package com.sinthoras.visualprospecting.mixins.late.journeymap;

import journeymap.client.render.draw.DrawStep;
import journeymap.client.render.map.GridRenderer;
import journeymap.client.ui.minimap.DisplayVars;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.ui.minimap.Shape;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.sinthoras.visualprospecting.integration.journeymap.JourneyMapState;
import com.sinthoras.visualprospecting.integration.journeymap.render.LayerRenderer;
import com.sinthoras.visualprospecting.integration.model.MapState;
import com.sinthoras.visualprospecting.integration.model.layers.LayerManager;

@Mixin(MiniMap.class)
public abstract class MiniMapMixin {

    @Final
    @Shadow(remap = false)
    private static GridRenderer gridRenderer;

    @Final
    @Shadow(remap = false)
    private Minecraft mc;

    @Shadow(remap = false)
    private DisplayVars dv;

    @Inject(method = "drawOnMapWaypoints", at = @At(value = "HEAD"), remap = false, require = 1)
    private void visualprospecting$onBeforeDrawWaypoints(double rotation, CallbackInfo ci) {
        for (LayerManager layerManager : MapState.instance.layers) {
            if (layerManager.isLayerActive()) {
                if (((DisplayVarsAccessor) dv).getShape() == Shape.Circle) {
                    layerManager.recacheMiniMap(
                            (int) mc.thePlayer.posX,
                            (int) mc.thePlayer.posZ,
                            ((DisplayVarsAccessor) dv).getMinimapWidth());
                } else {
                    layerManager.recacheMiniMap(
                            (int) mc.thePlayer.posX,
                            (int) mc.thePlayer.posZ,
                            gridRenderer.getWidth(),
                            gridRenderer.getHeight());
                }
            }
        }

        for (LayerRenderer layerRenderer : JourneyMapState.instance.renderers) {
            if (layerRenderer.isLayerActive()) {
                for (DrawStep drawStep : layerRenderer.getDrawStepsCachedForRendering()) {
                    drawStep.draw(
                            0.0D,
                            0.0D,
                            gridRenderer,
                            ((DisplayVarsAccessor) dv).getDrawScale(),
                            ((DisplayVarsAccessor) dv).getFontScale(),
                            rotation);
                }
            }
        }
    }
}
