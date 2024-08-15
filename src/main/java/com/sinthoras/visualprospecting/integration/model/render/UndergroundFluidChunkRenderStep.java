package com.sinthoras.visualprospecting.integration.model.render;

import com.gtnewhorizons.navigator.api.model.steps.UniversalRenderStep;
import com.gtnewhorizons.navigator.api.util.DrawUtils;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidChunkLocation;

public class UndergroundFluidChunkRenderStep extends UniversalRenderStep<UndergroundFluidChunkLocation> {

    public UndergroundFluidChunkRenderStep(UndergroundFluidChunkLocation location) {
        super(location);
        setFontScale(0.3F);
    }

    @Override
    public void draw(double topX, double topY, float drawScale, double zoom) {
        if (location.getFluidAmount() > 0 && getZoomStep() >= Config.minZoomLevelForUndergroundFluidDetails) {
            float alpha = (float) (location.getFluidAmount() - location.getMinAmountInField())
                    / (location.getMaxAmountInField() - location.getMinAmountInField() + 1);
            alpha = alpha * 255;
            int fluidColor = location.getFluid().getColor();
            DrawUtils.drawRect(topX, topY, getAdjustedWidth(), getAdjustedHeight(), fluidColor, (int) alpha);

            if (location.getFluidAmount() >= location.getMaxAmountInField()) {
                DrawUtils.drawHollowRect(topX, topY, getAdjustedWidth(), getAdjustedHeight(), 0xFFD700, 204, 1.5);
            }

            DrawUtils.drawLabel(
                    getLocation().getFluidAmountFormatted(),
                    topX + getAdjustedWidth() / 2,
                    topY + getAdjustedHeight() / 2,
                    0xFFFFFFFF,
                    0xB4000000,
                    true,
                    getFontScale());
        }
    }
}
