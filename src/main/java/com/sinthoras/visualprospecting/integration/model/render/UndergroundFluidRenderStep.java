package com.sinthoras.visualprospecting.integration.model.render;

import net.minecraft.client.resources.I18n;

import com.gtnewhorizons.navigator.api.model.steps.UniversalRenderStep;
import com.gtnewhorizons.navigator.api.util.DrawUtils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidLocation;

public class UndergroundFluidRenderStep extends UniversalRenderStep<UndergroundFluidLocation> {

    public UndergroundFluidRenderStep(UndergroundFluidLocation location) {
        super(location);
        setSize(VP.undergroundFluidSizeChunkX * VP.chunkWidth);
        setFontScale(0.5f);
    }

    @Override
    public void draw(double topX, double topY, float drawScale, double zoom) {
        final int maxAmountInField = location.getMaxProduction();
        final int borderColor = location.getFluid().getColor();
        DrawUtils.drawHollowRect(topX, topY, getAdjustedWidth(), getAdjustedHeight(), borderColor, 255, 2);

        if (getZoomStep() >= 1 && !isMinimap()) {
            String label = I18n.format("visualprospecting.empty");
            if (maxAmountInField > 0) {
                label = location.getMinProduction() + "L - "
                        + maxAmountInField
                        + "L  "
                        + location.getFluid().getLocalizedName();
            }

            DrawUtils.drawLabel(
                    label,
                    topX + getAdjustedWidth() / 2,
                    topY,
                    0xFFFFFFFF,
                    0xB4000000,
                    true,
                    getFontScale());
        }
    }
}
