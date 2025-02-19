package com.sinthoras.visualprospecting.integration.model.render;

import net.minecraft.client.resources.I18n;

import com.gtnewhorizons.navigator.api.model.steps.UniversalRenderStep;
import com.gtnewhorizons.navigator.api.util.DrawUtils;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.integration.model.layers.UndergroundFluidLayerManager;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidLocation;

public class UndergroundFluidRenderStep extends UniversalRenderStep<UndergroundFluidLocation> {

    public UndergroundFluidRenderStep(UndergroundFluidLocation location) {
        super(location);
        setSize(VP.undergroundFluidSizeChunkX * VP.chunkWidth);
        setFontScale(0.5f);
    }

    @Override
    public void draw(double topX, double topY, float drawScale, double zoom) {
        renderChunks(topX, topY);
        setSize(VP.undergroundFluidSizeChunkX * VP.chunkWidth);
        int alpha = location.isActive() ? 255 : 74;
        DrawUtils.drawHollowRect(
                topX,
                topY,
                getAdjustedWidth(),
                getAdjustedHeight() - 0.5,
                location.getFluid().getColor(),
                alpha,
                2);

        if (!isMinimap()) {
            final int maxAmountInField = location.getMaxProduction();
            String label = I18n.format("visualprospecting.empty");
            if (maxAmountInField > 0) {
                label = location.getMinProduction() + "L - "
                        + maxAmountInField
                        + "L  "
                        + location.getFluid().getLocalizedName();
            }

            int textColor = 0xFFFFFFFF;
            if (UndergroundFluidLayerManager.instance.isSearchActive()) {
                textColor = location.isActive() ? 0xFFFF00 : 0x444444;
            }

            DrawUtils.drawLabel(
                    label,
                    topX + getAdjustedWidth() / 2,
                    topY + 1.5,
                    textColor,
                    0xB4000000,
                    true,
                    getFontScale());
        }
    }

    private void renderChunks(double x, double y) {
        if (getZoomStep() < Config.minZoomLevelForUndergroundFluidDetails || location.getMaxProduction() <= 0
                || !location.isActive()) {
            return;
        }

        setSize(VP.chunkWidth);
        for (int chunkX = 0; chunkX < VP.undergroundFluidSizeChunkX; chunkX++) {
            for (int chunkZ = 0; chunkZ < VP.undergroundFluidSizeChunkZ; chunkZ++) {
                double xOffset = chunkX * getAdjustedWidth();
                double yOffset = chunkZ * getAdjustedHeight();
                int amount = location.getChunks()[chunkX][chunkZ];
                if (amount > 0) {
                    float alpha = (float) (amount - location.getMinProduction())
                            / (location.getMaxProduction() - location.getMinProduction() + 1);
                    alpha = alpha * 255;
                    int fluidColor = location.getFluid().getColor();
                    DrawUtils.drawRect(
                            x + xOffset,
                            y + yOffset,
                            getAdjustedWidth(),
                            getAdjustedHeight(),
                            fluidColor,
                            (int) alpha);

                    if (amount >= location.getMaxProduction()) {
                        DrawUtils.drawHollowRect(
                                x + xOffset,
                                y + yOffset,
                                getAdjustedWidth(),
                                getAdjustedHeight(),
                                0xFFD700,
                                204,
                                1.5);
                    }

                    DrawUtils.drawLabel(
                            getFluidAmountFormatted(amount),
                            x + xOffset + getAdjustedWidth() / 2,
                            y + yOffset + getAdjustedHeight() / 2,
                            0xFFFFFFFF,
                            0xB4000000,
                            true,
                            getFontScale());
                }
            }
        }
    }

    private String getFluidAmountFormatted(int amount) {
        if (amount >= 1000) {
            return (amount / 1000) + "kL";
        }
        return amount + "L";
    }
}
